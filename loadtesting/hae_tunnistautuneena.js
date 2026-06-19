import { browser } from 'k6/experimental/browser';
import { check } from 'k6';

export const options = {
  scenarios: {
    ui: {
      vus: 2,
      iterations: 2,
      executor: 'shared-iterations',
      options: {
        browser: {
          type: 'chromium',
          headless: false,
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
    console.log("click suomiFiTunnistauduButton " + page.title())
    await Promise.all([page.waitForNavigation(), suomiFiTunnistauduButton.click()]);
    console.log("clicked suomiFiTunnistauduButton " + page.title())
  } catch (e) {
    page.screenshot({ path: 'screenshot_error_after_suomiFiTunnistauduButton_'+ + Date.now() +'.png' })
    console.log("Error in suomiFiTunnistauduButton", e)
  }

  const jatkaPalveluunButton = page.locator('#continue-button')
  jatkaPalveluunButton.click()
  await page.waitForNavigation()
  await page.waitForNavigation() //Todo, tämä tuplasiirtymä vaikuttaa vähän rumalta. Olisiko siistimpi tapa?

  console.log("Waiting for email input...")
  page.waitForSelector('input[data-test-id="email-input"]', {
    timeout: 40000
  });

  const nameInput = page.locator('input[data-test-id="first-name-input"]')
  check(nameInput, {
    'Etunimi on esitäytetty': (n) => {
      return n.inputValue() === 'Nordea'
    }
  })

  const emailInput = page.locator('input[data-test-id="email-input"]')
  const emailInputVerify = page.locator('input[data-test-id="verify-email-input"]')
  const phoneInput = page.locator('input[data-test-id="phone-input"]')
  const postalCodeInput = page.locator('input[data-test-id="postal-code-input"]')
  const sendApplicationButton = page.locator('button[data-test-id="send-application-button"]')

  console.log("Filling hakemus fields...")
  emailInput.type("a@b.com")
  emailInputVerify.type("a@b.com")
  phoneInput.type("1234567890")
  postalCodeInput.fill('') //tunnistautumisen kautta tullut, mahdollisesti ei-validi postinumero pois tieltä
  postalCodeInput.type("20100")

  console.log("ready to submit?")
  sendApplicationButton.click()
  console.log("application submitted, hopefully?")

  page.waitForSelector('button[data-test-id="logout-button"]', {
    timeout: 25000
  });
  const logoutButton = page.locator('button[data-test-id="logout-button"]')
  console.log("clicking logout button")
  logoutButton.click();
  await page.waitForNavigation()

  console.log("waiting for sivu-header1 selector")
  try {
    page.waitForSelector('.Sivu-header1', {
      timeout: 50000
    });
  } catch (e) {
    page.screenshot({ path: 'screenshot_uloskirjautuminen_header_' + Date.now() + '.png' })
    console.log("error while waiting for uloskirjautuminen onnistui header: ", e)
  }
  check(page, {
    'uloskirjautuminen onnistui': (p) => {
      console.log("checking success: " + p.title())
      return p.title() == 'Uloskirjautuminen onnistui! - Opintopolku'
    }
  })
}