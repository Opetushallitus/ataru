import { test, expect, Page, Locator } from '@playwright/test'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'

import {
  getHakemuksenLahettamisenOsoite,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  getLomakkeenPoistamisenOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
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
      // FIXME: Jos lomakkeen nimen syöttää liian aikaisin, automaattitallennus ei triggeröidy! https://jira.eduuni.fi/browse/OY-4642
      await page.waitForTimeout(800)
      const nameInput = page.getByTestId('form-name-input')
      await nameInput.fill('Testilomake')
    }
  )
})

test.afterAll(async ({ request }) => {
  await request.delete(getLomakkeenPoistamisenOsoite(), {
    data: {
      formKey: lomakkeenTunnisteet.lomakkeenAvain,
    },
  })
  await page.close()
})

test('Hakemuksen täyttö virkailijana', async () => {
  const valikko = page.getByTestId('component-toolbar')
  await valikko.dispatchEvent('mouseover')

  const lisaysLinkki = valikko.getByText('Infoteksti').first()

  await lisaysLinkki.click()

  const infoField = page.getByTestId('info-input-field')

  await teeJaOdotaLomakkeenTallennusta(
    page,
    lomakkeenTunnisteet.lomakkeenId,
    async () => {
      await infoField.fill('Tämä teksti näkyy')
    }
  )

  // Hakijan näkymään siirtyminen lataa hakijan näkymän
  await Promise.all([
    page.goto(getHakijanNakymanOsoite(lomakkeenTunnisteet.lomakkeenAvain)),
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenTunnisteet.lomakkeenAvain))
    ),
  ])

  const lomakkeenNimi = page.getByTestId('application-header-label')
  await expect(lomakkeenNimi).toHaveText('Testilomake')

  await expect(page.getByText('Tarkista 10 tietoa')).toBeVisible()

  await taytaHenkilotietomoduuli(page, {
    'first-name': 'Virkailijan',
    'last-name': 'Täyttämä',
    ssn: '020202A0202',
    email: 'test@example.com',
    'verify-email': 'test@example.com',
    phone: '0123456789',
    address: 'Katutie 12 B',
    'postal-code': '00100',
    'home-town': 'Forssa',
  })

  await expect.soft(page.getByText('Tämä teksti näkyy')).toBeVisible()

  await expect(page.getByText('Tarkista')).toBeHidden()

  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])
  await page.getByTestId('send-feedback-button').click()

  // Näyttää täytetyn henkilötietomoduulin
  await expect.soft(lomakkeenNimi).toHaveText('Testilomake')
  await expect.soft(page.getByText('Virkailijan')).toHaveCount(2)
  await expect.soft(page.getByText('Täyttämä')).toBeVisible()
  await expect.soft(page.getByText('020202A0202')).toBeVisible()
  await expect.soft(page.getByText('test@example.com')).toBeVisible()
  await expect.soft(page.getByText('0123456789')).toBeVisible()
  await expect.soft(page.getByText('Katutie 12 B')).toBeVisible()
  await expect.soft(page.getByText('00100')).toBeVisible()
  await expect(page.getByText('HELSINKI')).toBeVisible()
})
