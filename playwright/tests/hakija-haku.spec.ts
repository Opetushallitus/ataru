import { randomUUID } from 'crypto'
import { test, expect, Page } from '@playwright/test'
import { fillField } from '../playwright-utils'
import {
  asetaTestiHaku,
  haeOletuslomakkeenSisalto,
  kirjauduVirkailijanNakymaan,
  luoLomakeAvaimella,
  poistaLomake,
  poistaTestiHaku,
  taytaHenkilotietomoduuli,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

// Haku 1.2.246.562.29.65950024185 (mock_tarjonta_service.clj) osoittaa
// hakijan lomakkeeksi tämän avaimen ja sillä on tarjonnassa vain yksi
// hakukohde, joten se valikoituu automaattisesti.
const SINGLE_HAKUKOHDE_LOMAKKEEN_AVAIN = '41101b4f-1762-49af-9db0-e3603adae3ad'
const SINGLE_HAKUKOHDE_HAKU_OID = '1.2.246.562.29.65950024185'
const KK_SINGLE_HAKUKOHDE_HAKU_OID = '1.2.246.562.29.65950024190'

// Haut, joissa on useita hakukohteita, rekisteröidään ajonaikaisesti
// mock-tarjontapalveluun /hakemus/test/tarjonta/haku-testirajapinnan kautta
// (ks. asetaTestiHaku), eikä esim. haun 1.2.246.562.29.65950024186 jaettua
// lomakeavainta "...3ae" käytetä, jota hakija-hakukohde.spec.ts jo luo ja
// poistaa omissa testeissään. Jokainen describe-lohko saa oman, satunnaisen
// haku-oidin ja lomakeavaimen, jotta eri Playwright-tiedostot (tai tämän
// tiedoston describe-lohkot) eivät voi törmätä toistensa siivoustoimiin,
// vaikka niitä ajettaisiin eri workereissa rinnakkain.
const MULTIPLE_HAKUKOHDE_OIDS = [
  '1.2.246.562.20.49028196523',
  '1.2.246.562.20.49028196524',
  '1.2.246.562.20.49028196525',
  '1.2.246.562.20.11111111111',
]

// /hakemus/api/haku/:haku-oid on rajoitettu polkuregexillä [0-9\.]+
// (hakija_routes.clj), joten testihaun oidin täytyy olla vain numeroita ja
// pisteitä, tai lomakkeen datan hakeva XHR ei löydä reittiä.
const randomHakuOid = () =>
  `1.2.246.562.29.${Date.now()}${Math.floor(Math.random() * 10000)}`

const getHaunOsoite = (hakuOid: string) => `/hakemus/haku/${hakuOid}`

const getFormSections = (page: Page) =>
  page.locator('.application__wrapper-element')

const getFormFields = (page: Page) => page.locator('.application__form-field')

const getSubmitButton = (page: Page) =>
  page.getByTestId('send-application-button')

const getInvalidFieldsStatus = (page: Page) =>
  page.locator('.application__invalid-field-status-title')

const getSelectedHakukohteet = (page: Page) =>
  page.locator('.application__selected-hakukohde-row')

const getSearchHits = (page: Page) =>
  page.locator('.application__search-hit-hakukohde-row')

const getSearchInput = (page: Page) =>
  page.locator('.application__form-text-input-in-box')

const getSelectedHakukohdeText = async (page: Page) =>
  (
    await page
      .locator('.application__selected-hakukohde-row--content')
      .allTextContents()
  ).join('')

const getSearchHitText = async (page: Page) =>
  (
    await page
      .locator('.application__search-hit-hakukohde-row--content')
      .allTextContents()
  ).join('')

const openHakukohdeSearch = async (page: Page) => {
  await page.locator('.application__hakukohde-selection-open-search').click()
}

const selectNthHakukohdeSearchHit = async (page: Page, n: number) => {
  await page
    .locator('.application__search-hit-hakukohde-row--select-button')
    .nth(n)
    .click()
}

test.describe('hakemus haulla, jolla on yksi hakukohde', () => {
  let page: Page

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(120000)
    page = await browser.newPage()

    await kirjauduVirkailijanNakymaan(page)
    const sisalto = await haeOletuslomakkeenSisalto(page)
    await luoLomakeAvaimella(page, SINGLE_HAKUKOHDE_LOMAKKEEN_AVAIN, sisalto)

    await page.goto(getHaunOsoite(SINGLE_HAKUKOHDE_HAKU_OID))
    await expect(getFormSections(page)).toHaveCount(2, { timeout: 30000 })
  })

  test.afterAll(async ({ request }) => {
    await poistaLomake(request, SINGLE_HAKUKOHDE_LOMAKKEEN_AVAIN)
    await page.close()
  })

  test('lomake latautuu täydellisenä ja ainoa hakukohde on valmiiksi valittuna', async () => {
    await expect(getFormFields(page)).toHaveCount(15)
    await expect(getSubmitButton(page)).toBeDisabled()
    await expect(page.locator('.application__header')).toHaveText('testing2')
    await expect(getInvalidFieldsStatus(page)).toHaveText('Tarkista 10 tietoa')
    await expect(getSelectedHakukohteet(page)).toHaveCount(1)
    await expect(getSearchInput(page)).toBeHidden()
    expect(await getSelectedHakukohdeText(page)).toBe(
      'Ajoneuvonosturinkuljettajan ammattitutkinto – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi | Tutkintonimike | Tarkenne'
    )
  })
})

