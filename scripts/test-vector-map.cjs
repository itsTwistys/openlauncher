const {chromium}=require('playwright');
const fs=require('fs'), path=require('path'), assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 try {
  const page=await browser.newPage({viewport:{width:800,height:480}}), errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  let fail=false, styles=0;
  const fixtureStyle={version:8,glyphs:'https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf',sources:{roads:{type:'geojson',data:{type:'FeatureCollection',features:[
   {type:'Feature',properties:{name:'SW 57th Avenue'},geometry:{type:'LineString',coordinates:[[-80.191,25.75],[-80.191,25.77]]}},
   {type:'Feature',properties:{name:'Current area'},geometry:{type:'Point',coordinates:[-80.19,25.761]}}
  ]}}},layers:[{id:'background',type:'background',paint:{'background-color':'#dce2da'}},
   {id:'roads',type:'line',source:'roads',filter:['==',['geometry-type'],'LineString'],paint:{'line-color':'#ffffff','line-width':14}},
   {id:'road-label',type:'symbol',source:'roads',filter:['==',['geometry-type'],'LineString'],layout:{'symbol-placement':'line','text-field':['get','name'],'text-font':['Noto Sans Regular'],'text-size':20,'text-keep-upright':false},paint:{'text-color':'#222222'}},
   {id:'place-label',type:'symbol',source:'roads',filter:['==',['geometry-type'],'Point'],layout:{'text-field':['get','name'],'text-font':['Noto Sans Regular'],'text-size':20,'text-rotation-alignment':'map'},paint:{'text-color':'#222222'}}]};
  await page.route('https://tiles.openfreemap.org/**',r=>{
   if(r.request().url().includes('/fonts/'))return r.fulfill({contentType:'application/x-protobuf',body:require('zlib').gunzipSync(Buffer.from(JSON.parse(fs.readFileSync('scripts/fixtures/noto-sans-0-255.json')).gzipBase64,'base64'))});
   styles++;return fail?r.fulfill({status:503,body:'Unavailable'}):r.fulfill({json:fixtureStyle});
  });
  await page.route('https://appassets.androidplatform.net/**',r=>{
   const name=path.basename(new URL(r.request().url()).pathname);
   return r.fulfill({path:path.join('app/src/main/assets/map',name),contentType:name.endsWith('.js')?'text/javascript':name.endsWith('.css')?'text/css':'text/html'});
  });
  await page.goto('https://appassets.androidplatform.net/assets/map/vector-map.html');
  await page.waitForFunction(()=>typeof window.mapStatus==='function');
  assert.equal(styles,0,'No map requests before GPS');
  await page.evaluate(()=>{window.setNetworkAvailable(false);window.updatePosition(25.761,-80.191,15,180);});
  assert.equal(styles,0,'No map requests while offline');
  await page.evaluate(()=>{window.setMapOptions(true,true);window.setNetworkAvailable(true);});
  await page.waitForFunction(()=>window.mapStatus().totalLoaded>0&&!window.mapStatus().loading);
  assert.equal(await page.evaluate(()=>map.getLayoutProperty('road-label','text-keep-upright')),true,'Road labels flip upright at southbound headings');
  assert.equal(await page.evaluate(()=>map.getLayoutProperty('place-label','text-rotation-alignment')),'viewport','Place labels stay screen-aligned');
  for(const bearing of [0,90,180,270]) {
   await page.evaluate(b=>window.updatePosition(25.761,-80.191,15,b,8,true),bearing);
   await page.waitForFunction(()=>!window.mapStatus().animating);
   assert.ok(Math.abs(await page.evaluate(()=>window.mapStatus().bearing)-bearing)<1);
   // Project a real point ahead of the vehicle, not just the numerical bearing.
   const delta=await page.evaluate(b=>{const r=b*Math.PI/180,c=map.project([-80.191,25.761]),p=map.project([-80.191+Math.sin(r)*0.001/Math.cos(25.761*Math.PI/180),25.761+Math.cos(r)*0.001]);return {x:p.x-c.x,y:p.y-c.y};},bearing);
   assert.ok(Math.abs(delta.x)<2&&delta.y<0,`Travel heading ${bearing} points up`);
  }
  await page.evaluate(()=>window.updatePosition(25.761,-80.191,15,180,8,true));
  await page.waitForFunction(()=>!window.mapStatus().animating);
  await page.waitForFunction(()=>map.loaded());
  assert.ok(await page.evaluate(()=>map.queryRenderedFeatures({layers:['road-label','place-label']}).length)>0,'Actual glyphs render in the rotated map');
  fs.mkdirSync('app/build/outputs/ui-checks',{recursive:true});
  await page.screenshot({path:'app/build/outputs/ui-checks/map-upright-southbound.png'});
  await page.evaluate(()=>window.updatePosition(25.761,-80.191,0,null,8,true));
  await page.waitForFunction(()=>!window.mapStatus().animating);
  assert.equal(await page.evaluate(()=>window.mapStatus().bearing),180,'Hold course through a short stop');
  await page.evaluate(()=>window.clearMotion());
  assert.equal(await page.evaluate(()=>window.mapStatus().bearing),0,'Stale GPS returns north up');
  await page.evaluate(()=>window.updatePosition(25.761,-80.191,0,null,8,true));
  await page.waitForFunction(()=>!window.mapStatus().animating);
  await page.evaluate(()=>window.setMapTheme(true));
  await page.waitForFunction(()=>!window.mapStatus().loading);
  assert.ok(await page.evaluate(()=>document.documentElement.classList.contains('dark-map')));
  fail=true;
  await page.evaluate(()=>{window.setNetworkAvailable(false);window.setNetworkAvailable(true);});
  await page.waitForFunction(()=>window.mapStatus().failed>0);
  assert.match(await page.locator('#status').innerText(),/unavailable/);
  fail=false;
  await page.evaluate(()=>{window.setNetworkAvailable(false);window.setNetworkAvailable(true);});
  await page.waitForFunction(()=>window.mapStatus().failed===0&&!window.mapStatus().loading);
  await page.setViewportSize({width:400,height:260});
  await page.waitForFunction(()=>map.getCanvas().clientWidth===400);
  const location=await page.evaluate(()=>map.project([-80.191,25.761]));
  assert.ok(Math.abs(location.x-200)<2&&Math.abs(location.y-130)<2,'Resume/resize keeps location centered');
  const requestsBeforeDriving=styles;
  for(let i=1;i<=10;i++) {
   await page.evaluate(i=>window.updatePosition(25.761,-80.191+i*0.0001,12,90,8,true),i);
   await page.waitForTimeout(90);
   const bounds=await page.locator('.location-marker').boundingBox();
   assert.ok(Math.abs(bounds.x+bounds.width/2-200)<2&&Math.abs(bounds.y+bounds.height/2-130)<2,'Marker and camera animate together');
  }
  await page.waitForFunction(()=>!window.mapStatus().animating);
  assert.equal(styles,requestsBeforeDriving,'Driving never refetches the style');
  await page.evaluate(()=>window.updatePosition(25.761,-80.1898,12,90,8,true));
  await page.mouse.move(280,120);await page.mouse.down();await page.mouse.move(340,155,{steps:5});await page.mouse.up();
  assert.equal(await page.locator('#follow').innerText(),'Follow off');
  const freeCenter=await page.evaluate(()=>map.getCenter().toArray());
  await page.evaluate(()=>window.updatePosition(25.761,-80.1897,12,90,8,true));
  await page.waitForFunction(()=>!window.mapStatus().animating);
  const heldCenter=await page.evaluate(()=>map.getCenter().toArray());
  assert.ok(heldCenter.every((value,i)=>Math.abs(value-freeCenter[i])<1e-9),'Manual pan cancels follow motion and holds the chosen view');
  assert.deepEqual(errors,[]);
  const fallback=await browser.newPage();
  await fallback.addInitScript(()=>{HTMLCanvasElement.prototype.getContext=()=>null;});
  await fallback.route('https://appassets.androidplatform.net/**',r=>r.fulfill({path:path.join('app/src/main/assets/map',path.basename(new URL(r.request().url()).pathname))}));
  await fallback.goto('https://appassets.androidplatform.net/assets/map/vector-map.html');
  await fallback.waitForURL('**/map.html?fallback=*');
  const webgl1=await browser.newPage();
  await webgl1.addInitScript(()=>{const original=HTMLCanvasElement.prototype.getContext;HTMLCanvasElement.prototype.getContext=function(name,...args){return name==='webgl2'?null:original.call(this,name,...args);};});
  await webgl1.route('https://appassets.androidplatform.net/**',r=>{const name=path.basename(new URL(r.request().url()).pathname);return r.fulfill({path:path.join('app/src/main/assets/map',name),contentType:name.endsWith('.js')?'text/javascript':name.endsWith('.css')?'text/css':'text/html'});});
  await webgl1.goto('https://appassets.androidplatform.net/assets/map/vector-map.html');
  await webgl1.waitForFunction(()=>window.mapStatus?.().renderer==='vector');
  assert.equal(await webgl1.evaluate(()=>window.mapStatus().webglVersion),1,'WebGL 1 remains a vector map');
  await webgl1.route('https://tiles.openfreemap.org/**',r=>r.request().url().includes('/fonts/')?r.fulfill({contentType:'application/x-protobuf',body:require('zlib').gunzipSync(Buffer.from(JSON.parse(fs.readFileSync('scripts/fixtures/noto-sans-0-255.json')).gzipBase64,'base64'))}):r.fulfill({json:fixtureStyle}));
  await webgl1.evaluate(()=>{window.setMapOptions(true,true);window.updatePosition(25.761,-80.191,15,180,8,true);});
  await webgl1.waitForFunction(()=>window.mapStatus().totalLoaded>0&&!window.mapStatus().loading);
  assert.ok(await webgl1.evaluate(()=>map.queryRenderedFeatures({layers:['road-label','place-label']}).length)>0,'WebGL 1 renders real road labels');
  console.log('Vector map: upright rendered labels, four real travel projections, offline/recovery, dark mode, resize and WebGL fallback passed.');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
