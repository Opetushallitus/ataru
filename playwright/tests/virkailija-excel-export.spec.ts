import { test, expect, Page } from '@playwright/test'
import fs from 'node:fs/promises'

import { kirjauduVirkailijanNakymaan } from '../playwright-ataru-utils'
import { fixtureFromFile, getFixturePath } from '../playwright-utils'

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

  await page.route(
    '**/lomake-editori/api/forms/latest-by-haku/**',
    async (route) => {
      const fixture = getFixturePath('uusiLomake.json')
      const filecontents = await fs.readFile(fixture, 'utf-8')
      const json = await JSON.parse(filecontents)
      json.key = '551149'
      await route.fulfill({ json })
    }
  )

  await kirjauduVirkailijanNakymaan(page, 'OPINTO-OHJAAJA')
})

test('Excel-latauksen rajainten valinta toimii ja lataus onnistuu', async () => {
  await avaaHaunHakemuksetVirkailijanNakymassa(
    page,
    '1.2.246.562.29.00000000000000018308'
  )
  const showResultsBtn = page.getByTestId('show-results')
  await showResultsBtn.click()

  await page.getByText('Lataa Excel').click()
  const excelPopup = page.locator('.application-handling__excel-request-popup')
  await expect(excelPopup).toBeVisible()
  const valitseTiedotRadio = excelPopup.getByLabel('Valitse excelin tiedot')
  await expect(valitseTiedotRadio).toBeChecked()
  const yleisetTiedotHeading = excelPopup.getByRole('heading', {
    name: 'Hakemuksen yleiset tiedot',
  })

  const yleisetTiedotExpand = yleisetTiedotHeading.getByRole('button', {
    name: '7/7 valittu',
  })

  const yleisetTiedotContent = excelPopup.getByRole('region', {
    name: 'Hakemuksen yleiset tiedot',
  })
  await expect(yleisetTiedotContent).toBeHidden()

  await yleisetTiedotExpand.click()

  const yleisetTiedotCheckedCheckboxes = yleisetTiedotContent.getByRole(
    'checkbox',
    { checked: true }
  )

  await expect(yleisetTiedotCheckedCheckboxes).toHaveCount(7)

  const yleisetTiedotCheckbox = yleisetTiedotHeading.getByLabel(
    'Hakemuksen yleiset tiedot'
  )
  await yleisetTiedotCheckbox.click()

  await expect(yleisetTiedotCheckedCheckboxes).toHaveCount(0)

  await yleisetTiedotHeading
    .getByRole('button', {
      name: '0/7 valittu',
    })
    .click()

  await expect(
    excelPopup.getByRole('checkbox', { name: 'Hakukohteet' })
  ).toBeChecked()

  await expect(
    excelPopup.getByRole('checkbox', { name: 'Henkilötiedot' })
  ).toBeChecked()

  await expect(
    excelPopup.getByRole('checkbox', { name: 'Testikysymys' })
  ).toBeChecked()

  const lataaExcelButton = excelPopup.getByText('Lataa Excel')

  const [download] = await Promise.all([
    page.waitForEvent('download'),
    lataaExcelButton.click(),
  ])

  await expect(download.suggestedFilename()).toMatch(
    /^Loytyi-2-hakemusta_\d{4}-\d\d-\d\d_\d{4}\.xlsx$/
  )

  const kirjoitaTunnisteetRadio = excelPopup.getByLabel('Kirjoita tunnisteet')
  await kirjoitaTunnisteetRadio.click()
  await expect(excelPopup.getByPlaceholder('Kaikki tunnisteet')).toBeVisible()
  await lataaExcelButton.click()

  const [download2] = await Promise.all([
    page.waitForEvent('download'),
    lataaExcelButton.click(),
  ])

  await expect(download2.suggestedFilename()).toMatch(
    /^Loytyi-2-hakemusta_\d{4}-\d\d-\d\d_\d{4}\.xlsx$/
  )
})
