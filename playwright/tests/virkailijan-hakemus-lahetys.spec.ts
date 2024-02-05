import { test, expect, Page, Locator } from '@playwright/test'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'

import {
  getHakemuksenLahettamisenOsoite,
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

const getAllByTestId = (loc: Locator | Page, testId: string) =>
  loc.locator(`[data-test-id=${testId}]`)

test('Hakemuksen täyttö virkailijana', async () => {
  const valikko = page.getByTestId('component-toolbar')
  await valikko.dispatchEvent('mouseover')

  const lisaysLinkki = valikko.getByText('Infoteksti').first()

  await lisaysLinkki.click()

  const infoField = page.getByTestId('info-input-field')
  await infoField.fill('Tämä teksti näkyy')

  const [newPage] = await Promise.all([
    page.context().waitForEvent('page'),
    page.getByTestId('application-preview-link-fi').click(),
  ])

  await newPage.waitForLoadState()

  // Hakijan näkymään siirtyminen lataa hakijan näkymän
  await waitForResponse(newPage, 'GET', (url) =>
    url.includes(lomakkeenTunnisteet.lomakkeenAvain)
  )
  const lomakkeenNimi = newPage.getByTestId('application-header-label')
  await expect(lomakkeenNimi).toHaveText('Testilomake')

  await expect(newPage.getByText('Tarkista 10 tietoa')).toBeVisible()

  await taytaHenkilotietomoduuli(newPage, {
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

  await expect(newPage.getByText('Tämä teksti näkyy')).toBeVisible()

  await expect(newPage.getByText('Tarkista')).toBeHidden()

  await Promise.all([
    waitForResponse(newPage, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    newPage.getByTestId('send-application-button').click(),
  ])
  await newPage.getByTestId('send-feedback-button').click()

  // Näyttää täytetyn henkilötietomoduulin
  await expect(lomakkeenNimi).toHaveText('Testilomake')
  await expect(newPage.getByText('Virkailijan')).toHaveCount(2)
  await expect(newPage.getByText('Täyttämä')).toBeVisible()
  await expect(newPage.getByText('020202A0202')).toBeVisible()
  await expect(newPage.getByText('test@example.com')).toBeVisible()
  await expect(newPage.getByText('0123456789')).toBeVisible()
  await expect(newPage.getByText('Katutie 12 B')).toBeVisible()
  await expect(newPage.getByText('00100')).toBeVisible()
  await expect(newPage.getByText('HELSINKI')).toBeVisible()
})
