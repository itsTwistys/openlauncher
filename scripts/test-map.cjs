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
 await page.route('https://tile.openstreetmap.org/**',r=>{requests++;return fail?r.abort():r.fulfill({contentType:'image/svg+xml',body:'<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256"><rect width="256" height="256" fill="#ddd"/></svg>'});});
 await page.route('https://appassets.androidplatform.net/**',r=>{
  const file=path.basename(new URL(r.request().url()).pathname);
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
 assert.equal(await page.evaluate(()=>marker.getLatLng().lat),25.761);
 await page.evaluate(()=>window.updatePosition(1000,0));
 assert.equal(await page.evaluate(()=>marker.getLatLng().lat),25.761);
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
 // Heading rotates map content; UI and gesture coordinates remain usable.
 await page.evaluate(()=>{window.setMapOptions(true,true);window.updatePosition(25.761,-80.191,15,90);});
 await page.waitForFunction(()=>Math.abs(map.getBearing()-270)<1);
 await page.locator('#center').click();
 assert.equal(await page.evaluate(()=>map.getZoom()),16);
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
 assert.deepEqual(errors,[]);

 console.log('Map checks passed: local assets/CSP, waiting GPS, offline, follow/recenter, tile failure/resume recovery and retained diagnostic counters, resize, heading rotation, speed zoom, hysteresis and manual override.');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
