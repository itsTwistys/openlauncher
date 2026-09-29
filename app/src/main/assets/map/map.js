'use strict';
const map = L.map('map', { attributionControl: true, rotate: true, rotateControl: false, dragRotate: false, touchRotate: false, shiftKeyRotate: false }).setView([0, 0], 2);
const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
});
let marker = null, follow = true, last = null, started = false, online = true;
let autoZoom = true, headingUp = false, autoZoomPaused = false, programmaticZoom = false;
let speedKmh = 0, heading = null, zoomTier = 17, lastZoomChange = 0;
function withProgrammaticZoom(action) { programmaticZoom = true; try { action(); } finally { programmaticZoom = false; } }
function updateMotionView(force = false) {
    if (headingUp && Number.isFinite(heading)) map.setHeading(heading);
    else { map.stopHeadingUp(); map.setBearing(0); }
    if (!last || !follow || !autoZoom || autoZoomPaused) return;
    // Hysteresis prevents repeated zoom changes around the speed boundaries.
    let target = zoomTier;
    if (force) target = speedKmh < 35 ? 17 : speedKmh < 80 ? 16 : 15;
    else if (zoomTier === 17 && speedKmh > 40) target = speedKmh > 85 ? 15 : 16;
    else if (zoomTier === 16) target = speedKmh < 30 ? 17 : speedKmh > 85 ? 15 : 16;
    else if (zoomTier === 15 && speedKmh < 75) target = speedKmh < 30 ? 17 : 16;
    if (force || (target !== zoomTier && Date.now() - lastZoomChange >= 5000)) {
        zoomTier = target; lastZoomChange = Date.now();
        withProgrammaticZoom(() => map.setZoom(target, { animate: false }));
    }
}
window.setMapOptions = (zoom, up) => {
    const changed = zoom !== autoZoom;
    autoZoom = zoom; headingUp = up;
    if (changed) autoZoomPaused = false;
    updateMotionView(changed); renderStatus();
};
window.clearMotion = () => { heading = null; speedKmh = 0; updateMotionView(); renderStatus(); };
let loading = false, failed = 0, loaded = 0, retries = 0, retryTimer = null, watchdog = null;
let totalLoaded = 0, totalErrors = 0, totalTimeouts = 0;
const followButton = document.getElementById('follow');
const statusBox = document.getElementById('status');
function message() {
    if (!online) return 'Offline · map will retry when connected';
    if (!last) return 'Waiting for GPS · map is ready';
    if (loading) return 'Loading map tiles…';
    if (failed) return 'Some map tiles unavailable · check internet';
    if (!loaded) return 'Waiting for map tiles…';
    if (headingUp && !Number.isFinite(heading)) return 'Heading unavailable · north up';
    if (autoZoom && autoZoomPaused) return 'Manual zoom · Recenter resumes auto zoom';
    return 'Map ready';
}
function renderStatus() {
    statusBox.textContent = message();
    statusBox.style.display = online && last && loaded && !failed && !loading ? 'none' : 'block';
}
function cancelRetry() { clearTimeout(retryTimer); retryTimer = null; }
function scheduleRetry() {
    if (!online || !started || retryTimer || retries >= 3 || document.hidden) return;
    retryTimer = setTimeout(() => { retryTimer = null; retries++; reloadTiles(); }, 5000 * Math.pow(2, retries));
}
function reloadTiles() {
    if (!online || !started) return;
    failed = 0; loaded = 0;
    tiles.redraw();
    renderStatus();
}
function setFollow(value) {
    follow = value;
    followButton.textContent = value ? 'Follow on' : 'Follow off';
    followButton.classList.toggle('active', value);
    followButton.setAttribute('aria-pressed', value);
}
followButton.onclick = () => { setFollow(!follow); if (follow && last) { autoZoomPaused = false; map.panTo(last); updateMotionView(true); } };
document.getElementById('center').onclick = () => { setFollow(true); autoZoomPaused = false; if (last) map.panTo(last); updateMotionView(true); renderStatus(); };
map.on('dragstart', () => setFollow(false));
map.on('zoomstart', () => { if (!programmaticZoom) autoZoomPaused = true; });
tiles.on('loading', () => {
    loading = true; failed = 0; loaded = 0;
    clearTimeout(watchdog);
    watchdog = setTimeout(() => { loading = false; totalTimeouts++; failed = Math.max(1, failed); renderStatus(); scheduleRetry(); }, 15000);
    renderStatus();
});
tiles.on('tileerror', () => { failed++; totalErrors++; renderStatus(); });
tiles.on('tileload', () => { loaded++; totalLoaded++; });
tiles.on('load', () => {
    loading = false; clearTimeout(watchdog);
    if (failed) scheduleRetry(); else { retries = 0; cancelRetry(); }
    renderStatus();
});
window.updatePosition = (lat, lon, speedMps = 0, travelHeading = null) => {
    if (!Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) return;
    speedKmh = Number.isFinite(speedMps) ? Math.max(0, speedMps) * 3.6 : 0;
    heading = Number.isFinite(travelHeading) && speedKmh >= 7.2 ? ((travelHeading % 360) + 360) % 360 : null;
    const first = !last;
    last = [lat, lon];
    if (!marker) {
        withProgrammaticZoom(() => map.setView(last, autoZoom ? 17 : 15, { animate: false }));
        marker = L.circleMarker(last, { radius: 10, color: '#fff', weight: 3, fillColor: '#1769e0', fillOpacity: 1 }).addTo(map);
    } else { marker.setLatLng(last); if (follow) map.panTo(last, { animate: true, duration: 0.6 }); }
    updateMotionView(first);
    if (!started && online) { started = true; tiles.addTo(map); }
    renderStatus();
};
window.setNetworkAvailable = available => {
    const changed = online !== available;
    online = available;
    if (!online) cancelRetry();
    if (online && changed) {
        retries = 0;
        if (!started && last) { started = true; tiles.addTo(map); }
        else reloadTiles();
    }
    renderStatus();
};
let resizeFrame = null;
window.resizeMap = () => {
    if (resizeFrame !== null) cancelAnimationFrame(resizeFrame);
    resizeFrame = requestAnimationFrame(() => {
        resizeFrame = null;
        // A zero-sized AndroidView during wake/expansion must not become the cached map size.
        const bounds = document.getElementById('map').getBoundingClientRect();
        if (bounds.width > 0 && bounds.height > 0) map.invalidateSize({ pan: false, animate: false });
    });
};
window.resumeMap = () => {
    window.resizeMap();
    retries = 0;
    if (online && started && (loading || failed || !loaded)) reloadTiles();
    renderStatus();
};
window.mapStatus = () => ({ message: message(), started, loading, failed, loaded, totalLoaded, totalErrors, totalTimeouts, autoZoomPaused, headingUp, bearing: map.getBearing(), zoom: map.getZoom() });
if (window.ResizeObserver) new ResizeObserver(() => window.resizeMap()).observe(document.body);
window.addEventListener('resize', () => window.resizeMap());
document.addEventListener('visibilitychange', () => {
    if (document.hidden) { cancelRetry(); clearTimeout(watchdog); }
    else window.resumeMap();
});
renderStatus();
