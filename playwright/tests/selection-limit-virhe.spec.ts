import { test, expect, Page } from '@playwright/test'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'
import {
  getHakemuksenLahettamisenOsoite,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  taytaHenkilotietomoduuli,
  teeJaOdotaLomakkeenTallennusta,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenTunnisteet: { lomakkeenAvain: string; lomakkeenId: number }

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)

  const lomake = await lisaaLomake(page)

  lomakkeenTunnisteet = {
    lomakkeenAvain: unsafeFoldOption(lomake.lomakkeenAvain),
    lomakkeenId: unsafeFoldOption(lomake.lomakkeenId),
  }

  await teeJaOdotaLomakkeenTallennusta(
    page,
    lomakkeenTunnisteet.lomakkeenId,
    async () => {
      await page
        .getByTestId('form-name-input')
        .fill('Testilomake selection-limit')
    }
  )
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenTunnisteet.lomakkeenAvain)
  await page.close()
})

test('Hakija näkee punaisen virhebannerin kun valintakoepaikka on täyttynyt', async () => {
  await Promise.all([
    page.goto(getHakijanNakymanOsoite(lomakkeenTunnisteet.lomakkeenAvain)),
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenTunnisteet.lomakkeenAvain))
    ),
  ])

  await taytaHenkilotietomoduuli(page)

  await page.route(`**${getHakemuksenLahettamisenOsoite()}`, async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'selection-limit-reached',
          failures: ['Selection limit reached'],
        }),
      })
    } else {
      await route.continue()
    }
  })

  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])

  const errorBanner = page.getByTestId('error-display')
  await expect(errorBanner).toBeVisible()
  await expect(errorBanner).toHaveClass(/application__message-display--error/)
  await expect(errorBanner).toContainText(
    'Valitsemasi valintakoepaikka on täyttynyt'
  )
  await expect(errorBanner).toContainText('Valitse uusi valintakoepaikka')
})

test('Punainen banneri ei näy onnistuneen lähetyksen jälkeen', async () => {
  await page.unroute(`**${getHakemuksenLahettamisenOsoite()}`)

  await Promise.all([
    page.goto(getHakijanNakymanOsoite(lomakkeenTunnisteet.lomakkeenAvain)),
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenTunnisteet.lomakkeenAvain))
    ),
  ])

  await taytaHenkilotietomoduuli(page)

  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])

  await expect(page.getByTestId('error-display')).toBeHidden()
})
