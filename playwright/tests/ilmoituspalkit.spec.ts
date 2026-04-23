import { test, expect, Page } from '@playwright/test'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'

import {
  getHakemuksenLahettamisenOsoite,
  getHakemuksenMuokkausOsoite,
  getHakijanNakymanOsoite,
  getLatestApplicationSecretOsoite,
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
        .fill('Ilmoituspalkit-testilomake')
    }
  )
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenTunnisteet.lomakkeenAvain)
  await page.close()
})

test('Preview-ilmoituspalkki näkyy esikatselussa ennen ensimmäistä lähetystä', async () => {
  await Promise.all([
    page.goto(getHakijanNakymanOsoite(lomakkeenTunnisteet.lomakkeenAvain)),
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenTunnisteet.lomakkeenAvain))
    ),
  ])

  await expect(page.getByTestId('preview-notification-banner')).toBeHidden()
  await expect(page.getByTestId('editing-notification-banner')).toBeHidden()

  await page.getByTestId('preview-answers-button').click()

  await expect(page.getByTestId('preview-notification-banner')).toBeVisible()
  await expect(page.getByTestId('preview-notification-banner')).toContainText(
    'Olet esikatselussa ja hakemusta ei ole vielä lähetetty'
  )
  await expect(page.getByTestId('editing-notification-banner')).toBeHidden()
})

test('Preview-ilmoituspalkki häviää muokkaustilaan palatessa', async () => {
  await page.getByTestId('edit-answers-button').click()

  await expect(page.getByTestId('preview-notification-banner')).toBeHidden()
  await expect(page.getByTestId('editing-notification-banner')).toBeHidden()
})

test('Muokkaustilan ilmoituspalkki näkyy hakemusta muokattaessa', async () => {
  await taytaHenkilotietomoduuli(page)

  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])
  await page.getByTestId('send-feedback-button').click()

  const secretResponse = await page.request.get(
    getLatestApplicationSecretOsoite()
  )
  const secret = await secretResponse.text()

  await page.goto(getHakemuksenMuokkausOsoite(secret))

  await expect(page.getByTestId('editing-notification-banner')).toBeVisible()
  await expect(page.getByTestId('editing-notification-banner')).toContainText(
    'Olet muokkaamassa hakemusta'
  )
  await expect(page.getByTestId('editing-notification-banner')).toContainText(
    'Muista lähettää muutokset!'
  )
  await expect(page.getByTestId('preview-notification-banner')).toBeHidden()
})

test('Muokkaustilan ilmoituspalkki häviää muutosten lähettämisen jälkeen', async () => {
  await page.getByTestId('phone-input').fill('0987654321')
  await expect(page.getByTestId('send-application-button')).toBeEnabled()

  await Promise.all([
    waitForResponse(page, 'PUT', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])
  await page.getByTestId('send-feedback-button').click()

  await expect(page.getByTestId('editing-notification-banner')).toBeHidden()
  await expect(page.getByTestId('preview-notification-banner')).toBeHidden()
})
