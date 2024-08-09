import { test, expect, Page } from '@playwright/test'

import {
  expectUusiLomakeValid,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  teeJaOdotaLomakkeenTallennusta,
} from '../playwright-ataru-utils'
import { unsafeFoldOption } from '../playwright-utils'

const getHakukohteetHeaderLabel = (page: Page) =>
  page.getByTestId('hakukohteet-header-label')

let lomakkeenAvain: string
let lomakkeenId: number
let page: Page

const haeLomakkeenLisaysNappi = (page: Page) =>
  page.getByTestId('add-form-button')

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)

  const lomake = await lisaaLomake(page)

  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)
  lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    const nameInput = page.getByTestId('form-name-input')
    await nameInput.fill('Testilomake')
  })
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

test('Hakukohteet-moduuli', async () => {
  await expectUusiLomakeValid(page, lomakkeenAvain, 'Testilomake')

  await expect(haeLomakkeenLisaysNappi(page)).toBeVisible()

  await expect(getHakukohteetHeaderLabel(page)).toHaveText('Hakukohteet')

  const autoExpandCheckbox = page.getByRole('checkbox', {
    name: 'Näytä hakukohteet hakukohdekohtaisissa kysymyksissä',
  })
  await expect(autoExpandCheckbox).not.toBeChecked()
  await autoExpandCheckbox.click()
  await expect(autoExpandCheckbox).toBeChecked()

  const orderByOpetuskieliCheckbox = page.getByRole('checkbox', {
    name: 'Järjestä hakukohteet opetuskielen mukaan',
  })
  await expect(orderByOpetuskieliCheckbox).not.toBeChecked()
  await orderByOpetuskieliCheckbox.click()
  await expect(orderByOpetuskieliCheckbox).toBeChecked()
})