test.describe('hakemus KK-haulla, jolla on yksi hakukohde', () => {
  let page: Page

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(120000)
    page = await browser.newPage()

    await kirjauduVirkailijanNakymaan(page)
    const sisalto = await haeOletuslomakkeenSisalto(page)
    await luoLomakeAvaimella(page, SINGLE_HAKUKOHDE_LOMAKKEEN_AVAIN, sisalto)

    await page.goto(getHaunOsoite(KK_SINGLE_HAKUKOHDE_HAKU_OID))
    await expect(getFormSections(page)).toHaveCount(2, { timeout: 30000 })
  })

  test.afterAll(async ({ request }) => {
    await poistaLomake(request, SINGLE_HAKUKOHDE_LOMAKKEEN_AVAIN)
    await page.close()
  })

  test('hakukohteen tarkempia tietoja ei näytetä', async () => {
    expect(await getSelectedHakukohdeText(page)).toBe(
      'Ajoneuvonosturinkuljettajan ammattitutkinto – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie'
    )
  })
})

test.describe('hakemus KK-haulla, jolla on useita hakukohteita', () => {
  let page: Page
  const hakuOid = randomHakuOid()
  const lomakkeenAvain = randomUUID()

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(120000)
    page = await browser.newPage()

    await kirjauduVirkailijanNakymaan(page)
    const sisalto = await haeOletuslomakkeenSisalto(page)
    await luoLomakeAvaimella(page, lomakkeenAvain, sisalto)
    await asetaTestiHaku(page, {
      oid: hakuOid,
      ataruLomakeAvain: lomakkeenAvain,
      usePriority: true,
      kohdejoukkoUri: 'haunkohdejoukko_12#',
      hakukohdeOids: MULTIPLE_HAKUKOHDE_OIDS,
    })

    await page.goto(getHaunOsoite(hakuOid))
    await expect(getFormSections(page)).toHaveCount(2, { timeout: 30000 })
  })

  test.afterAll(async ({ request }) => {
    await poistaTestiHaku(request, hakuOid)
    await poistaLomake(request, lomakkeenAvain)
    await page.close()
  })

  test('hakukohteen lisääminen hakutuloksista näyttää oikean valinnan ja hakutulokset', async () => {
    await openHakukohdeSearch(page)
    await fillField(page, getSearchInput(page), 'haku')
    await expect(getSearchHits(page)).toHaveCount(3)

    await selectNthHakukohdeSearchHit(page, 2)
    await expect(getSelectedHakukohteet(page)).toHaveCount(1)
    await expect(getInvalidFieldsStatus(page)).toHaveText('Tarkista 10 tietoa')

    await expect(getSubmitButton(page)).toBeDisabled()
    expect(await getSelectedHakukohdeText(page)).toBe(
      'Testihakukohde 3 – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie'
    )
    expect(await getSearchHitText(page)).toBe(
      'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTestihakukohde 3 – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie'
    )
  })
})

