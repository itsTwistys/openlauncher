// Run with Playwright installed: node scripts/test-map.cjs
const {chromium}=require('playwright');
const fs=require('fs');
const path=require('path');
const assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.CHROME_PATH?{executablePath:process.env.CHROME_PATH}:{}),args:['--no-sandbox']});
 try {
 const page=await browser.newPage({viewport:{width:800,height:480}});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 let fail=false, requests=0;
 await page.route('https://tile.openstreetmap.org/**',r=>{requests++;return fail?r.abort():r.fulfill({contentType:'image/svg+xml',body:'<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256"><rect width="256" height="256" fill="#e7eadf"/><path d="M0 70H256M0 170H256M65 0V256M190 0V256" stroke="#fff" stroke-width="12"/><path d="M0 128H256" stroke="#e7bf85" stroke-width="16"/></svg>'});});
 await page.route('https://appassets.androidplatform.net/**',r=>{
  const file=path.basename(new URL(r.request().url()).pathname);
  if (file.endsWith('.ttf')) return r.fulfill({path:path.join('app/src/main/res/font',file),contentType:'font/ttf'});
  return r.fulfill({path:path.join('app/src/main/assets/map',file),contentType:file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html'});
 });
 await page.goto('https://appassets.androidplatform.net/assets/map/map.html');
 assert.match(await page.locator('#status').innerText(),/Waiting for GPS/);
 assert.equal(requests,0,'No tile requests before GPS');
 assert.equal(await page.evaluate(()=>window.mapStatus().started),false);
 assert.equal(await page.evaluate(()=>window.mapStatus().totalErrors),0);
 await page.evaluate(()=>{window.setNetworkAvailable(false);window.updatePosition(25.76,-80.19);});
 assert.equal(requests,0,'No tile requests when offline');
 await page.evaluate(()=>window.setNetworkAvailable(true));
 await page.waitForFunction(()=>window.mapStatus().loaded>0&&!window.mapStatus().loading);
 assert.equal(await page.evaluate(()=>marker.getLatLng().lat),25.76);
 await page.evaluate(()=>window.updatePosition(25.761,-80.191));
 await page.waitForFunction(()=>Math.abs(marker.getLatLng().lat-25.761)<0.0000001);
 await page.evaluate(()=>window.updatePosition(1000,0));
 await page.waitForFunction(()=>Math.abs(marker.getLatLng().lat-25.761)<0.0000001);
 await page.evaluate(()=>map.fire('dragstart'));
 assert.equal(await page.locator('#follow').getAttribute('aria-pressed'),'false');
 await page.locator('#center').click();
 assert.equal(await page.locator('#follow').getAttribute('aria-pressed'),'true');
 fail=true;
 await page.evaluate(()=>{window.setNetworkAvailable(false);window.updatePosition(26.76,-81.19);window.setNetworkAvailable(true);});
 await page.waitForFunction(()=>window.mapStatus().failed>0&&!window.mapStatus().loading);
 assert.match(await page.locator('#status').innerText(),/unavailable/);
 const failedTotal = await page.evaluate(()=>window.mapStatus().totalErrors);
 assert.ok(failedTotal>0,'Tile errors counted independently of current loading batch');
 fail=false;
 await page.evaluate(()=>window.resumeMap());
 await page.waitForFunction(()=>window.mapStatus().loaded>0&&!window.mapStatus().loading&&!window.mapStatus().failed);
 assert.equal(await page.evaluate(()=>window.mapStatus().totalErrors),failedTotal,'Recovery retains error evidence');
 assert.ok(await page.evaluate(()=>window.mapStatus().totalLoaded)>0,'Tile successes counted');
 await page.setViewportSize({width:400,height:260});
 await page.waitForTimeout(250);
 assert.equal(await page.evaluate(()=>map.getSize().x),400);
 assert.equal(await page.locator('#status').isVisible(),false);
 // A head unit can lay out its WebView at zero height during ignition wake.
 // Resume must use the restored bounds rather than retaining the hidden size.
 await page.evaluate(()=>{document.getElementById('map').style.height='0px';window.resizeMap();});
 await page.waitForTimeout(100);
 await page.evaluate(()=>{document.getElementById('map').style.height='100%';window.resumeMap();});
 await page.waitForFunction(()=>map.getSize().y===260);
 const centered=await page.evaluate(()=>map.latLngToContainerPoint(marker.getLatLng()));
 assert.ok(Math.abs(centered.x-200)<2 && Math.abs(centered.y-130)<2,'Follow marker stays centered after resize');
 const tileLoadsBeforeResize=await page.evaluate(()=>window.mapStatus().totalLoaded);
 await page.setViewportSize({width:800,height:480});
 await page.waitForFunction(()=>map.getSize().x===800&&map.getSize().y===480);
 assert.ok(await page.evaluate(()=>window.mapStatus().totalLoaded)>=tileLoadsBeforeResize);
 const zoomBounds=await page.locator('.leaflet-control-zoom-in').boundingBox();
 assert.ok(zoomBounds.width>=48&&zoomBounds.height>=48,'Map zoom targets are at least 48px');
 fs.mkdirSync('app/build/outputs/ui-checks',{recursive:true});
 await page.screenshot({path:'app/build/outputs/ui-checks/map-recovered-landscape.png'});
 // Heading rotates map content; UI and gesture coordinates remain usable.
 await page.evaluate(()=>{window.setMapOptions(true,true);window.updatePosition(25.761,-80.191,15,90);});
 await page.waitForFunction(()=>Math.abs(map.getBearing()-270)<1);
 await page.locator('#center').click();
 assert.equal(await page.evaluate(()=>map.getZoom()),16);
 // Slow traffic/missing bearing retains a recent trusted heading; stale GPS resets it.
 await page.evaluate(()=>window.updatePosition(25.761,-80.191,0,null,8,true));
 assert.ok(Math.abs(await page.evaluate(()=>map.getBearing())-270)<1);
 for (let i=1;i<=12;i++) {
   await page.evaluate(i=>window.updatePosition(25.761+i*0.0001,-80.191,15,90,8,true),i);
   await page.waitForTimeout(80);
 }
 await page.waitForFunction(()=>!window.mapStatus().loading);
 const movingCenter=await page.evaluate(()=>map.latLngToContainerPoint(marker.getLatLng()));
 assert.ok(Math.abs(movingCenter.x-400)<2 && Math.abs(movingCenter.y-240)<2,'Rotated moving map stays centered');
 assert.ok(await page.locator('.leaflet-tile-loaded').count()>0,'Tiles retained while following and rotating');
 await page.evaluate(()=>{lastHeadingAt=Date.now()-31000;window.updatePosition(25.761,-80.191,0,null,8,true);});
 assert.equal(await page.evaluate(()=>map.getBearing()),0,'Old course expires instead of inventing a direction');
 // Faster speeds widen the view; boundaries resist small speed fluctuations.
 await page.evaluate(()=>{lastZoomChange=0;window.updatePosition(25.761,-80.191,30,0);});
 assert.equal(await page.evaluate(()=>map.getZoom()),15);
 await page.evaluate(()=>{lastZoomChange=0;window.updatePosition(25.761,-80.191,22,0);});
 assert.equal(await page.evaluate(()=>map.getZoom()),15);
 // Manual zoom remains intact until recenter, which restores speed-based zoom.
 await page.evaluate(()=>map.setZoom(18,{animate:false}));
 await page.evaluate(()=>window.updatePosition(25.761,-80.191,30,0));
 assert.equal(await page.evaluate(()=>map.getZoom()),18);
 assert.equal(await page.evaluate(()=>window.mapStatus().autoZoomPaused),true);
 await page.locator('#center').click();
 assert.equal(await page.evaluate(()=>map.getZoom()),15);
 // Heading 0 is valid. Missing/stale heading returns to north-up.
 await page.waitForFunction(()=>Math.abs(map.getBearing())<1);
 await page.evaluate(()=>window.updatePosition(25.761,-80.191,15,90));
 await page.waitForFunction(()=>Math.abs(map.getBearing()-270)<1);
 await page.evaluate(()=>window.clearMotion());
 assert.equal(await page.evaluate(()=>map.getBearing()),0);
 await page.evaluate(()=>window.setMapOptions(false,false));
 await page.evaluate(()=>map.setZoom(18,{animate:false}));
 await page.evaluate(()=>window.updatePosition(25.761,-80.191,30,90));
 assert.equal(await page.evaluate(()=>map.getZoom()),18);
 assert.equal(await page.evaluate(()=>map.getBearing()),0);
 await page.evaluate(()=>window.updatePosition(25.761,-80.191,0,null,25,false));
 assert.equal(await page.locator('.location-marker.stale').count(),1);
 assert.match(await page.locator('#status').innerText(),/GPS stale/);
 await page.evaluate(()=>window.updatePosition(25.761,-80.191,0,null,25,true));
 assert.equal(await page.locator('.location-marker.stale').count(),0);
 // Theme changes retain the map, cached tiles, position and blue marker.
 const loadsBeforeTheme = await page.evaluate(()=>totalLoaded);
 await page.evaluate(()=>window.setMapTheme(true));
 assert.equal(await page.locator('html.dark-map').count(),1);
 assert.equal(await page.locator('.leaflet-tile-pane').evaluate(e=>getComputedStyle(e).filter),'none','Avoid a filtered moving pane on head-unit WebViews');
 assert.notEqual(await page.locator('.leaflet-tile').first().evaluate(e=>getComputedStyle(e).filter),'none');
 assert.equal(await page.locator('.leaflet-marker-pane').evaluate(e=>getComputedStyle(e).filter),'none');
 assert.equal(await page.evaluate(()=>totalLoaded),loadsBeforeTheme);
 await page.screenshot({path:'app/build/outputs/ui-checks/map-dark-landscape.png'});
 // Normal tile loading while moving must not flash an overlay over the existing map.
 await page.evaluate(()=>tiles.fire('loading'));
 assert.equal(await page.locator('#status').isVisible(),false);
 await page.evaluate(()=>tiles.fire('load'));
 await page.evaluate(()=>{
   window.testMap=map; window.redraws=0;
   const redraw=tiles.redraw.bind(tiles); tiles.redraw=()=>{window.redraws++; return redraw();};
 });
 for(let i=1;i<=12;i++) {
   await page.evaluate(i=>{window.setMapOptions(false,false); window.updatePosition(25.761+i*0.000015,-80.191,12,90,8,true);},i);
   await page.waitForTimeout(100);
 }
 await page.waitForFunction(()=>Math.abs(marker.getLatLng().lat-(25.761+12*0.000015))<0.0000001);
 assert.equal(await page.evaluate(()=>window.testMap===map),true);
 assert.equal(await page.evaluate(()=>window.redraws),0,'Driving fixes do not redraw cached tiles');
 await page.evaluate(()=>window.setMapTheme(false));
 assert.equal(await page.locator('html.dark-map').count(),0);
 await page.evaluate(()=>window.setDashboardStyle('#33aa88','#111111','JETBRAINS_MONO'));
 await page.evaluate(()=>document.fonts.ready);
 assert.equal(await page.locator('#center').evaluate(e=>getComputedStyle(e).backgroundColor),'rgb(51, 170, 136)');
 assert.ok(await page.locator('#center').evaluate(e=>getComputedStyle(e).fontFamily.includes('LauncherMono')));
 assert.equal(await page.evaluate(()=>document.fonts.check('14px LauncherMono')),true);
 assert.equal(await page.evaluate(()=>window.redraws),0,'Dashboard appearance does not reload tiles');
 assert.deepEqual(errors,[]);

 console.log('Map checks passed: local assets/CSP, waiting GPS, offline, follow/recenter, tile failure/resume recovery and retained diagnostic counters, resize, heading rotation, speed zoom, hysteresis and manual override.');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
