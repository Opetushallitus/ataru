import { test, expect, Page } from '@playwright/test'

import { kirjauduVirkailijanNakymaan } from '../playwright-ataru-utils'
import { fixtureFromFile } from '../playwright-utils'

let page: Page

test.describe.configure({ mode: 'parallel' })

const haeHaunHakemusListausOsoite = (hakuOid: string) =>
  `/lomake-editori/applications/haku/${hakuOid}?ensisijaisesti=false`

const avaaHaunHakemuksetVirkailijanNakymassa = async (
  page: Page,
  hakuOid: string
) => {
  await page.goto(haeHaunHakemusListausOsoite(hakuOid))
}

test.describe('Hakemusten massatoiminnot ei-reskisterinpitäjälle hakemusten käsittelyä varten', () => {
  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage()

    await page.route(
      '**/lomake-editori/api/valinta-tulos-service/valinnan-tulos/hakemus/?hakemusOid=*',
      (route) => route.fulfill({ json: [] })
    )

    await page.route('**/lomake-editori/api/applications/list', (route) => {
      if (route.request().method() === 'POST') {
        return fixtureFromFile('hakemuksetmassatoiminnoille.json')(route)
      }
    })

    await page.route(
      '**/lomake-editori/api/haku*',
      fixtureFromFile('hautmassatoiminnoille.json')
    )

    await page.route(
      '**/lomake-editori/api/haut*',
      fixtureFromFile('hautmassatoiminnoille.json')
    )

    await kirjauduVirkailijanNakymaan(page, 'OPINTO-OHJAAJA')
  })

  test('Massaviestipainike on näkyvissä ja massaviesti-ikkuna latautuu oikeilla teksteillä', async () => {
    await avaaHaunHakemuksetVirkailijanNakymassa(
      page,
      '1.2.246.562.29.00000000000000018308'
    )
    const showResultsBtn = page.getByTestId('show-results')
    await showResultsBtn.click()
    const massInfoLink = page.locator(
      '.application-handling__mass-information-request-link'
    )
    await massInfoLink.click()
    await expect(
      page.locator('.application-handling__mass-information-request-popup')
    ).toBeVisible()
    await expect(page.getByTestId('mass-send-update-link')).toBeVisible()
    await expect(
      page.getByText(
        'Viestin mukana lähetetään hakemuksen muokkauslinkki hakijalle'
      )
    ).toBeVisible()
    await expect(page.getByText('Lähetä sähköposti 2 hakijalle')).toBeVisible()
  })

  test('Toisen asteen yhteishaussa ei näy massamuistiinpanotoimintoa', async () => {
    await avaaHaunHakemuksetVirkailijanNakymassa(
      page,
      '1.2.246.562.29.10000000001'
    )
    const showResultsBtn = page.getByTestId('show-results')
    await showResultsBtn.click()

    const massInfoLink = page.locator(
      '.application-handling__mass-information-request-link'
    )
    await expect(massInfoLink).toBeVisible()
    await expect(page.getByTestId('mass-review-notes-button')).toBeHidden()
  })

  test('Ei-yhteishaulle massamuistiinpanotoiminto näkyy', async () => {
    await avaaHaunHakemuksetVirkailijanNakymassa(
      page,
      '1.2.246.562.29.00000000000000018308'
    )
    const showResultsBtn = page.getByTestId('show-results')
    await showResultsBtn.click()
    await expect(page.getByTestId('mass-review-notes-button')).toBeVisible()
  })
})
