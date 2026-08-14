import { randomUUID } from 'crypto'
import { test, expect, Page } from '@playwright/test'
import { fillField, selectOption, waitForResponse } from '../playwright-utils'
import {
  asetaPriorisoivaHakukohderyhma,
  asetaRajaavaHakukohderyhma,
  asetaTestiHaku,
  asetaTestiHakukohde,
  getApplicationSecretById,
  getHakemuksenLahettamisenOsoite,
  haeOletuslomakkeenSisalto,
  kirjauduVirkailijanNakymaan,
  luoLomakeAvaimella,
  luoTestiHaunOid,
  poistaLomake,
  poistaPriorisoivaHakukohderyhma,
  poistaRajaavaHakukohderyhma,
  poistaTestiHaku,
  poistaTestiHakukohde,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

// Haku ja lomake rekisteröidään ajonaikaisesti (ks. asetaTestiHaku) sen
// sijaan, että käytettäisiin haun 1.2.246.562.29.65950024187 staattista,
// dedikoitua avainta "hakija-hakukohteen-hakuaika-test-form" suoraan —
// näin tämä testi ei ole riippuvainen mock_tarjonta_service.clj:n
// staattisesta testidatasta.
const lomakkeenAvain = randomUUID()
const HAKU_OID = luoTestiHaunOid()
const HAKUKOHDERYHMA_OID = '1.2.246.562.28.00000000001'

// Nämä neljä hakukohdetta ovat olemassa olevaa, sisällöltään valmista
// tarjonnan testidataa (mock_tarjonta_service.clj), jotka liitetään tämän
// testin omaan hakuun asetaTestiHaku-kutsun hakukohdeOids-listan kautta.
// Vain HAKUKOHDE_HAKUAIKA_OHI_OID:n hakuaika on tarjonnan mock-datassa jo
// mennyt umpeen.
const HAKUKOHDE_OID = '1.2.246.562.20.49028100003'
const HAKUKOHDE_HAKUAIKA_OHI_OID = '1.2.246.562.20.49028100001'
const HAKUKOHDE_OSA_HAKUAJOISTA_OID = '1.2.246.562.20.49028100002'
const HAKUKOHDE_MUU_OID = '1.2.246.562.20.490281000035'

const METADATA = {
  'created-by': {
    oid: '1.2.246.562.24.1000000',
    date: '2018-03-21T15:45:29.23+02:00',
    name: 'Teppo Testinen',
  },
  'modified-by': {
    oid: '1.2.246.562.24.1000000',
    date: '2018-03-22T07:55:08Z',
    name: 'Teppo Testinen',
  },
}

const customField = (
  id: string,
  labelFi: string,
  extra: Record<string, unknown>
) => ({
  label: { fi: labelFi },
  fieldClass: 'formField',
  fieldType: 'textField',
  id,
  params: {},
  metadata: METADATA,
  ...extra,
})

// Vastaavat kysymykset kuin spec/ataru/fixtures/db/browser_test_db.clj:n
// hakija-hakukohteen-hakuaika-test-form -lomakkeella oli.
const HAKUAJAT_OHI_KENTAT = [
  customField('hakuajat-ohi', 'Hakukohteiden hakuajat ohi!', {
    'belongs-to-hakukohteet': [HAKUKOHDE_HAKUAIKA_OHI_OID],
  }),
  customField('osa-hakuajoista-ohi', 'Osa hakuajoista voimassa!', {
    'belongs-to-hakukohteet': [
      HAKUKOHDE_OSA_HAKUAJOISTA_OID,
      HAKUKOHDE_HAKUAIKA_OHI_OID,
    ],
  }),
  customField('kaikki-hakuajat-voimassa', 'Kaikki hakuajat voimassa!', {
    'belongs-to-hakukohteet': [HAKUKOHDE_OSA_HAKUAJOISTA_OID, HAKUKOHDE_OID],
  }),
  customField(
    'assosiaatio-hakukohderyhman-kautta',
    'Assosiaatio hakukohderyhmän kautta',
    { 'belongs-to-hakukohderyhma': [HAKUKOHDERYHMA_OID] }
  ),
]

const getHakukohdeOsoite = (hakukohdeOid: string) =>
  `/hakemus/hakukohde/${hakukohdeOid}`

const getAlterApplicationToHakuaikaloppuOsoite = (secret: string) =>
  `/hakemus/alter-application-to-hakuaikaloppu-for-secret/${secret}`

const getFormSections = (page: Page) =>
  page.locator('.application__wrapper-element')

const getSubmitButton = (page: Page) =>
  page.getByTestId('send-application-button')

const getFieldById = (page: Page, id: string) => page.locator(`[id="${id}"]`)

const openHakukohdeSearch = async (page: Page) => {
  await page.locator('.application__hakukohde-selection-open-search').click()
}

// Hakutulosten järjestys ei ole sama kuin hakukohdeOids-listan järjestys
// (mm. päättyneet hakuajat sortataan viimeiseksi), joten valitaan hakukohde
// aina sen omalla oidilla eikä indeksillä.
const selectHakukohde = async (page: Page, hakukohdeOid: string) => {
  await page
    .locator(
      `.application__search-hit-hakukohde-row--select-button[data-hakukohde-oid="${hakukohdeOid}"]`
    )
    .click()
}

const kysymysKoskeeHakukohteitaText = (page: Page, fieldId: string) =>
  page
    .locator('.application__form-field')
    .filter({ has: getFieldById(page, fieldId) })
    .locator('.application__question_hakukohde_names_container')

let page: Page
let modifySecret: string

test.beforeAll(async ({ browser }) => {
  test.setTimeout(120000)
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)

  const oletusSisalto = await haeOletuslomakkeenSisalto(page)
  await luoLomakeAvaimella(page, lomakkeenAvain, [
    ...oletusSisalto,
    ...HAKUAJAT_OHI_KENTAT,
  ])
  await asetaTestiHaku(page, {
    oid: HAKU_OID,
    ataruLomakeAvain: lomakkeenAvain,
    usePriority: true,
    hakukohdeOids: [
      HAKUKOHDE_HAKUAIKA_OHI_OID,
      HAKUKOHDE_OSA_HAKUAJOISTA_OID,
      HAKUKOHDE_OID,
      HAKUKOHDE_MUU_OID,
    ],
  })
  await asetaTestiHakukohde(page, { oid: HAKUKOHDE_OID, hakuOid: HAKU_OID })

  await asetaPriorisoivaHakukohderyhma(page, HAKU_OID, HAKUKOHDERYHMA_OID, [
    [HAKUKOHDE_OID],
    [HAKUKOHDE_HAKUAIKA_OHI_OID],
  ])
  await asetaRajaavaHakukohderyhma(page, HAKU_OID, HAKUKOHDERYHMA_OID, 2)

  // Varmistetaan suoraan rajapinnasta, että hakukohde löytyy lomakkeineen,
  // ennen kuin luotetaan selaimen renderöintiin.
  const hakukohdeResponse = await page.request.get(
    `/hakemus/api/hakukohde/${HAKUKOHDE_OID}?role=hakija`
  )
  if (!hakukohdeResponse.ok()) {
    throw new Error(
      `Hakukohdetta ${HAKUKOHDE_OID} ei saatu haettua lomakkeen ${lomakkeenAvain} kautta: ${hakukohdeResponse.status()} ${await hakukohdeResponse.text()}`
    )
  }

  await Promise.all([
    waitForResponse(page, 'GET', (url) =>
      url.includes(`/hakemus/api/hakukohde/${HAKUKOHDE_OID}`)
    ),
    page.goto(getHakukohdeOsoite(HAKUKOHDE_OID)),
  ])

  await expect(getFormSections(page)).toHaveCount(2, { timeout: 30000 })

  // Lisätään toinen hakutoive. HAKUKOHDE_HAKUAIKA_OHI_OID:ia (...001) ei voi
  // valita tässä vaiheessa, koska sen hakuaika on jo mennyt umpeen eikä
  // hakija voi enää valita sitä hakukohdehausta.
  await openHakukohdeSearch(page)
  await selectHakukohde(page, HAKUKOHDE_OSA_HAKUAJOISTA_OID)

  await fillField(
    page,
    page.getByTestId('first-name-input'),
    'Etunimi Tokanimi'
  )
  await page.getByTestId('first-name-input').press('Tab')
  await expect(page.getByTestId('preferred-name-input')).toHaveValue('Etunimi')

  await fillField(page, page.getByTestId('last-name-input'), 'Sukunimi')
  await fillField(page, page.getByTestId('ssn-input'), '020202A0202')
  await fillField(page, page.getByTestId('email-input'), 'test@example.com')
  await fillField(
    page,
    page.getByTestId('verify-email-input'),
    'test@example.com'
  )
  await fillField(page, page.getByTestId('phone-input'), '0123456789')
  await fillField(page, page.getByTestId('address-input'), 'Katutie 12 B')
  await fillField(page, page.getByTestId('postal-code-input'), '40100')
  await expect(page.getByTestId('postal-office-input')).not.toHaveValue('')

  await selectOption(page, page.getByTestId('home-town-input'), '179')
  await selectOption(page, page.getByTestId('language-input'), 'FI')

  // Ei täytetä hakuaikakysymyksiä tässä vaiheessa: mikään niistä ei ole
  // pakollinen, eikä "hakuajat-ohi" ole vielä edes näkyvissä, koska sen
  // hakukohde (...49028100001) ei ole hakuajan päättymisen vuoksi
  // valittavissa hakukohdehausta ennen alter-application-to-hakuaikaloppu
  // -kutsua alempana.
  await expect(getSubmitButton(page)).toBeEnabled()

  const [submitResponse] = await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    getSubmitButton(page).click(),
  ])
  await expect(
    page.locator('.application__sent-placeholder-text')
  ).toBeVisible()

  const submitPayload = (await submitResponse.json()) as { id?: number }
  const applicationId = submitPayload.id
  if (!applicationId) {
    throw new Error('Missing application id in submit response')
  }

  modifySecret = await getApplicationSecretById(page, applicationId)

  // Muutetaan hakemuksen ensimmäinen hakutoive sellaiseksi, jonka hakuaika on
  // ohi, jotta muokkausnäkymässä nähdään hakuaika-riippuvaisten kenttien
  // disabloituminen
  await page.request.get(getAlterApplicationToHakuaikaloppuOsoite(modifySecret))

  await page.goto(`/hakemus?modify=${modifySecret}`)
  await expect(getFormSections(page)).toHaveCount(2, { timeout: 30000 })
})

