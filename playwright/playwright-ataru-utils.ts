import { Page, Locator, expect, APIRequestContext } from '@playwright/test'
import {
  fillField,
  getJsonResponseKey,
  selectOption,
  unsafeFoldOption,
  waitForResponse,
} from './playwright-utils'
import * as Option from 'fp-ts/lib/Option'

// Muodollisesti pätevä (ks. ataru.ssn/ssn? tarkistusmerkkilaskenta), mutta
// joka kutsukerralla eri henkilötunnus. Kiinteitä henkilötunnuksia (esim.
// "020202A0202") ei pidä käyttää testeissä, jotka hakevat hakemuksia
// henkilötunnuksen perusteella lomake-/hakurajauksetta — Playwright-testit
// ajetaan pysyvää, ajojen välillä säilyvää tietokantaa vasten (toisin kuin
// vanhat speclj-selaintestit, joilla oli oma, joka ajolla nollautuva
// tietokantansa), joten samaa kiinteää henkilötunnusta uudelleenkäyttävät
// testit kerryttävät siihen hakemuksia ajojen yli ja rikkovat lopulta
// tarkkoja määrätarkistuksia (ks. virkailija-hakemuksen-haku-ja-
// muokkauslinkki.spec.ts).
export const createUniqueSSN = (): string => {
  // Päivä rajataan 1-28:aan, jotta se on validi kaikille kuukausille ilman
  // kuukausikohtaista päivälukumäärän tarkistusta. Vuosi rajataan
  // 2000-2020:een (vuosisatamerkki "A" = 2000-luku, ks. ataru.ssn/valid-year?),
  // jotta henkilötunnus pysyy validina (ei tulevaisuudessa) riippumatta
  // siitä, minä vuonna testi ajetaan.
  const paiva = String(1 + Math.floor(Math.random() * 28)).padStart(2, '0')
  const kuukausi = String(1 + Math.floor(Math.random() * 12)).padStart(2, '0')
  const vuosi = String(Math.floor(Math.random() * 21)).padStart(2, '0')
  const vuosisataMerkki = 'A'
  const yksilonumero = String(Math.floor(Math.random() * 1000)).padStart(3, '0')
  const tarkistusmerkit = '0123456789ABCDEFHJKLMNPRSTUVWXY'
  const tarkistusluku = Number.parseInt(
    paiva + kuukausi + vuosi + yksilonumero,
    10
  )
  const tarkistusmerkki = tarkistusmerkit[tarkistusluku % 31]
  return `${paiva}${kuukausi}${vuosi}${vuosisataMerkki}${yksilonumero}${tarkistusmerkki}`
}

// Kytkee selaimen konsolivirheiden/-varoitusten ja käsittelemättömien
// poikkeusten tulostuksen testin ajon ajaksi Node-puolen konsoliin. Pois
// päältä oletuksena, koska normaalilla ajolla tulostus on vain kohinaa —
// päällä kannattaa pitää tilapäisesti silloin, kun testi jää jumiin tavalla,
// jota DOM-tason virheet eivät suoraan selitä.
export const naytaSelaimenVirheetKonsolissa = (
  page: Page,
  enabled = false
): void => {
  if (!enabled) {
    return
  }
  page.on('console', (msg) => {
    if (msg.type() === 'error' || msg.type() === 'warning') {
      console.log(`[browser-${msg.type()}]`, msg.text())
    }
  })
  page.on('pageerror', (err) => console.log('[browser-pageerror]', err))
}

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

export const getHakemuksenMuokkausOsoite = (secret: string) =>
  `/hakemus?modify=${secret}`

export const getLatestApplicationSecretOsoite = () =>
  '/hakemus/latest-application-secret'

const getLomakkeenEsikatseluOsoite = (lomakkeenAvain: string) =>
  `/lomake-editori/api/preview/form/${lomakkeenAvain}?lang=fi`

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
  await page.goto(`/lomake-editori/auth/cas?ticket=${ticket ?? 'DEVELOPER'}`)
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
    } else {
      await loc.fill(value)
    }

    // FIXME: Jos lomake täytetään ilman taukoja, lähettäessä jotkin lomakkeen kentät ovat tyhjiä, vaikka yllä tarkistetaan, että kenttään on mennyt syötetty arvo.
    // eslint-disable-next-line playwright/no-wait-for-timeout
    await page.waitForTimeout(100)
  }
}

