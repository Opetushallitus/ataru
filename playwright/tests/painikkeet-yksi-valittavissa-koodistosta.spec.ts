import { test, expect, Page, Locator } from '@playwright/test'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'
import { pipe } from 'fp-ts/lib/function'
import {
  asetaKysymyksenVastausArkaluontoiseksi,
  getHakemuksenLahettamisenOsoite,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  getLomakkeenPoistamisenOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  taytaHenkilotietomoduuli,
  teeJaOdotaLomakkeenTallennusta,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

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
  await valikko.dispatchEvent('mouseover')

  const lisaysLinkki = valikko.getByText(
    'Painikkeet, yksi valittavissa, koodisto'
  )
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
  await asetaKysymyksenVastausArkaluontoiseksi(page)

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

  await taytaHenkilotietomoduuli(page)
  // Näyttää täytetyn henkilötietomoduulin
  await expect(page.getByTestId('postal-office-input')).toHaveValue('HELSINKI')

  // Hakijan lomake, jolla on "Painikkeet, yksi valittavissa, koodisto"
  await page
    .getByText(
      'Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)',
      { exact: true }
    )
    .click()

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

  // Painikkeet, yksi valittavissa, koodisto -toiminnon arvojen näyttäminen hakemuksen lähettämisen jälkeen
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
