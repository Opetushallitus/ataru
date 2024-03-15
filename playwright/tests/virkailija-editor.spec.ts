import { test, expect, Page } from '@playwright/test'
import {
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  teeJaOdotaLomakkeenTallennusta,
} from '../playwright-ataru-utils'
import { unsafeFoldOption } from '../playwright-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let formCount: number

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await page.route('**/lomake-editori/api/forms**', async (route) => {
    const response = await route.fetch()
    const json = await response.json()
    formCount = json['forms'].length
    await route.fulfill({ response })
  })
  await kirjauduVirkailijanNakymaan(page)
})

const formListItems = (page: Page) => {
  return page.locator('.editor-form__list').locator('a')
}

test.describe('Editori', () => {
  test('näyttää lomakelistan', async () => {
    const items = formListItems(page)
    await expect(items).toHaveCount(formCount)
  })

  test('lomakkeen luonti luo ja avaa tyhjän lomakkeen', async () => {
    const lomake = await lisaaLomake(page)

    const lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
    await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
      const nameInput = page.getByTestId('form-name-input')
      await nameInput.fill('Tyhjä lomake')
    })

    const lomakkeet = formListItems(page)
    const ensimmainenLomake = lomakkeet.nth(0)
    await expect(
      ensimmainenLomake.locator('.editor-form__list-form-name')
    ).toHaveText('Tyhjä lomake')

    const lomakeKomponentit = page.locator('.editor-form__component-wrapper')
    await expect(lomakeKomponentit).toHaveCount(2)

    const henkilotiedot = lomakeKomponentit.nth(1)

    await expect(
      henkilotiedot.locator('.editor-form__component-header')
    ).toHaveText('Henkilötiedot')
  })
})