test.afterAll(async ({ request }) => {
  await poistaTestiHakukohde(request, HAKUKOHDE_OID)
  await poistaTestiHaku(request, HAKU_OID)
  await poistaLomake(request, lomakkeenAvain)
  await poistaPriorisoivaHakukohderyhma(request, HAKU_OID, HAKUKOHDERYHMA_OID)
  await poistaRajaavaHakukohderyhma(request, HAKU_OID, HAKUKOHDERYHMA_OID)
  await page.close()
})

test('disabloi komponentit kun hakuaika on ohi (ja sallii, kun jokin hakuaika on voimassa)', async () => {
  await expect(
    page.locator('.application__selected-hakukohde-row--remove')
  ).toHaveCount(2)
  await expect(
    page.locator('.application__selected-hakukohde-row--remove[disabled]')
  ).toHaveCount(1)

  await expect(getFieldById(page, 'hakuajat-ohi')).toBeDisabled()
  await expect(getFieldById(page, 'osa-hakuajoista-ohi')).toBeEnabled()
  await expect(getFieldById(page, 'kaikki-hakuajat-voimassa')).toBeEnabled()
  await expect(
    getFieldById(page, 'assosiaatio-hakukohderyhman-kautta')
  ).toBeEnabled()

  await expect(kysymysKoskeeHakukohteitaText(page, 'hakuajat-ohi')).toHaveText(
    'Kysymys kuuluu hakukohteisiin: Näytä hakukohteet (1)'
  )
  await expect(
    kysymysKoskeeHakukohteitaText(page, 'osa-hakuajoista-ohi')
  ).toHaveText('Kysymys kuuluu hakukohteisiin: Näytä hakukohteet (2)')
  await expect(
    kysymysKoskeeHakukohteitaText(page, 'kaikki-hakuajat-voimassa')
  ).toHaveText('Kysymys kuuluu hakukohteisiin: Näytä hakukohteet (1)')
  await expect(
    kysymysKoskeeHakukohteitaText(page, 'assosiaatio-hakukohderyhman-kautta')
  ).toHaveText('Kysymys kuuluu hakukohteisiin: Näytä hakukohteet (1)')
})

