# QA Automation — Web (Playwright)

## Run locally

```
cd qa-automation/web
npm install
npm run install-browsers   # one-time: downloads Chromium/Firefox/WebKit
docker compose --profile app up -d --build   # from repo root, if not already running
npm test
```

## Check the results

- Terminal shows a live pass/fail list as it runs.
- `npm run report` opens the interactive HTML report (screenshots/video/trace on failure).
- Allure results land in `allure-results/` — see `../README.md` for the combined report.

## What's in here right now

- `tests/gateway-api.spec.js` — runs today, no frontend needed. Uses Playwright's `request`
  fixture (a plain HTTP client, no browser) to smoke-test the gateway directly. This is a
  legitimate, permanent part of the suite, not just a placeholder.
- `tests/web-app.spec.js` — placeholder, `test.skip`'d, for the real React web/admin app once
  it exists. See the comment at the top of that file for the three steps to activate it.

## Why Playwright and not something else for the web app

Free (Apache 2.0), drives real Chromium/Firefox/WebKit (not just headless Chrome), has
first-class auto-waiting so tests don't need manual sleeps, and its trace viewer makes a
failing CI run debuggable without reproducing locally. No paid tier, no browser-minute limits.
