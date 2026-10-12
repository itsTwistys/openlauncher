'use strict';
(() => {
    const fallback = reason => window.location.replace('map.html?fallback=' + reason); 
    if (!window.maplibregl) { fallback('missing-asset'); return; }
    let map, webglVersion = 0;
    try {
        map = new maplibregl.Map({container: 'map', style: {version: 8, sources: {}, layers: []},
            center: [0, 0], zoom: 2, maxZoom: 19, attributionControl: false,
            dragRotate: false, pitchWithRotate: false, touchPitch: false, fadeDuration: 0});
        webglVersion = map.getCanvas().getContext("webgl2") ? 2 : map.getCanvas().getContext("webgl") ? 1 : 0;
        if (!webglVersion) { fallback("unsupported"); return; }
    } catch (_) { fallback("init-failed"); return; }
    window.map = map;
    map.touchZoomRotate.disableRotation();
    map.addControl(new maplibregl.NavigationControl({showCompass: false}), 'top-left');
    map.addControl(new maplibregl.AttributionControl({compact: false,
        customAttribution: '<a href="https://openfreemap.org">OpenFreeMap</a> · © <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'}));
    map.getCanvas().addEventListener('webglcontextlost', () => fallback('context-lost')); 
    let last = null, marker = null, online = true, gpsFresh = true, follow = true;
    let autoZoom = true, headingUp = false, autoZoomPaused = false, heading = null, lastHeadingAt = 0;
    let speedKmh = 0, zoomTier = 17, lastZoomChange = 0, dark = false, programmatic = false;
    let started = false, loading = false, loaded = 0, failed = 0, totalLoaded = 0, totalErrors = 0, totalTimeouts = 0;
    let styleReady = false, styleGeneration = 0, controller = null, retryTimer = null, watchdog = null, retries = 0;
    let motionFrame = null;
    const status = document.getElementById('status'), followButton = document.getElementById('follow');
    function message() {
        if (!online) return 'Offline · map will retry when connected';
        if (!last) return 'Waiting for GPS · map is ready';
        if (!gpsFresh) return 'GPS stale · showing last location';
        if (failed) return 'Map tiles unavailable · check internet';
        if (loading && !totalLoaded) return 'Loading map tiles…';
        if (!styleReady || !totalLoaded) return 'Waiting for map tiles…';
        if (headingUp && heading === null) return 'Move to establish heading · north up';
        if (autoZoomPaused && autoZoom) return 'Manual zoom · Recenter resumes auto zoom';
        return 'Map ready';
    }
    function renderStatus() {
        const text = message(); status.textContent = text;
        status.style.display = text === 'Map ready' || text.startsWith('Manual zoom') ? 'none' : 'block';
    }
    function setFollow(value) {
        follow = value; followButton.textContent = value ? 'Follow on' : 'Follow off';
        followButton.classList.toggle('active', value); followButton.setAttribute('aria-pressed', value);
    }
    function motion(force = false, animate = false) {
        if (motionFrame !== null) cancelAnimationFrame(motionFrame);
        motionFrame = null;
        let zoom = map.getZoom(), target = zoomTier;
        if (last && follow && autoZoom && !autoZoomPaused) {
            if (force) target = speedKmh < 35 ? 17 : speedKmh < 80 ? 16 : 15;
            else if (zoomTier === 17 && speedKmh > 40) target = speedKmh > 85 ? 15 : 16;
            else if (zoomTier === 16) target = speedKmh < 30 ? 17 : speedKmh > 85 ? 15 : 16;
            else if (zoomTier === 15 && speedKmh < 75) target = speedKmh < 30 ? 17 : 16;
            if (force || (target !== zoomTier && Date.now() - lastZoomChange >= 5000)) {
                zoomTier = target; lastZoomChange = Date.now(); zoom = target;
            }
        }
        const from = marker?.getLngLat().toArray() || last;
        const startCenter = map.getCenter().toArray(), startZoom = map.getZoom(), startBearing = map.getBearing();
        const targetBearing = headingUp && Number.isFinite(heading) ? heading : 0;
        const wrap = value => (value + 540) % 360 - 180;
        const moveMeters = from && last ? new maplibregl.LngLat(...from).distanceTo(new maplibregl.LngLat(...last)) : 0;
        const smooth = animate && gpsFresh && !document.hidden && moveMeters < 250;
        const duration = smooth ? 650 : 0, began = performance.now();
        function draw(t) {
            const point = from && last ? [from[0] + wrap(last[0] - from[0]) * t, from[1] + (last[1] - from[1]) * t] : null;
            if (point && marker) marker.setLngLat(point);
            programmatic = true;
            try { map.jumpTo({center: follow && point ? point : startCenter, zoom: startZoom + (zoom - startZoom) * t,
                bearing: startBearing + wrap(targetBearing - startBearing) * t}); }
            finally { programmatic = false; }
        }
        if (!duration) { draw(1); return; }
        // One animation owns marker, camera and heading; new fixes retarget the current displayed state.
        let lastFrame = 0;
        const step = time => {
            if (time - lastFrame < 30) { motionFrame = requestAnimationFrame(step); return; }
            lastFrame = time;
            const fraction = Math.min(1, (time - began) / duration), eased = fraction * (2 - fraction);
            draw(eased);
            motionFrame = fraction < 1 ? requestAnimationFrame(step) : null;
        };
        motionFrame = requestAnimationFrame(step);
    }

    function retry() {
        if (!online || !last || retryTimer || retries >= 3 || document.hidden) return;
        retryTimer = setTimeout(() => { retryTimer = null; retries++; loadStyle(); }, 5000 * 2 ** retries);
    }
    async function loadStyle() {
        if (!online || !last) return;
        const generation = ++styleGeneration;
        controller?.abort(); controller = new AbortController();
        clearTimeout(watchdog); loading = true; started = true; styleReady = false; failed = 0; loaded = 0;
        watchdog = setTimeout(() => {
            if (generation !== styleGeneration) return;
            loading = false; totalTimeouts++; failed++; controller.abort(); renderStatus(); retry();
        }, 15000);
        renderStatus();
        try {
            const response = await fetch('https://tiles.openfreemap.org/styles/' + (dark ? 'dark' : 'liberty'), {signal: controller.signal});
            if (!response.ok) throw new Error('Style unavailable');
            const style = await response.json();
            if (generation !== styleGeneration) return;
            for (const layer of style.layers) {
                if (layer.type === 'fill' && layer['source-layer'] === 'building') {
                    layer.paint ||= {}; layer.paint['fill-opacity'] = dark ? 0.28 : 0.4;
                }
                if (layer.type !== 'symbol') continue;
                layer.layout ||= {};
                // A driving map prioritizes roads and place names over addresses and minor businesses.
                if (layer['source-layer'] === 'housenumber' || /^poi_/.test(layer.id)) layer.layout.visibility = 'none';
                if (layer['source-layer'] === 'transportation_name') {
                    layer.paint ||= {}; layer.paint['text-halo-width'] = 1.2;
                    if (/highway-name/.test(layer.id)) layer.layout['text-size'] = ['interpolate', ['linear'], ['zoom'], 12, 12, 17, 15, 19, 17];
                }
                layer.layout ||= {};
                const line = ['line', 'line-center'].includes(layer.layout['symbol-placement']);
                if (layer.layout['text-field']) {
                    layer.layout['text-rotation-alignment'] = line ? 'map' : 'viewport';
                    layer.layout['text-pitch-alignment'] = 'viewport';
                    if (line) layer.layout['text-keep-upright'] = true;
                }
                if (!line) layer.layout['icon-rotation-alignment'] = 'viewport';
            }
            style.metadata = {...style.metadata, openlauncherGeneration: generation};
            map.setStyle(style, {diff: false}); motion();
        } catch (_) {
            if (generation !== styleGeneration || !online) return;
            clearTimeout(watchdog); loading = false; failed++; totalErrors++; renderStatus(); retry();
        }
    }
    map.on('style.load', () => { styleReady = started && map.getStyle()?.metadata?.openlauncherGeneration === styleGeneration; renderStatus(); });
    map.on('sourcedata', event => {
        if (event.sourceDataType === 'content' && styleReady) { loaded++; totalLoaded++; }
    });
    map.on('idle', () => {
        if (!started || !styleReady) return;
        loading = false; clearTimeout(watchdog);
        if (!failed) { retries = 0; clearTimeout(retryTimer); retryTimer = null; }
        renderStatus();
    });
    map.on('error', () => { failed++; totalErrors++; renderStatus(); retry(); });
    map.on('dragstart', () => setFollow(false));
    map.on('zoomstart', event => { if (!programmatic && event.originalEvent) autoZoomPaused = true; });
    followButton.onclick = () => { setFollow(!follow); if (follow) { autoZoomPaused = false; motion(true); } renderStatus(); };
    document.getElementById('center').onclick = () => { setFollow(true); autoZoomPaused = false; motion(true); renderStatus(); };
    window.updatePosition = (lat, lon, speed = 0, bearing = null, accuracy = 0, fresh = true) => {
        if (!Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) return;
        const first = !last; last = [lon, lat]; gpsFresh = fresh;
        speedKmh = Number.isFinite(speed) ? Math.max(0, speed) * 3.6 : 0;
        if (fresh && Number.isFinite(bearing) && speedKmh >= 7.2) { heading = (bearing % 360 + 360) % 360; lastHeadingAt = Date.now(); }
        else if (!fresh || Date.now() - lastHeadingAt > 30000) heading = null;
        if (!marker) {
            const element = document.createElement('div'); element.className = 'location-marker';
            element.style.width = element.style.height = '36px';
            element.innerHTML = '<span class="location-halo"></span><span class="location-dot"></span>';
            marker = new maplibregl.Marker({element, rotationAlignment: 'viewport'}).setLngLat(last).addTo(map);
        }
        marker.getElement().classList.toggle('stale', !fresh); motion(first, fresh && !first);
        if (!started && online) loadStyle(); renderStatus();
    };
    window.setMapOptions = (zoom, up) => {
        const changed = autoZoom !== zoom; autoZoom = zoom; headingUp = up;
        if (changed) autoZoomPaused = false; motion(changed); renderStatus();
    };
    window.setMapTheme = value => {
        if (dark === value) return; dark = value;
        document.documentElement.classList.toggle('dark-map', dark);
        if (started && online) loadStyle();
    };
    window.setDashboardStyle = (accent, foreground, font) => {
        if (!/^#[0-9a-f]{6}$/i.test(accent) || !/^#[0-9a-f]{6}$/i.test(foreground)) return;
        const style = document.documentElement.style;
        style.setProperty('--accent', accent); style.setProperty('--on-accent', foreground);
        style.setProperty('--control-font', font === 'JETBRAINS_MONO' ? 'LauncherMono, monospace' : font === 'SOURCE_CODE_PRO' ? 'LauncherCode, monospace' : 'sans-serif');
    };
    window.setNetworkAvailable = value => {
        const changed = online !== value; online = value;
        if (!online) { styleGeneration++; controller?.abort(); clearTimeout(retryTimer); retryTimer = null; clearTimeout(watchdog); loading = false; }
        else if (changed && last) { retries = 0; loadStyle(); } renderStatus();
    };
    window.clearMotion = () => { gpsFresh = false; heading = null; lastHeadingAt = 0; speedKmh = 0; marker?.getElement().classList.add('stale'); motion(); renderStatus(); };
    window.resizeMap = () => { map.resize(); if (follow && last) motion(); };
    window.resumeMap = () => { window.resizeMap(); if (online && last && (!started || failed)) { retries = 0; loadStyle(); } renderStatus(); };
    window.mapStatus = () => ({message: message(), gpsFresh, started, loading, failed, loaded, totalLoaded, totalErrors, totalTimeouts,
        autoZoomPaused, headingUp, bearing: (map.getBearing() + 360) % 360, zoom: map.getZoom(), renderer: 'vector', webglVersion, animating: motionFrame !== null});
    window.addEventListener('resize', window.resizeMap);
    if (window.ResizeObserver) new ResizeObserver(window.resizeMap).observe(document.body);
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) { clearTimeout(retryTimer); retryTimer = null; if (motionFrame !== null) cancelAnimationFrame(motionFrame); motionFrame = null; }
        else window.resumeMap();
    });
    renderStatus();
})();
