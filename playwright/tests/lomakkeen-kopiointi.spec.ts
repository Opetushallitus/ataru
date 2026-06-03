import { test, expect, Page } from '@playwright/test'
import {
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  teeJaOdotaLomakkeenTallennusta,
  getUudenLomakkeenLahettamisenOsoite,
} from '../playwright-ataru-utils'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenId: number
let lomakkeenAvain: string
let kopioLomakeAvain: string

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await page.route('**/lomake-editori/api/tarjonta/haku**', (route) =>
    route.fulfill({
      json: [{ oid: '1.2.246.562.29.00000000000000009710', yhteishaku: true }],
    })
  )
  await page.route(
    '**/lomake-editori/api/tarjonta/haku/1.2.246.562.29.00000000000000009710',
    (route) =>
      route.fulfill({
        json: { yhteishaku: true },
      })
  )

  await kirjauduVirkailijanNakymaan(page)

  const lomake = await lisaaLomake(page)
  lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    await page.getByTestId('form-name-input').fill('Testilomake')
    const toolbar = page.getByTestId('component-toolbar')
    await toolbar.hover()
    await toolbar.getByTestId('component-toolbar-tekstikenttä').click()
    await page
      .getByTestId('tekstikenttä-kysymys')
      .fill('Näytettävän tekstikentän kysymys')
    await page.locator('button.belongs-to-hakukohteet__modal-toggle').click()
    await page
      .locator('.hakukohde-and-hakukohderyhma-visibility-checkbox input')
      .click()
  })
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  if (kopioLomakeAvain) {
    await poistaLomake(request, kopioLomakeAvain)
  }
  await page.close()
})

test('Kopioi lomakkeen', async () => {
  const [response] = await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getUudenLomakkeenLahettamisenOsoite())
    ),
    page.getByTestId('copy-form-button').click(),
  ])

  const body = await response.json()
  kopioLomakeAvain = body['key']

  await expect(page.getByTestId('form-name-input')).toHaveValue(
    'Testilomake - KOPIO'
  )
})

test('Tekstikenttä näytetään kopioidussa lomakkeessa', async () => {
  await page.locator('button.editor-form__component-fold-button').click()
  await expect(
    page.locator('.belongs-to-hakukohteet__modal-toggle-label')
  ).toHaveText('näkyy kaikille')
})