export const poistaLomake = async (
  request: APIRequestContext,
  lomakkeenAvain: string
) => {
  await request.delete(getLomakkeenPoistamisenOsoite(), {
    data: {
      formKey: lomakkeenAvain,
    },
  })
}

// Luo tilapäisen lomakkeen saadakseen palvelimen tuottaman oletussisällön
// (hakukohteet-kentän ja henkilötietomoduulin), jota voidaan käyttää
// pohjana muille API:n kautta luotaville testilomakkeille.
export const haeOletuslomakkeenSisalto = async (
  page: Page
): Promise<unknown[]> => {
  const lomake = await lisaaLomake(page)
  const lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
  const lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  const response = await page.request.get(
    `/lomake-editori/api/forms/${lomakkeenId}`
  )
  const form = (await response.json()) as { content: unknown[] }

  await poistaLomake(page.request, lomakkeenAvain)

  return form.content
}

// Luo (tai korvaa) lomakkeen tietyllä, kutsujan valitsemalla avaimella
// suoraan API:n kautta. Tätä tarvitaan, kun testissä pitää saada hakija
// ohjattua hakukohteen/haun kautta lomakkeelle, jonka avain on kiinnitetty
// tarjonnan mock-datassa (ks. mock_tarjonta_service.clj:n
// :ataruLomakeAvain-kentät) — lomaketta ei silloin voi luoda editorin
// "Uusi lomake" -napista, koska sen avain olisi palvelimen generoima.
export const luoLomakeAvaimella = async (
  page: Page,
  lomakkeenAvain: string,
  content: unknown[],
  nimi = 'Testilomake'
): Promise<void> => {
  // Poistetaan mahdollinen edellisestä epäonnistuneesta ajosta jäänyt
  // samanavaiminen lomake, jotta luonti on turvallista ajaa uudestaan.
  await poistaLomake(page.request, lomakkeenAvain)

  const response = await page.request.post(
    getUudenLomakkeenLahettamisenOsoite(),
    {
      data: {
        key: lomakkeenAvain,
        name: { fi: nimi },
        content,
        languages: ['fi'],
        locked: null,
        'locked-by': null,
      },
    }
  )
  if (!response.ok()) {
    throw new Error(
      `Lomakkeen luonti avaimella ${lomakkeenAvain} epäonnistui: ${response.status()} ${await response.text()}`
    )
  }
  const created = (await response.json()) as { key?: string }
  if (created.key !== lomakkeenAvain) {
    throw new Error(
      `Lomake luotiin avaimella "${created.key}" halutun "${lomakkeenAvain}" sijaan`
    )
  }

  // Varmistetaan heti, että lomake on myös hakijan puolelta haettavissa
  // pyydetyllä avaimella, jotta mahdollinen virhe paljastuu tässä eikä vasta
  // myöhemmin oudoksi jäävänä tyhjänä sivuna.
  const haettu = await page.request.get(getLomakkeenHaunOsoite(lomakkeenAvain))
  if (!haettu.ok()) {
    throw new Error(
      `Juuri luotua lomaketta ${lomakkeenAvain} ei saatu haettua hakijan rajapinnasta: ${haettu.status()} ${await haettu.text()}`
    )
  }
}

// Hakija- ja virkailija-puoli ajetaan kaksena erillisenä prosessina, joilla
// kummallakin on oma, prosessikohtainen mock-tarjonta-service-tila (defonce
// test-haut / test-hakukohteet), joten testihaku/-hakukohde pitää
// rekisteröidä MOLEMPIIN, jotta esim. virkailijan hakukohtainen
// hakemuslistaus (joka hakee haun tiedot omasta, virkailijan prosessista)
// löytää sen. Ks. ataru.hakija.hakija-routes/test-routes ja sen peilaus
// ataru.virkailija.virkailija-routes/test-routes:ssa.
export const getTestiHaunOsoite = (hakuOid?: string) =>
  hakuOid
    ? `/hakemus/test/tarjonta/haku/${hakuOid}`
    : '/hakemus/test/tarjonta/haku'

