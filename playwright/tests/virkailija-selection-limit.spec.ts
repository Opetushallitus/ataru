import { test, expect, Page, Locator } from '@playwright/test'
import {
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  teeJaOdotaLomakkeenTallennusta,
} from '../playwright-ataru-utils'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenId: number
let lomakkeenAvain: string

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await Promise.all([
    waitForResponse(page, 'GET', (url) =>
      url.includes('/lomake-editori/api/forms')
    ),
    kirjauduVirkailijanNakymaan(page),
  ])

  const lomake = await lisaaLomake(page)
  lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    await page.getByTestId('form-name-input').fill('Selection Limit')
  })
})

test.afterAll(async ({ request }) => {
  if (lomakkeenAvain) {
    await poistaLomake(request, lomakkeenAvain)
  }
  if (page) {
    await page.close()
  }
})

const clickComponentToolbar = async (
  loc: Page | Locator,
  component: string
) => {
  const toolbar = loc.getByTestId('component-toolbar')
  await toolbar.hover()
  await toolbar.getByTestId(`component-toolbar-${component}`).click()
}

const clickSubComponentToolbar = async (
  loc: Page | Locator,
  component: string
) => {
  const toolbar = loc.getByTestId('component-subform-toolbar')
  await toolbar.hover()
  await toolbar.getByTestId(`component-toolbar-${component}`).click()
}

const optionLabelInput = (option: Locator) =>
  option.locator(
    '.editor-form__text-field:not(.editor-form__text-field--selection-limit)'
  )

const optionSelectionLimitInput = (option: Locator) =>
  option.locator('.editor-form__text-field--selection-limit')

test('Virkailija voi luoda valintarajoitetun painikekysymyksen', async () => {
  await expect(page.getByTestId('form-name-input')).toHaveValue(
    'Selection Limit'
  )
  await expect(page.locator('.editor-form__component-wrapper')).toHaveCount(3)

  await clickComponentToolbar(page, 'lomakeosio')

  const lomakeosio = page.getByTestId(
    'editor-form__wrapperElement-component-wrapper'
  )
  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    await lomakeosio
      .locator('.editor-form__text-field')
      .first()
      .fill('Lomakeosio')
  })

  await clickSubComponentToolbar(lomakeosio, 'painikkeet-yksi-valittavissa')

  const singleChoice = lomakeosio.getByTestId(
    'editor-form__singleChoice-component-wrapper'
  )
  const options = singleChoice
    .getByTestId('editor-form__multi-options-container')
    .locator('.editor-form__multi-options-wrapper-outer')

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    await singleChoice
      .getByTestId('editor-form__singleChoice-label')
      .fill('Rajoitettu valinta')
  })
  await singleChoice.getByLabel('Pakollinen').click()
  await singleChoice.getByLabel('Rajoitettu valinta').click()

  await singleChoice.locator('.editor-form__add-dropdown-item a').click()
  await optionLabelInput(options.nth(0)).fill('Aina täynnä')
  await optionSelectionLimitInput(options.nth(0)).fill('0')

  await singleChoice.locator('.editor-form__add-dropdown-item a').click()
  await optionLabelInput(options.nth(1)).fill('Aina tilaa')

  await singleChoice.locator('.editor-form__add-dropdown-item a').click()
  await optionLabelInput(options.nth(2)).fill('Yksi paikka')
  await optionSelectionLimitInput(options.nth(2)).fill('1')

  await expect(
    singleChoice.getByTestId('editor-form__singleChoice-label')
  ).toHaveValue('Rajoitettu valinta')
  await expect(singleChoice.getByLabel('Pakollinen')).toBeChecked()
  await expect(singleChoice.getByLabel('Rajoitettu valinta')).toBeChecked()
  await expect(options).toHaveCount(3)
  await expect(optionLabelInput(options.nth(0))).toHaveValue('Aina täynnä')
  await expect(optionSelectionLimitInput(options.nth(0))).toHaveValue('0')
  await expect(optionLabelInput(options.nth(1))).toHaveValue('Aina tilaa')
  await expect(optionSelectionLimitInput(options.nth(1))).toHaveValue('')
  await expect(optionLabelInput(options.nth(2))).toHaveValue('Yksi paikka')
  await expect(optionSelectionLimitInput(options.nth(2))).toHaveValue('1')

  await expect(
    page.locator('.top-banner .flasher span').filter({
      hasText: 'Kaikki muutokset tallennettu',
    })
  ).toBeVisible()
})
