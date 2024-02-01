import { test, expect, Page, Locator } from '@playwright/test'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'
import { pipe } from 'fp-ts/lib/function'
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
  //Painikkeet, yksi valittavissa, koodisto -elementin lisäys
  const valikko = page.getByTestId('component-toolbar')
  await valikko.dispatchEvent('mouseover')

  const lisaysLinkki = valikko.getByText('Infoteksti, koko ruutu')

  await lisaysLinkki.click()
  await page.getByTestId('info-input-field')
  //await lisaysLinkki.click()

  //const infoField = await page.getByTestId('info-input-field').locator('input')
  //await infoField.fill("Tämä teksti näkyy");

  const [newPage] = await Promise.all([
    page.context().waitForEvent('page'),
    await page.getByTestId('application-preview-link-fi').click(),
  ])

  await newPage.waitForLoadState()

  // Hakijan näkymään siirtyminen lataa hakijan näkymän
  await waitForResponse(newPage, 'GET', (url) =>
    url.includes(lomakkeenTunnisteet.lomakkeenAvain)
  )
  const lomakkeenNimi = newPage.getByTestId('application-header-label')
  await expect(lomakkeenNimi).toHaveText('Testilomake')

  await taytaHenkilotietomoduuli(newPage)
  // Näyttää täytetyn henkilötietomoduulin
  await expect(newPage.getByTestId('postal-office-input')).toHaveValue(
    'HELSINKI'
  )

  // Näyttää näkyväksi tarkoitetun infotekstin
  await expect(newPage.getByText('Tämä teksti näkyy')).toBeVisible()

  await Promise.all([
    waitForResponse(newPage, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    newPage.getByTestId('send-application-button').click(),
  ])
  await newPage.getByTestId('send-feedback-button').click()

  // Näyttää lomakkeen nimen
  await expect(lomakkeenNimi).toHaveText('Testilomake')
})