export const getVirkailijaTestiHaunOsoite = (hakuOid?: string) =>
  hakuOid
    ? `/lomake-editori/test/tarjonta/haku/${hakuOid}`
    : '/lomake-editori/test/tarjonta/haku'

export interface TestiHaku {
  oid: string
  ataruLomakeAvain: string
  hakukohdeOids: string[]
  usePriority?: boolean
  kohdejoukkoUri?: string
  kohdejoukonTarkenne?: string
  hakutapaUri?: string
}

// Rekisteröi ajonaikaisesti mock-tarjontapalveluun testikohtaisen haun
// molempiin prosesseihin (ks. yllä oleva kommentti), jotta testin ei
// tarvitse jakaa staattista, kaikille testeille yhteistä
// mock_tarjonta_service.clj:n testidataa (ja sen :ataruLomakeAvain-kenttää)
// muiden, mahdollisesti rinnakkain ajettavien testitiedostojen kanssa.
export const asetaTestiHaku = async (
  page: Page,
  haku: TestiHaku
): Promise<void> => {
  const [hakijaVastaus, virkailijaVastaus] = await Promise.all([
    page.request.post(getTestiHaunOsoite(), { data: haku }),
    page.request.post(getVirkailijaTestiHaunOsoite(), { data: haku }),
  ])
  for (const [nimi, vastaus] of [
    ['hakija', hakijaVastaus],
    ['virkailija', virkailijaVastaus],
  ] as const) {
    if (!vastaus.ok()) {
      throw new Error(
        `Testihaun ${haku.oid} rekisteröinti epäonnistui (${nimi}): ${vastaus.status()} ${await vastaus.text()}`
      )
    }
  }
}

export const poistaTestiHaku = async (
  request: APIRequestContext,
  hakuOid: string
): Promise<void> => {
  await Promise.all([
    request.delete(getTestiHaunOsoite(hakuOid)),
    request.delete(getVirkailijaTestiHaunOsoite(hakuOid)),
  ])
}

// /hakemus/api/haku/:haku-oid on rajoitettu polkuregexillä [0-9\.]+
// (ataru.hakija.hakija-routes), joten asetaTestiHaku-kutsuun annettavan
// haku-oidin täytyy olla vain numeroita ja pisteitä.
export const luoTestiHaunOid = (): string =>
  `1.2.246.562.29.${Date.now()}${Math.floor(Math.random() * 10000)}`

export const getTestiHakukohteenOsoite = (hakukohdeOid?: string) =>
  hakukohdeOid
    ? `/hakemus/test/tarjonta/hakukohde/${hakukohdeOid}`
    : '/hakemus/test/tarjonta/hakukohde'

export const getVirkailijaTestiHakukohteenOsoite = (hakukohdeOid?: string) =>
  hakukohdeOid
    ? `/lomake-editori/test/tarjonta/hakukohde/${hakukohdeOid}`
    : '/lomake-editori/test/tarjonta/hakukohde'

export interface TestiHakukohdeMuutos {
  oid: string
  hakuOid?: string
  // Vain uusille, mock_tarjonta_service.clj:n staattisessa hakukohde-
  // mapissa ennestään tuntemattomille oideille: nimettömän
  // base-hakukohde-pohjan sijaan hakukohteelle saa oman nimen (ks.
  // register-test-hakukohde!, joka asetetaan base-hakukohde:en, jos oidia ei jo
  // löydy). Näin testi voi rekisteröidä TÄYSIN OMAN, kenenkään muun testin
  // kanssa jakamattoman hakukohteen sen sijaan, että se joutuisi
  // uudelleenkäyttämään jotakin ennestään olemassa olevaa, mahdollisesti
  // muidenkin (esim. hakija-haku.spec.ts:n tai hakija-hakukohde.spec.ts:n)
  // samanaikaisesti käyttämää hakukohdetta (esim. "Testihakukohde 1/2/3",
  // oidit 49028196523-525) — kahden tiedoston rekisteröidessä samaan
  // hakukohteeseen ajonaikaisia muutoksia samanaikaisesti eri workereissa on
  // havaittu ajoittain sekoittavan toisen tiedoston hakutulosten järjestystä.
  hakukohteenNimet?: { kieli_fi: string; kieli_sv?: string }
}