test('ei salli hakutoiveen lisäämistä väärässä prioriteettijärjestyksessä', async () => {
  await openHakukohdeSearch(page)
  await selectHakukohde(page, HAKUKOHDE_OID)

  await expect(getSubmitButton(page)).toBeDisabled()
  await expect(
    page.locator('.application__invalid-field-status-title')
  ).toHaveText('Tarkista 1 tietoa')

  // Banneri renderöityy uudelleen tiuhaan validointitilan päivittyessä, joten
  // tavallinen klikkaus saattaa jäädä odottamaan elementin vakautumista
  // loputtomiin ("element was detached from the DOM, retrying"). Ohitetaan
  // vakausvaatimus, koska elementti on silti aina olemassa ja klikattavissa.
  const invalidFieldStatusTitle = page.locator(
    '.application__invalid-field-status-title'
  )
  // eslint-disable-next-line playwright/no-force-option
  await invalidFieldStatusTitle.click({ force: true })

  await expect(
    page.locator('.application__selected-hakukohde-row--offending-priorization')
  ).toHaveCount(2)

  const invalidFieldNames = await page
    .locator('.application__invalid-fields > a > div')
    .allTextContents()
  expect(invalidFieldNames.join(';')).toBe('Hakukohteet')
})

test('ei salli hakutoiveen lisäämistä kun rajoittavan hakukohderyhmän raja on täynnä', async () => {
  await expect(
    page.locator('.application__search-hit-hakukohde-row--limit-reached')
  ).toHaveCount(1)
})
