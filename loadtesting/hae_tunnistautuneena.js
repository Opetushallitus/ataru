import { browser } from 'k6/experimental/browser';
import { sleep } from 'k6';

export const options = {
  scenarios: {
    ui: {
      vus: 2,
      iterations: 4,
      executor: 'shared-iterations',
      options: {
        browser: {
          type: 'chromium',
          headless: true,
          timeout: '120s'
        },
      },
    },
  },
  thresholds: {
    checks: ['rate==1.0'],
  },
};

export default async function () {
  const context = browser.newContext();
  const page = context.newPage();

  try {
    await page.goto('https://untuvaopintopolku.fi/hakemus/85a8f75c-a64c-40cf-92e4-e47102d22db6?lang=fi', { waitUntil: 'load'});
    console.log("initial load done" + page.title())

    const tunnistauduButton = page.locator('[data-test-id="tunnistautuminen-button"]');
    await Promise.all([page.waitForNavigation(), tunnistauduButton.click()]);

    const fakeVetumaButton = page.locator('#fakevetuma2')
    await Promise.all([page.waitForNavigation(), fakeVetumaButton.click()]);

    const hetuInput = page.locator('#hetu_input')
    const hetu = '210281-9988'
    hetuInput.type(hetu)
    console.log("hetu input done: " + hetu)
    const suomiFiTunnistauduButton = page.locator('button[id="tunnistaudu"]')

    try {
      console.log("click suomiFiTunnistauduButton" + page.title())
      page.screenshot({ path: 'screenshot_suomiFiTunnistauduButton.png' })
      await Promise.all([page.waitForNavigation(), suomiFiTunnistauduButton.click()]);
      console.log("clicked suomiFiTunnistauduButton" + page.title())
      page.screenshot({ path: 'screenshot_after_suomiFiTunnistauduButton.png' })

    } catch (e) {
      console.log("Error in suomiFiTunnistauduButton", e)
    }
    try {
      const jatkaPalveluunButton = page.locator('#continue-button')
      await Promise.all([page.waitForNavigation(), jatkaPalveluunButton.click()]);
    } catch (e) {
      console.log("error in promise...", e)
    } finally {
      console.log("mid-promise finally")
    }
    // check(page, {
    //   header: (p) => p.locator('h2').textContent() == 'Opintopolku - hakulomake - testilomake (ht2)',
    // });
  } catch (e) {
    console.log("Unforeseen error in fakesuomifi-tunnistautuminen: ", e)
  }
  const emailInput = page.locator('input[data-test-id="email-input"]')
  const emailInputVerify = page.locator('input[data-test-id="verify-email-input"]')
  const phoneInput = page.locator('input[data-test-id="phone-input"]')
  const postalCodeInput = page.locator('input[data-test-id="postal-code-input"]')
  const sendApplicationButton = page.locator('button[data-test-id="send-application-button')

  emailInput.type("a@b.com")
  emailInputVerify.type("a@b.com")
  phoneInput.type("1234567890")
  postalCodeInput.fill('')
  postalCodeInput.type("20100")
  sleep(2)
  console.log("ready to submit?")
  sendApplicationButton.click()
  var sleepAmount = Math.random() * 50
  console.log("Sleeping for " + sleepAmount)
  sleep(sleepAmount)
  console.log("application submitted, hopefully?")
}