// Muuttaa ajonaikaisesti mock-tarjontapalvelun olemassa olevaa hakukohdetta
// molemmissa prosesseissa (ks. asetaTestiHaku:n kommentti), tyypillisesti
// sen :hakuOid-kenttää osoittamaan testin omaan, asetaTestiHaku-kutsulla
// rekisteröityyn hakuun. Näin testi voi navigoida hakukohteen kautta (esim.
// /hakemus/hakukohde/:oid) käyttäen olemassa olevan hakukohteen (esim.
// "Testihakukohde 1") nimeä ja koulutustietoja, mutta ilman että sen
// täytyy jakaa hakukohteen alkuperäisen haun lomakeavainta muiden
// testien kanssa. Uudelle, ennestään tuntemattomalle oidille (ks.
// hakukohteenNimet-kentän kommentti) tämä sen sijaan LUO täysin uuden,
// eristetyn hakukohteen.
export const asetaTestiHakukohde = async (
  page: Page,
  hakukohdeMuutos: TestiHakukohdeMuutos
): Promise<void> => {
  const [hakijaVastaus, virkailijaVastaus] = await Promise.all([
    page.request.post(getTestiHakukohteenOsoite(), { data: hakukohdeMuutos }),
    page.request.post(getVirkailijaTestiHakukohteenOsoite(), {
      data: hakukohdeMuutos,
    }),
  ])
  for (const [nimi, vastaus] of [
    ['hakija', hakijaVastaus],
    ['virkailija', virkailijaVastaus],
  ] as const) {
    if (!vastaus.ok()) {
      throw new Error(
        `Hakukohteen ${hakukohdeMuutos.oid} testimuutos epäonnistui (${nimi}): ${vastaus.status()} ${await vastaus.text()}`
      )
    }
  }
}

export const poistaTestiHakukohde = async (
  request: APIRequestContext,
  hakukohdeOid: string
): Promise<void> => {
  await Promise.all([
    request.delete(getTestiHakukohteenOsoite(hakukohdeOid)),
    request.delete(getVirkailijaTestiHakukohteenOsoite(hakukohdeOid)),
  ])
}

export const expectUusiLomakeValid = async (
  page: Page,
  lomakkeenAvain: string,
  nimi: string
) => {
  await expect(page).toHaveURL(new RegExp(`${lomakkeenAvain}$`))
  await expect(page.getByTestId('form-name-input')).toHaveValue(nimi)

  const esikatseluLinkki = page.getByTestId('application-preview-link-fi')
  await expect(esikatseluLinkki).toHaveText('FI')
  await expect(esikatseluLinkki).toHaveAttribute(
    'href',
    getLomakkeenEsikatseluOsoite(lomakkeenAvain)
  )
}

export const getRajaavatHakukohderyhmatOsoite = (
  hakuOid: string,
  hakukohderyhmaOid: string
) =>
  `/lomake-editori/api/rajaavat-hakukohderyhmat/${hakuOid}/ryhma/${hakukohderyhmaOid}`

export const getPriorisoivatHakukohderyhmatOsoite = (
  hakuOid: string,
  hakukohderyhmaOid: string
) =>
  `/lomake-editori/api/priorisoivat-hakukohderyhmat/${hakuOid}/ryhma/${hakukohderyhmaOid}`

export const asetaRajaavaHakukohderyhma = async (
  page: Page,
  hakuOid: string,
  hakukohderyhmaOid: string,
  raja: number
): Promise<void> => {
  // Poistetaan mahdollinen aiempi asetus ensin, jotta luonti on turvallista
  // ajaa uudestaan (luonti palauttaa 409, jos sama pari on jo olemassa).
  await page.request.delete(
    getRajaavatHakukohderyhmatOsoite(hakuOid, hakukohderyhmaOid)
  )

  const response = await page.request.put(
    getRajaavatHakukohderyhmatOsoite(hakuOid, hakukohderyhmaOid),
    {
      // Palvelin yrittää jäsentää If-Unmodified-Since-otsaketta aina, kun
      // If-None-Match ei ole "*", ja kaatuu 400:aan, jos kumpaakaan ei ole
      // annettu. "*" kertoo, että kyseessä on luonti, ei päivitys.
      headers: { 'If-None-Match': '*' },
      data: {
        'haku-oid': hakuOid,
        'hakukohderyhma-oid': hakukohderyhmaOid,
        raja,
      },
    }
  )
  if (!response.ok()) {
    throw new Error(
      `Rajaavan hakukohderyhmän ${hakukohderyhmaOid} asettaminen epäonnistui: ${response.status()}`
    )
  }
}

