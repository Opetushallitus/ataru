// Ajaa ClojureScript-yksikkötestit (cljsbuild "test") headless-selaimessa.
// Käännä ensin: lein with-profile test cljsbuild once test
import { chromium } from '@playwright/test'
import { readFile } from 'node:fs/promises'
import path from 'node:path'

const testDir = path.resolve('resources/public/js/test')
const origin = 'http://cljs-unit-tests.local'
const timeoutMs = 5 * 60 * 1000

const contentTypes = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.map': 'application/json',
}

const browser = await chromium.launch({
  channel: process.env.CLJS_TEST_BROWSER_CHANNEL ?? 'chrome',
})

let success = false
try {
  const page = await browser.newPage()
  page.on('console', (msg) => console.log(msg.text()))
  page.on('pageerror', (err) => console.error(err))

  await page.route(`${origin}/**`, async (route) => {
    const { pathname } = new URL(route.request().url())
    if (pathname === '/') {
      return route.fulfill({
        contentType: 'text/html',
        body: '<!doctype html><html><body><script src="test.js"></script></body></html>',
      })
    }
    const file = path.join(testDir, path.normalize(pathname))
    if (!file.startsWith(testDir)) {
      return route.fulfill({ status: 403 })
    }
    try {
      return route.fulfill({
        contentType: contentTypes[path.extname(file)] ?? 'application/octet-stream',
        body: await readFile(file),
      })
    } catch {
      return route.fulfill({ status: 404 })
    }
  })

  await page.goto(`${origin}/`)
  const result = await page.waitForFunction(() => window.cljsTestResult, null, {
    timeout: timeoutMs,
  })
  success = (await result.jsonValue()).success
} catch (err) {
  console.error(err)
} finally {
  await browser.close()
}

process.exit(success ? 0 : 1)
