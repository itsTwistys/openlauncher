"""Check public feed contracts, retaining responses only as CI test artifacts."""
import concurrent.futures
import datetime as dt
import gzip
import json
from pathlib import Path
import urllib.request
from zoneinfo import ZoneInfo

out = Path("app/build/outputs/sports-probe")
out.mkdir(parents=True, exist_ok=True)
now = dt.datetime.now(ZoneInfo("America/New_York"))
day = (now - dt.timedelta(days=1)).strftime("%Y%m%d")
leagues = {"NFL": "football/nfl", "NBA": "basketball/nba", "NHL": "hockey/nhl", "MLB": "baseball/mlb", "F1": "racing/f1"}
urls = {key: f"https://site.api.espn.com/apis/site/v2/sports/{path}/scoreboard?dates={day}&limit=200" for key, path in leagues.items()}
urls.update(F1_RESULTS=f"https://api.jolpi.ca/ergast/f1/{now.year}/last/results.json",
            F1_UPCOMING=f"https://api.jolpi.ca/ergast/f1/{now.year}/races.json?limit=100")

def probe(item):
    name, url = item
    try:
        request = urllib.request.Request(url, headers={"Accept": "application/json", "User-Agent": "OpenLauncher/feed-check"})
        with urllib.request.urlopen(request, timeout=12) as response:
            raw = response.read(5_000_001)
            if raw.startswith(b"\x1f\x8b"):
                raw = gzip.decompress(raw)
            assert len(raw) <= 5_000_000
            data = json.loads(raw)
            if name in leagues:
                assert isinstance(data.get("events"), list)
                count = len(data["events"])
            else:
                count = len(data["MRData"]["RaceTable"]["Races"])
            (out / f"{name}.json").write_text(json.dumps(data))
            return {"feed": name, "status": response.status, "events": count}
    except Exception as error:
        # Provider downtime is separate from an app build failure. Parser tests
        # validate every successfully obtained fixture; deterministic tests cover outages.
        return {"feed": name, "error": type(error).__name__, "status": getattr(error, "code", None)}

with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
    results = list(executor.map(probe, urls.items()))
(out / "probe-meta.json").write_text(json.dumps({"now": int(now.timestamp() * 1000), "results": results}))
for result in results:
    print(json.dumps(result))