export const poistaRajaavaHakukohderyhma = async (
  request: APIRequestContext,
  hakuOid: string,
  hakukohderyhmaOid: string
): Promise<void> => {
  await request.delete(
    getRajaavatHakukohderyhmatOsoite(hakuOid, hakukohderyhmaOid)
  )
}

export const asetaPriorisoivaHakukohderyhma = async (
  page: Page,
  hakuOid: string,
  hakukohderyhmaOid: string,
  prioriteetit: string[][]
): Promise<void> => {
  await page.request.delete(
    getPriorisoivatHakukohderyhmatOsoite(hakuOid, hakukohderyhmaOid)
  )

  const response = await page.request.put(
    getPriorisoivatHakukohderyhmatOsoite(hakuOid, hakukohderyhmaOid),
    {
      // Ks. asetaRajaavaHakukohderyhma: "*" kertoo palvelimelle, että
      // kyseessä on luonti, jotta If-Unmodified-Since-jäsennys ohitetaan.
      headers: { 'If-None-Match': '*' },
      data: {
        'haku-oid': hakuOid,
        'hakukohderyhma-oid': hakukohderyhmaOid,
        prioriteetit,
      },
    }
  )
  if (!response.ok()) {
    throw new Error(
      `Priorisoivan hakukohderyhmän ${hakukohderyhmaOid} asettaminen epäonnistui: ${response.status()}`
    )
  }
}

export const poistaPriorisoivaHakukohderyhma = async (
  request: APIRequestContext,
  hakuOid: string,
  hakukohderyhmaOid: string
): Promise<void> => {
  await request.delete(
    getPriorisoivatHakukohderyhmatOsoite(hakuOid, hakukohderyhmaOid)
  )
}

export const getApplicationSecretById = async (
  page: Page,
  applicationId: number
): Promise<string> => {
  const response = await page.request.get(
    `/hakemus/application-secret-by-id/${applicationId}`
  )
  if (!response.ok()) {
    throw new Error(
      `Failed to fetch application secret for id ${applicationId}`
    )
  }
  return await response.text()
}

// Kysymysryhmällinen testilomake (ks. entinen virkailijaEditorQuestionGroupSpec.js),
// rakennettu suoraan API:n kautta injektoimalla lomakkeen sisältöön, koska
// sisällön klikkaaminen kokoon editorin kautta olisi sekä hidasta että hauras
// jokaisessa sitä tarvitsevassa testissä.
export type FormNode = {
  id?: string
  fieldType?: string
  children?: FormNode[]
  module?: string
  [key: string]: unknown
}

const systemMetadata = {
  'created-by': {
    name: 'system',
    oid: 'system',
    date: '1970-01-01T00:00:00Z',
  },
  'modified-by': {
    name: 'system',
    oid: 'system',
    date: '1970-01-01T00:00:00Z',
  },
}

