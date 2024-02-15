import { Page, Locator, expect } from '@playwright/test'
import { getJsonResponseKey, waitForResponse } from './playwright-utils'
import * as Option from 'fp-ts/lib/Option'

export const getSensitiveAnswer = (page: Page | Locator): Locator =>
  page.getByTestId('checkbox-sensitive-answer')

export const getUudenLomakkeenLahettamisenOsoite = () =>
  '/lomake-editori/api/forms'
export const getLomakkeenMuuttamisenOsoite = (lomakkeenId: number) =>
  `/lomake-editori/api/forms/${lomakkeenId}`
export const getLomakkeenPoistamisenOsoite = () =>
  '/lomake-editori/api/cypress/form'
export const getHakijanNakymanOsoite = (lomakkeenAvain: string) =>
  `/hakemus/${lomakkeenAvain}`

export const getLomakkeenHaunOsoite = (lomakkeenAvain: string) =>
  `/hakemus/api/form/${lomakkeenAvain}?role=hakija`

export const getHakemuksenLahettamisenOsoite = () => '/hakemus/api/application'

export const clickLisaaLomakeButton = async (page: Page) =>
  await page.getByTestId('add-form-button').click()

export const lisaaLomake = async (
  page: Page
): Promise<{
  lomakkeenId: Option.Option<number>
  lomakkeenAvain: Option.Option<string>
}> => {
  const [response] = await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getUudenLomakkeenLahettamisenOsoite())
    ),
    clickLisaaLomakeButton(page),
  ])
  return Promise.resolve({
    lomakkeenId: await getJsonResponseKey<number>(response, 'id'),
    lomakkeenAvain: await getJsonResponseKey<string>(response, 'key'),
  })
}

export const teeJaOdotaLomakkeenTallennusta = async (
  page: Page,
  lomakeId: number,
  fn: () => Promise<void>
) => {
  await Promise.all([
    waitForResponse(page, 'PUT', (url) =>
      url.includes(getLomakkeenMuuttamisenOsoite(lomakeId))
    ),
    fn(),
  ])
}

export const kirjauduVirkailijanNakymaan = async (
  page: Page,
  ticket?: string
): Promise<void> => {
  await page.goto(`/lomake-editori/auth/cas?ticket=${ticket || 'DEVELOPER'}`)
}

export const asetaKysymyksenVastausArkaluontoiseksi = async (
  page: Page | Locator
) => {
  const sensitiveAnswer = getSensitiveAnswer(page)
  await expect(sensitiveAnswer).toBeVisible()
  await expect(sensitiveAnswer).not.toBeChecked()
  await sensitiveAnswer.click()
  await expect(sensitiveAnswer).toBeChecked()
}

const defaultHenkiloInputFieldValues = {
  'first-name': 'Frank Zacharias',
  'last-name': 'Testerberg',
  ssn: '160600A999C',
  email: 'f.t@ex.com',
  'verify-email': 'f.t@ex.com',
  phone: '0401234567',
  address: 'Yliopistonkatu 4',
  'postal-code': '00100',
  'home-town': 'Forssa',
}

export const taytaHenkilotietomoduuli = async (
  page: Page,
  inputFieldValues = defaultHenkiloInputFieldValues
) => {
  // Henkilötietomoduulin täyttäminen

  for (const [idPrefix, value] of Object.entries(inputFieldValues)) {
    const loc = page.getByTestId(`${idPrefix}-input`)
    if (idPrefix === 'home-town') {
      await loc.selectOption(value)
      await expect(loc).toHaveValue('061')
    } else {
      await loc.fill(value)
      await expect(loc).toHaveValue(value)
    }

    // FIXME: Jos lomake täytetään ilman taukoja, lähettäessä jotkin lomakkeen kentät ovat tyhjiä, vaikka yllä tarkistetaan, että kenttään on mennyt syötetty arvo.
    await page.waitForTimeout(100)
  }
}
