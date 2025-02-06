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

  test('Massamuutos tilalistauksessa oikeat tilat', async () => {
    await avaaHaunHakemuksetVirkailijanNakymassa(
      page,
      '1.2.246.562.29.00000000000000018308'
    )
    await page.getByTestId('show-results').click()
    await page.getByTestId('mass-update').click()

    const fromStates = page
      .getByTestId('from-list')
      .locator('.application-handling__review-state-row')
    const toStates = page
      .getByTestId('to-list')
      .locator('.application-handling__review-state-row')

    await fromStates.click()
    await toStates.click()

    const expectedStates = [
      'Käsittelemättä',
      'Käsittelyssä',
      'Kutsuttu haast.',
      'Kutsuttu valintak.',
      'Arvioinnissa',
      'Valintaesitys',
      'Käsitelty',
      'Täydennyspyyntö',
    ]

    for (const state of expectedStates) {
      await expect(fromStates.getByText(state)).toBeVisible()
      await expect(toStates.getByText(state)).toBeVisible()
    }
    await expect(fromStates.getByText('Päätös maksettu')).toBeHidden()
    await expect(fromStates.getByText('Laskutuksessa')).toBeHidden()
  })

  test('Massamuutos tilalistauksessa oikeat tilat astu-lomakkeelle', async () => {
    await page.unroute('**/lomake-editori/api/forms/latest/*')
    await page.unroute('**/lomake-editori/api/applications/list')
    await page.route(
      '**/lomake-editori/api/forms/latest/*',
      fixtureFromFile('astuLomake.json')
    )
    await page.route(
      '**/lomake-editori/api/applications/list',
      fixtureFromFile('astuHakemukset.json')
    )
    await page.goto(
      '/lomake-editori/applications/12462c02-b1eb-4a6e-8fad-a93ba8a1cb41?ensisijaisesti=false'
    )
    await page.getByTestId('show-results').click()
    await page.getByTestId('mass-update').click()

    const fromStates = page
      .getByTestId('from-list')
      .locator('.application-handling__review-state-row')
    const toStates = page
      .getByTestId('to-list')
      .locator('.application-handling__review-state-row')

    await fromStates.click()
    await toStates.click()

    const expectedStates = [
      'Käsittelemättä',
      'Käsittelyssä',
      'Käsitelty',
      'Täydennyspyyntö',
      'Päätösmaksu avoin',
      'Päätös maksamatta',
    ]

    for (const state of expectedStates) {
      await expect(fromStates.getByText(state)).toBeVisible()
      await expect(toStates.getByText(state)).toBeVisible()
    }
  })

  test('Massamuutos tilalistauksessa oikeat tilat tutu-lomakkeelle', async () => {
    await page.unroute('**/lomake-editori/api/forms/latest/*')
    await page.unroute('**/lomake-editori/api/applications/list')
    await page.route(
      '**/lomake-editori/api/forms/latest/*',
      fixtureFromFile('tutuLomake.json')
    )
    await page.route(
      '**/lomake-editori/api/applications/list',
      fixtureFromFile('tutuHakemukset.json')
    )
    await page.goto(
      '/lomake-editori/applications/ac234541-c5f0-49e7-8096-08e2b8e88a8a?ensisijaisesti=false'
    )
    await page.getByTestId('show-results').click()
    await page.getByTestId('mass-update').click()

    const fromStates = page
      .getByTestId('from-list')
      .locator('.application-handling__review-state-row')
    const toStates = page
      .getByTestId('to-list')
      .locator('.application-handling__review-state-row')

    await fromStates.click()
    await toStates.click()

    const expectedStates = [
      'Käsittelemättä',
      'Käsittelyssä',
      'Käsitelty',
      'Täydennyspyyntö',
      'Käsittely maksamatta',
      'Käsittely maksettu',
      'Päätösmaksu avoin',
      'Päätös maksamatta',
      'Päätös maksettu',
      'Laskutuksessa',
    ]

    for (const state of expectedStates) {
      await expect(fromStates.getByText(state)).toBeVisible()
      await expect(toStates.getByText(state)).toBeVisible()
    }
  })
})