export const questionGroupFixture: FormNode = {
  fieldClass: 'questionGroup',
  fieldType: 'fieldset',
  id: 'pw-question-group',
  label: { fi: 'Kysymysryhmä: ryhman otsikko' },
  metadata: systemMetadata,
  params: {},
  children: [
    {
      fieldClass: 'formField',
      fieldType: 'dropdown',
      id: 'pw-qg-dropdown',
      label: { fi: 'Kysymysryhmä: pudotusvalikko' },
      metadata: systemMetadata,
      params: {},
      validators: [],
      options: [
        { value: '0', label: { fi: 'Pudotusvalikko: A' } },
        { value: '1', label: { fi: 'Pudotusvalikko: B' } },
      ],
    },
    {
      fieldClass: 'formField',
      fieldType: 'singleChoice',
      id: 'pw-qg-single-choice',
      label: { fi: 'Kysymysryhmä: painikkeet, yksi valittavissa' },
      metadata: systemMetadata,
      params: {},
      validators: [],
      options: [
        { value: '0', label: { fi: 'Painikkeet, yksi valittavissa: A' } },
        { value: '1', label: { fi: 'Painikkeet, yksi valittavissa: B' } },
      ],
    },
    {
      fieldClass: 'formField',
      fieldType: 'multipleChoice',
      id: 'pw-qg-multi-choice',
      label: { fi: 'Kysymysryhmä: lista, monta valittavissa' },
      metadata: systemMetadata,
      params: {},
      validators: [],
      options: [
        { value: '0', label: { fi: 'Lista, monta valittavissa: A' } },
        { value: '1', label: { fi: 'Lista, monta valittavissa: B' } },
      ],
    },
    {
      fieldClass: 'formField',
      fieldType: 'textField',
      id: 'pw-qg-text-single',
      label: { fi: 'Tekstikentta, yksi vastaus' },
      metadata: systemMetadata,
      params: {},
      validators: [],
    },
    {
      fieldClass: 'formField',
      fieldType: 'textField',
      id: 'pw-qg-text-multi',
      label: { fi: 'Tekstikentta, monta vastausta' },
      metadata: systemMetadata,
      params: { repeatable: true },
      validators: [],
    },
    {
      fieldClass: 'formField',
      fieldType: 'textArea',
      id: 'pw-qg-textarea',
      label: { fi: 'Tekstialue' },
      metadata: systemMetadata,
      params: {},
      validators: [],
    },
    {
      fieldClass: 'wrapperElement',
      fieldType: 'adjacentfieldset',
      id: 'pw-qg-adj-single',
      label: { fi: 'Vierekkaiset tekstikentat' },
      metadata: systemMetadata,
      params: {},
      children: [
        {
          fieldClass: 'formField',
          fieldType: 'textField',
          id: 'pw-qg-adj-single-a',
          label: { fi: 'Vierekkaiset tekstikentat, yksi vastaus: A' },
          metadata: systemMetadata,
          params: { adjacent: true },
          validators: [],
        },
        {
          fieldClass: 'formField',
          fieldType: 'textField',
          id: 'pw-qg-adj-single-b',
          label: { fi: 'Vierekkaiset tekstikentat, yksi vastaus: B' },
          metadata: systemMetadata,
          params: { adjacent: true },
          validators: [],
        },
      ],
    },
    {
      fieldClass: 'wrapperElement',
      fieldType: 'adjacentfieldset',
      id: 'pw-qg-adj-multi',
      label: { fi: 'Vierekkaiset tekstikentat' },
      metadata: systemMetadata,
      params: { repeatable: true },
      children: [
        {
          fieldClass: 'formField',
          fieldType: 'textField',
          id: 'pw-qg-adj-multi-a',
          label: { fi: 'Vierekkaiset tekstikentat, monta vastausta: A' },
          metadata: systemMetadata,
          params: { adjacent: true },
          validators: [],
        },
        {
          fieldClass: 'formField',
          fieldType: 'textField',
          id: 'pw-qg-adj-multi-b',
          label: { fi: 'Vierekkaiset tekstikentat, monta vastausta: B' },
          metadata: systemMetadata,
          params: { adjacent: true },
          validators: [],
        },
      ],
    },
  ],
}

export const mainLevelDropdownFixture: FormNode = {
  fieldClass: 'formField',
  fieldType: 'dropdown',
  id: 'pw-main-level-dropdown',
  label: { fi: 'Päätaso: pudotusvalikko' },
  metadata: systemMetadata,
  params: {},
  validators: [],
  options: [
    { value: '0', label: { fi: 'Päätaso: A' }, followups: [] },
    {
      value: '1',
      label: { fi: 'Päätaso: B' },
      followups: [questionGroupFixture],
    },
  ],
}

