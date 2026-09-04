import { test, expect, Page, Locator } from '@playwright/test'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'
import {
  getHakemuksenLahettamisenOsoite,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  taytaHenkilotietomoduuli,
  teeJaOdotaLomakkeenTallennusta,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenId: number
let lomakkeenAvain: string

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)

  const lomake = await lisaaLomake(page)
  lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    await page.getByTestId('form-name-input').fill('Selection Limit')
  })
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
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

test('Virkailija luo valintarajoitetun painikekysymyksen', async () => {
  await clickComponentToolbar(page, 'lomakeosio')

  const lomakeosio = page.getByTestId(
    'editor-form__wrapperElement-component-wrapper'
  )

  const singleChoice = lomakeosio.getByTestId(
    'editor-form__singleChoice-component-wrapper'
  )
  const options = singleChoice
    .getByTestId('editor-form__multi-options-container')
    .locator('.editor-form__multi-options-wrapper-outer')

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    await lomakeosio
      .locator('.editor-form__text-field')
      .first()
      .fill('Lomakeosio')

    await clickSubComponentToolbar(lomakeosio, 'painikkeet-yksi-valittavissa')

    await singleChoice
      .getByTestId('editor-form__singleChoice-label')
      .fill('Rajoitettu valinta')
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
  })

  await expect(singleChoice.getByLabel('Pakollinen')).toBeChecked()
  await expect(singleChoice.getByLabel('Rajoitettu valinta')).toBeChecked()

  await expect(
    page.locator('.top-banner .flasher span').filter({
      hasText: 'Kaikki muutokset tallennettu',
    })
  ).toBeVisible()
})

test('Hakija täyttää ja lähettää valintarajoitetun hakemuksen', async () => {
  await Promise.all([
    page.goto(getHakijanNakymanOsoite(lomakkeenAvain)),
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenAvain))
    ),
  ])

  await expect(page.locator('.application__wrapper-element')).toHaveCount(2)
  await expect(page.getByTestId('application-header-label')).toHaveText(
    'Selection Limit'
  )
  await expect(page.getByTestId('send-application-button')).toBeDisabled()
  await expect(
    page.locator('.application__invalid-field-status-title')
  ).toHaveText('Tarkista 11 tietoa')

  await taytaHenkilotietomoduuli(page)

  await expect(
    page.locator('.application__invalid-field-status-title')
  ).toHaveText('Tarkista 1 tietoa')

  const disabledOptionLabel = page.locator(
    '.application__form-single-choice-button:disabled + label'
  )
  const checkedOptionLabel = page.locator(
    '.application__form-single-choice-button:checked + label'
  )
  const clickOption = (label: string) =>
    page
      .locator('.application__form-single-choice-button-container label', {
        hasText: label,
      })
      .click()

  await clickOption('Aina tilaa')
  await expect(disabledOptionLabel).toHaveText('Aina täynnä (ei valittavissa)')
  await expect(checkedOptionLabel).toHaveText('Aina tilaa')

  await clickOption('Yksi paikka')
  await expect(disabledOptionLabel).toHaveText('Aina täynnä (ei valittavissa)')
  await expect(checkedOptionLabel).toHaveText('Yksi paikka')

  await expect(page.getByTestId('send-application-button')).toBeEnabled()

  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])

  await expect(page.locator('.application__sent-placeholder-text')).toHaveText(
    'Hakemus lähetetty'
  )
  await expect(page.locator('.application-feedback-form')).toBeVisible()
})