test.describe('hakemus haulla, jolla on useita hakukohteita', () => {
  let page: Page
  const hakuOid = randomHakuOid()
  const lomakkeenAvain = randomUUID()

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(120000)
    page = await browser.newPage()

    await kirjauduVirkailijanNakymaan(page)
    const sisalto = await haeOletuslomakkeenSisalto(page)
    await luoLomakeAvaimella(page, lomakkeenAvain, sisalto)
    await asetaTestiHaku(page, {
      oid: hakuOid,
      ataruLomakeAvain: lomakkeenAvain,
      usePriority: true,
      kohdejoukkoUri: 'haunkohdejoukko_12#1',
      kohdejoukonTarkenne: 'haunkohdejoukontarkenne_1#1',
      hakukohdeOids: MULTIPLE_HAKUKOHDE_OIDS,
    })

    await page.goto(getHaunOsoite(hakuOid))
    await expect(getFormSections(page)).toHaveCount(2, { timeout: 30000 })
  })

  test.afterAll(async ({ request }) => {
    await poistaTestiHaku(request, hakuOid)
    await poistaLomake(request, lomakkeenAvain)
    await page.close()
  })

  test('lomake latautuu täydellisenä eikä hakukohdetta ole valmiiksi valittuna', async () => {
    await expect(getFormFields(page)).toHaveCount(15)
    await expect(getSubmitButton(page)).toBeDisabled()
    await expect(page.locator('.application__header')).toHaveText('testing2')
    await expect(getSelectedHakukohteet(page)).toHaveCount(0)
    await expect(getInvalidFieldsStatus(page)).toHaveText('Tarkista 11 tietoa')
    await expect(getSearchInput(page)).toBeHidden()
  })

  test('hakukohteen lisääminen hakutuloksista onnistuu', async () => {
    await openHakukohdeSearch(page)
    await fillField(page, getSearchInput(page), 'haku')
    await expect(getSearchHits(page)).toHaveCount(3)

    await selectNthHakukohdeSearchHit(page, 2)
    await expect(getSelectedHakukohteet(page)).toHaveCount(1)
    await expect(getInvalidFieldsStatus(page)).toHaveText('Tarkista 10 tietoa')

    await expect(getSubmitButton(page)).toBeDisabled()
  })

  test('valitun hakukohteen poistaminen onnistuu', async () => {
    await page
      .locator('.application__selected-hakukohde-row--remove')
      .first()
      .click()
    await expect(getSelectedHakukohteet(page)).toHaveCount(0)

    await expect(getInvalidFieldsStatus(page)).toHaveText('Tarkista 11 tietoa')
  })

  test('hakutoiveiden uudelleenlisääminen ja lomakkeen täyttäminen onnistuu', async () => {
    await selectNthHakukohdeSearchHit(page, 0)
    await selectNthHakukohdeSearchHit(page, 1)
    // Suljetaan hakukohdehaku, jotta seuraavat henkilötietokentät tulevat
    // näkyviin samalla tavalla kuin ne olisivat, jos hakija ei olisi
    // koskaan avannutkaan hakua.
    await openHakukohdeSearch(page)

    await taytaHenkilotietomoduuli(page, {
      'first-name': 'Etunimi Tokanimi',
      'last-name': 'Sukunimi',
      ssn: '020202A0202',
      email: 'test@example.com',
      'verify-email': 'test@example.com',
      phone: '0123456789',
      address: 'Katutie 12 B',
      'postal-code': '00100',
      'home-town': 'Helsinki',
    })

    await expect(getSearchInput(page)).toBeHidden()
    await expect(getSelectedHakukohteet(page)).toHaveCount(2)
    await expect(getInvalidFieldsStatus(page)).toHaveCount(0)
    await expect(getSubmitButton(page)).toBeEnabled()
  })

  test('hakutoiveiden priorisointijärjestyksen muuttaminen onnistuu', async () => {
    const ensimmainenPrioriteetti =
      'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie'
    const toinenPrioriteetti =
      'Testihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTestihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie'

    await getSelectedHakukohteet(page)
      .nth(0)
      .locator('.application__selected-hakukohde-row--priority-decrease')
      .click()
    await expect
      .poll(() => getSelectedHakukohdeText(page))
      .toBe(toinenPrioriteetti)

    await getSelectedHakukohteet(page)
      .nth(1)
      .locator('.application__selected-hakukohde-row--priority-increase')
      .click()
    await expect
      .poll(() => getSelectedHakukohdeText(page))
      .toBe(ensimmainenPrioriteetti)

    // Varmistetaan, että ensimmäisen hakutoiveen nostaminen ja viimeisen
    // laskeminen eivät tee mitään, koska niitä vastaavat napit ovat
    // disabloituina.
    await getSelectedHakukohteet(page)
      .nth(0)
      .locator('.application__selected-hakukohde-row--priority-increase')
      .click()
    await getSelectedHakukohteet(page)
      .nth(1)
      .locator('.application__selected-hakukohde-row--priority-decrease')
      .click()

    expect(await getSelectedHakukohdeText(page)).toBe(ensimmainenPrioriteetti)
  })

  test('lomakkeen lähettäminen ja lähetetyn hakemuksen tarkasteleminen onnistuu', async () => {
    await getSubmitButton(page).click()
    await expect(
      page.locator('.application__sent-placeholder-text')
    ).toBeVisible()

    const hakukohdeValues = (
      await page
        .locator('.application__hakukohde-selected-list')
        .allTextContents()
    ).join('')
    expect(hakukohdeValues).toBe(
      '1Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie2Testihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie'
    )

    const otherValues = await page
      .locator('.application__text-field-paragraph')
      .allTextContents()
    expect(otherValues).toEqual([
      'Etunimi Tokanimi',
      'Etunimi',
      'Sukunimi',
      'Suomi',
      '020202A0202',
      'test@example.com',
      '0123456789',
      'Suomi',
      'Katutie 12 B',
      '00100',
      'HELSINKI',
      'Helsinki',
      'suomi',
    ])
  })
})
