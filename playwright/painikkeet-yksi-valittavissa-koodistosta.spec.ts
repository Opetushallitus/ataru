import { test, expect, Page, Response, Locator } from '@playwright/test'
import {
  getSensitiveAnswer,
  kirjauduVirkailijanNakymaan,
  waitForResponse,
} from './playwright-utils'
import * as Option from 'fp-ts/lib/Option'
import * as Record from 'fp-ts/lib/Record'
import { pipe } from 'fp-ts/lib/function'
import { AssertionError } from 'assert'

const getUudenLomakkeenLahettamisenOsoite = () => '/lomake-editori/api/forms'
const getLomakkeenMuuttamisenOsoite = (lomakkeenId: number) =>
  `/lomake-editori/api/forms/${lomakkeenId}`
const getLomakkeenPoistamisenOsoite = () => '/lomake-editori/api/cypress/form'
const getHakijanNakymanOsoite = (lomakkeenAvain: string) =>
  `/hakemus/${lomakkeenAvain}`

const getLomakkeenHaunOsoite = (lomakkeenAvain: string) =>
  `/hakemus/api/form/${lomakkeenAvain}?role=hakija`

const getHakemuksenLahettamisenOsoite = () => '/hakemus/api/application'

const unsafeFoldOption = <T>(o: Option.Option<T>): T => {
  return Option.fold<T, T>(
    () => {
      throw new AssertionError({ message: 'Option was None' })
    },
    (val) => val
  )(o)
}

const getJsonResponseKey = async <T>(res: Response, key: string) => {
  try {
    const body = await res.json()
    return Record.hasOwnProperty(key, body)
      ? Option.some(body[key] as T)
      : Option.none
  } catch (e) {
    return Option.none
  }
}

const clickLisaaLomakeButton = async (page: Page) =>
  await page.getByTestId('add-form-button').click()

const lisaaLomake = async (
  page: Page
): Promise<{
  lomakkeenId: Option.Option<number>
  lomakkeenAvain: Option.Option<string>
}> => {
  const [response, _] = await Promise.all([
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

test.describe.configure({ mode: 'serial' })

const teeJaOdotaLomakkeenTallennusta = async (
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

let page: Page
let lomakkeenTunnisteet: { lomakkeenAvain: string; lomakkeenId: number }

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)

  await page.route(
    '**/lomake-editori/api/tarjonta/haku?form-key=**',
    async (route) => {
      await route.fulfill({
        json: [
          {
            oid: '1.2.246.562.29.00000000000000009710',
            yhteishaku: true,
            'kohdejoukko-uri': 'haunkohdejoukko_11#1',
          },
        ],
      })
    }
  )
  await page.route(
    '**/lomake-editori/api/tarjonta/haku/1.2.246.562.29.00000000000000009710',
    async (route) => {
      await route.fulfill({
        json: { yhteishaku: true, 'kohdejoukko-uri': 'haunkohdejoukko_11#1' },
      })
    }
  )

  const lomake = await lisaaLomake(page)

  lomakkeenTunnisteet = {
    lomakkeenAvain: unsafeFoldOption(lomake.lomakkeenAvain),
    lomakkeenId: unsafeFoldOption(lomake.lomakkeenId),
  }

  await teeJaOdotaLomakkeenTallennusta(
    page,
    lomakkeenTunnisteet.lomakkeenId,
    async () => {
      // FIXME: Jos lomakkeen nimen syöttää liian aikaisin, automaattitallennus ei triggeröidy!
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

test('Painikkeet, yksi valittavissa, koodisto -lomake-elementti', async () => {
  //Painikkeet, yksi valittavissa, koodisto -elementin lisäys
  const valikko = page.getByTestId('component-toolbar')

  const lisaysLinkki = valikko.getByText(
    'Painikkeet, yksi valittavissa, koodisto'
  )
  await valikko.dispatchEvent('mouseover')
  await lisaysLinkki.click()
  const kysymysTeksti = page
    .getByTestId('editor-form__singleChoice-component-question-wrapper')
    .locator('input')
  await kysymysTeksti.fill('Minkä koulutuksen olet suorittanut?')
  await expect(kysymysTeksti).toHaveValue('Minkä koulutuksen olet suorittanut?')

  const koodistoSelect = page.getByTestId(
    'editor-form__select-koodisto-dropdown'
  )

  await teeJaOdotaLomakkeenTallennusta(
    page,
    lomakkeenTunnisteet.lomakkeenId,
    async () => {
      await koodistoSelect.selectOption('Kk-pohjakoulutusvaihtoehdot')
      await page.getByTestId('editor-form__show_koodisto-values__link').click()
      await expect(
        page.getByTestId('editor-form__singleChoice-component-main-label')
      ).toHaveText('Painikkeet, yksi valittavissa, koodisto')
    }
  )

  // Näyttää kysymyksen tekstin
  await expect(kysymysTeksti).toHaveValue('Minkä koulutuksen olet suorittanut?')

  const vastausvaihtoehdot = pipe(
    page,
    (_) => _.getByTestId('editor-form__multi-options-container'),
    (_) => getAllByTestId(_, 'editor-form__koodisto-field')
  )
  // Näyttää valitun koodiston
  await expect(
    vastausvaihtoehdot.filter({
      hasText: 'Korkeakoulun edellyttämät avoimen korkeakoulun opinnot',
    })
  ).toHaveCount(1)

  // Asetetaan tieto arkaluontoiseksi
  const sensitiveAnswer = getSensitiveAnswer(page)
  await expect(sensitiveAnswer).toBeVisible()
  await expect(sensitiveAnswer).not.toBeChecked()
  await sensitiveAnswer.click()
  await expect(sensitiveAnswer).toBeChecked()

  // Aseta kysymys näkyväksi, koska yhteishaussa kysymys on oletuksena piilotettu
  await teeJaOdotaLomakkeenTallennusta(
    page,
    lomakkeenTunnisteet.lomakkeenId,
    async () => {
      await page.getByText('Näkyvyys lomakkeella').first().click()
      await page.getByText('ei näytetä lomakkeella').first().click()
    }
  )

  // Hakijan näkymään siirtyminen lataa hakijan näkymän
  await Promise.all([
    page.goto(getHakijanNakymanOsoite(lomakkeenTunnisteet.lomakkeenAvain)),
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenTunnisteet.lomakkeenAvain))
    ),
  ])
  const lomakkeenNimi = page.getByTestId('application-header-label')
  await expect(lomakkeenNimi).toHaveText('Testilomake')

  // Henkilötietomoduulin täyttäminen
  const inputFieldValues = {
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

  // Hakijan lomake, jolla on "Painikkeet, yksi valittavissa, koodisto"
  await page
    .getByText(
      'Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)',
      { exact: true }
    )
    .click()

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

  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])
  await page.getByTestId('send-feedback-button').click()
  await page.getByTestId('close-feedback-form-button').click()

  // Näyttää lomakkeen nimen
  await expect(lomakkeenNimi).toHaveText('Testilomake')

  // Näyttää kysymyksen tekstin
  await expect(
    page.getByText('Minkä koulutuksen olet suorittanut?')
  ).toBeVisible()

  // Näyttää valitun koodiarvon
  await expect(
    page.getByText(
      'Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)'
    )
  ).toBeVisible()
})