export const injectQuestionGroupFormData = async (
  page: Page,
  formId: number,
  formName: string
): Promise<void> => {
  const getResponse = await page.request.get(
    `/lomake-editori/api/forms/${formId}`
  )
  if (!getResponse.ok()) {
    throw new Error(`Failed to fetch form ${formId}`)
  }

  const form = (await getResponse.json()) as {
    name?: Record<string, string>
    content: FormNode[]
    [key: string]: unknown
  }

  const hakukohteet = form.content.find(
    (node) => node.id === 'hakukohteet' || node.fieldType === 'hakukohteet'
  )
  const personInfoModule = form.content.find(
    (node) => node.module === 'person-info' || node.id === 'onr'
  )

  if (!hakukohteet || !personInfoModule) {
    throw new Error('Failed to build question group fixture content')
  }

  const formWithoutTimestamp = { ...form }
  delete formWithoutTimestamp['created-time']
  const updatedForm = {
    ...formWithoutTimestamp,
    name: { fi: formName },
    content: [hakukohteet, personInfoModule, mainLevelDropdownFixture],
  }

  const postResponse = await page.request.post('/lomake-editori/api/forms', {
    data: updatedForm,
  })

  if (!postResponse.ok()) {
    throw new Error(`Failed to persist form ${formId}`)
  }
}

