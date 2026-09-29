'use strict';
const map = L.map('map', { attributionControl: true }).setView([0, 0], 2);
const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
});
let marker = null, follow = true, last = null, started = false, online = true;
let loading = false, failed = 0, loaded = 0, retries = 0, retryTimer = null, watchdog = null;
const followButton = document.getElementById('follow');
const statusBox = document.getElementById('status');
function message() {
    if (!online) return 'Offline · map will retry when connected';
    if (!last) return 'Waiting for GPS · map is ready';
    if (loading) return 'Loading map tiles…';
    if (failed) return 'Some map tiles unavailable · check internet';
    if (!loaded) return 'Waiting for map tiles…';
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
followButton.onclick = () => { setFollow(!follow); if (follow && last) map.panTo(last); };
document.getElementById('center').onclick = () => { setFollow(true); if (last) map.panTo(last); };
map.on('dragstart', () => setFollow(false));
tiles.on('loading', () => {
    loading = true; failed = 0; loaded = 0;
    clearTimeout(watchdog);
    watchdog = setTimeout(() => { loading = false; failed = Math.max(1, failed); renderStatus(); scheduleRetry(); }, 15000);
    renderStatus();
});
tiles.on('tileerror', () => { failed++; renderStatus(); });
tiles.on('tileload', () => { loaded++; });
tiles.on('load', () => {
    loading = false; clearTimeout(watchdog);
    if (failed) scheduleRetry(); else { retries = 0; cancelRetry(); }
    renderStatus();
});
window.updatePosition = (lat, lon) => {
    if (!Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) return;
    last = [lat, lon];
    if (!marker) {
        map.setView(last, 15);
        marker = L.circleMarker(last, { radius: 10, color: '#fff', weight: 3, fillColor: '#1769e0', fillOpacity: 1 }).addTo(map);
    } else { marker.setLatLng(last); if (follow) map.panTo(last, { animate: true, duration: 0.6 }); }
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
window.resumeMap = () => {
    map.invalidateSize({ pan: false });
    retries = 0;
    if (online && started && (loading || failed || !loaded)) reloadTiles();
    renderStatus();
};
window.mapStatus = () => ({ message: message(), loading, failed, loaded });
if (window.ResizeObserver) new ResizeObserver(() => map.invalidateSize({ pan: false })).observe(document.body);
window.addEventListener('resize', () => map.invalidateSize({ pan: false }));
document.addEventListener('visibilitychange', () => {
    if (document.hidden) { cancelRetry(); clearTimeout(watchdog); }
    else window.resumeMap();
});
renderStatus();
