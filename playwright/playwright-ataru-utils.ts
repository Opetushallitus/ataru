import { Page, Locator, expect, APIRequestContext } from '@playwright/test'
import {
  getJsonResponseKey,
  unsafeFoldOption,
  waitForResponse,
} from './playwright-utils'
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

export const getTestiHaunOsoite = (hakuOid?: string) =>
  hakuOid
    ? `/hakemus/test/tarjonta/haku/${hakuOid}`
    : '/hakemus/test/tarjonta/haku'

export interface TestiHaku {
  oid: string
  ataruLomakeAvain: string
  hakukohdeOids: string[]
  usePriority?: boolean
  kohdejoukkoUri?: string
  kohdejoukonTarkenne?: string
}

// Rekisteröi ajonaikaisesti mock-tarjontapalveluun testikohtaisen haun (ks.
// ataru.hakija.hakija-routes/test-routes ja
// ataru.tarjonta-service.mock-tarjonta-service/register-test-haku!), jotta
// testin ei tarvitse jakaa staattista, kaikille testeille yhteistä
// mock_tarjonta_service.clj:n testidataa (ja sen :ataruLomakeAvain-kenttää)
// muiden, mahdollisesti rinnakkain ajettavien testitiedostojen kanssa.
export const asetaTestiHaku = async (
  page: Page,
  haku: TestiHaku
): Promise<void> => {
  const response = await page.request.post(getTestiHaunOsoite(), {
    data: haku,
  })
  if (!response.ok()) {
    throw new Error(
      `Testihaun ${haku.oid} rekisteröinti epäonnistui: ${response.status()} ${await response.text()}`
    )
  }
}

export const poistaTestiHaku = async (
  request: APIRequestContext,
  hakuOid: string
): Promise<void> => {
  await request.delete(getTestiHaunOsoite(hakuOid))
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