// Täyttää henkilötietomoduulin, päätason pudotusvalikon (jonka toinen
// vaihtoehto paljastaa kysymysryhmän), lisää kysymysryhmään toisen rivin ja
// vastaa kysymysryhmän jokaiseen kenttätyyppiin molemmilla riveillä. Käytetään
// sekä hakijan täyttökokemusta että virkailijan hakemuskäsittelyä testaavista
// tiedostoista, koska sisältö on kummassakin sama.
export const fillAndSubmitQuestionGroupApplication = async (
  page: Page,
  formKey: string,
  formName: string
): Promise<void> => {
  const formFields = page.locator(
    '.application__form-content-area .application__form-field'
  )

  const selectNth = async (fieldIndex: number, value: string) => {
    await selectOption(
      page,
      formFields.nth(fieldIndex).locator('select').first(),
      value
    )
  }

  await Promise.all([
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(formKey))
    ),
    page.goto(getHakijanNakymanOsoite(formKey)),
  ])

  await expect(page.getByTestId('application-header-label')).toHaveText(
    formName
  )

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
  await fillField(page, page.getByTestId('phone-input'), '050123')
  await fillField(page, page.getByTestId('address-input'), 'Katutie 12 B')
  await fillField(page, page.getByTestId('postal-code-input'), '40100')
  await expect(page.getByTestId('postal-office-input')).toHaveValue(
    /JYV.*SKYL.*/
  )
  await selectOption(page, page.getByTestId('home-town-input'), '179')

  await selectNth(14, '1')
  await page
    .locator(
      '.application__form-dropdown-followups .application__add-question-group-row a'
    )
    .first()
    .click()

  const questionGroupRows = page.locator(
    '.application__form-dropdown-followups .application__question-group-row'
  )
  await expect(questionGroupRows).toHaveCount(2)

  const row0 = questionGroupRows.nth(0)
  const row1 = questionGroupRows.nth(1)

  // Dropdown
  await selectOption(
    page,
    row0.getByRole('combobox', { name: /Kysymysryhmä: pudotusvalikko/i }),
    '0'
  )
  await selectOption(
    page,
    row1.getByRole('combobox', { name: /Kysymysryhmä: pudotusvalikko/i }),
    '1'
  )

  // Single choice
  await row0
    .getByRole('radio', { name: /Painikkeet, yksi valittavissa: A/i })
    .click()
  await row1
    .getByRole('radio', { name: /Painikkeet, yksi valittavissa: B/i })
    .click()

  // Multiple choice
  await row0
    .locator('label', { hasText: 'Lista, monta valittavissa: A' })
    .click()
  await row0
    .locator('label', { hasText: 'Lista, monta valittavissa: B' })
    .click()
  await row1
    .locator('label', { hasText: 'Lista, monta valittavissa: B' })
    .click()

  // Single-answer text field
  const row0SingleText = row0
    .getByRole('textbox', { name: /Tekstikentt.*yksi vastaus/i })
    .first()
  const row1SingleText = row1
    .getByRole('textbox', { name: /Tekstikentt.*yksi vastaus/i })
    .first()
  await fillField(page, row0SingleText, 'Tekstikenttä, yksi vastaus: A')
  await expect(row0SingleText).toHaveValue('Tekstikenttä, yksi vastaus: A')
  await fillField(page, row1SingleText, 'Tekstikenttä, yksi vastaus: B')
  await expect(row1SingleText).toHaveValue('Tekstikenttä, yksi vastaus: B')

  // Multi-answer text field
  const row0MultiText = row0.getByRole('textbox', {
    name: /Tekstikentt.*monta vastausta/i,
  })
  const row1MultiText = row1.getByRole('textbox', {
    name: /Tekstikentt.*monta vastausta/i,
  })
  await fillField(
    page,
    row0MultiText.nth(0),
    'Tekstikenttä, monta vastausta: A'
  )
  await row0MultiText.nth(0).press('Tab')
  await expect(row0MultiText.nth(1)).toBeEnabled()
  await fillField(
    page,
    row0MultiText.nth(1),
    'Tekstikenttä, monta vastausta: B'
  )
  await fillField(
    page,
    row1MultiText.nth(0),
    'Tekstikenttä, monta vastausta: C'
  )
  await row1MultiText.nth(0).press('Tab')
  await expect(row1MultiText.nth(1)).toBeEnabled()
  await fillField(
    page,
    row1MultiText.nth(1),
    'Tekstikenttä, monta vastausta: D'
  )

  const row0TextArea = row0.getByRole('textbox', { name: /Tekstialue/i })
  const row1TextArea = row1.getByRole('textbox', { name: /Tekstialue/i })

  // Adjacent single-answer fields
  const row0AdjSingle = row0
    .locator('.application__form-adjacent-text-fields-wrapper')
    .nth(0)
    .getByRole('textbox')
  const row1AdjSingle = row1
    .locator('.application__form-adjacent-text-fields-wrapper')
    .nth(0)
    .getByRole('textbox')
  await fillField(
    page,
    row0AdjSingle.nth(0),
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus A'
  )
  await fillField(
    page,
    row0AdjSingle.nth(1),
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus B'
  )
  await fillField(
    page,
    row1AdjSingle.nth(0),
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus C'
  )
  await fillField(
    page,
    row1AdjSingle.nth(1),
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus D'
  )

  // Adjacent multi-answer fields
  const row0AdjMultiField = row0
    .locator('.application__form-field')
    .filter({ hasText: 'Vierekkaiset tekstikentat, monta vastausta: A' })
    .first()
  const row1AdjMultiField = row1
    .locator('.application__form-field')
    .filter({ hasText: 'Vierekkaiset tekstikentat, monta vastausta: A' })
    .first()
  const row0AdjMulti = row0AdjMultiField.getByRole('textbox')
  const row1AdjMulti = row1AdjMultiField.getByRole('textbox')
  await fillField(
    page,
    row0AdjMulti.nth(0),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus A1'
  )
  await fillField(
    page,
    row0AdjMulti.nth(1),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus B1'
  )
  await row0AdjMultiField.getByText('Lisää rivi').click()
  await expect(row0AdjMulti).toHaveCount(4)
  await fillField(
    page,
    row0AdjMulti.nth(2),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus A2'
  )
  await fillField(
    page,
    row0AdjMulti.nth(3),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus B2'
  )

  await fillField(
    page,
    row1AdjMulti.nth(0),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus C1'
  )
  await fillField(
    page,
    row1AdjMulti.nth(1),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus D1'
  )
  await row1AdjMultiField.getByText('Lisää rivi').click()
  await expect(row1AdjMulti).toHaveCount(4)
  await fillField(
    page,
    row1AdjMulti.nth(2),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus C2'
  )
  await fillField(
    page,
    row1AdjMulti.nth(3),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus D2'
  )

  // Täytetään tekstialueet vasta rivien lisäysten jälkeen, koska niiden
  // aiheuttama uudelleenrenderöinti nollasi aiemmin täytetyn tekstialueen
  // arvon näkyvästi ennen lähetystä.
  await fillField(page, row0TextArea, 'Tekstialue: AAAAA')
  await expect(row0TextArea).toHaveValue('Tekstialue: AAAAA')
  await fillField(page, row1TextArea, 'Tekstialue: BBBBB')
  await expect(row1TextArea).toHaveValue('Tekstialue: BBBBB')

  await expect(page.getByTestId('send-application-button')).toBeEnabled()

  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])
}
