import { test, expect, Page } from '@playwright/test'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'
import {
  getHakemuksenLahettamisenOsoite,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  createUniqueSSN,
  poistaLomake,
  taytaHenkilotietomoduuli,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenAvain: string

// Kaksi hakijaa jaetulla henkilötunnuksella, jotta "Näytä oppijan hakemukset"
// -linkki tulee näkyviin, ja kolmas eri henkilötunnuksella, jolle linkkiä ei
// pidä näyttää. Testi hakee hakemuksia SSN:n perusteella yli lomakerajojen
// (ks. testi "näyttää linkin..."), joten sekä ssn:n että sähköpostin pitää
// olla yksilöllisiä joka ajokerralla: "applications-count" (ks.
// resources/sql/application-queries.sql:n yesql-get-latest-application-by-key)
// lasketaan hakemuksista, joilla on SAMA ssn TAI SAMA sähköposti — pelkkä
// ssn:n satunnaistaminen ei siis riitä, jos sähköposti pysyy kiinteänä ja
// kertyy pysyvään kehitystietokantaan ajojen yli.
const AJON_TUNNISTE = Math.random().toString(36).slice(2, 10)
const YHTEINEN_SSN = createUniqueSSN()
const ERILLINEN_SSN = createUniqueSSN()
const HAKIJAT = [
  {
    'first-name': 'Hakija',
    'last-name': 'Yhteinen1',
    ssn: YHTEINEN_SSN,
    email: `yhteinen1-${AJON_TUNNISTE}@example.com`,
    'verify-email': `yhteinen1-${AJON_TUNNISTE}@example.com`,
    phone: '0400000001',
    address: 'Katutie 1',
    'postal-code': '00100',
    'home-town': 'Forssa',
  },
  {
    'first-name': 'Hakija',
    'last-name': 'Yhteinen2',
    ssn: YHTEINEN_SSN,
    email: `yhteinen2-${AJON_TUNNISTE}@example.com`,
    'verify-email': `yhteinen2-${AJON_TUNNISTE}@example.com`,
    phone: '0400000002',
    address: 'Katutie 2',
    'postal-code': '00100',
    'home-town': 'Forssa',
  },
  {
    'first-name': 'Hakija',
    'last-name': 'ErillinenHenkilo',
    ssn: ERILLINEN_SSN,
    email: `erillinen-${AJON_TUNNISTE}@example.com`,
    'verify-email': `erillinen-${AJON_TUNNISTE}@example.com`,
    phone: '0400000003',
    address: 'Katutie 3',
    'postal-code': '00100',
    'home-town': 'Forssa',
  },
]

const applicantRows = () =>
  page.locator(
    '.application-handling__list-row:not(.application-handling__list-header)'
  )

const applicantRow = (lastName: string) =>
  applicantRows().filter({ hasText: lastName })

const mainHeading = () =>
  page.locator('.application-handling__review-area-main-heading')

const clickAndWaitForListReload = async (
  locator: ReturnType<typeof applicantRows>
) => {
  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes('/lomake-editori/api/applications/list')
    ),
    locator.click(),
  ])
}

const naytaSovelletunLomakkeenTulokset = async () => {
  await page.goto(
    `/lomake-editori/applications/${lomakkeenAvain}?ensisijaisesti=false`
  )
  await clickAndWaitForListReload(page.getByTestId('show-results'))
}

const avaaHakemus = async (lastName: string) => {
  await applicantRow(lastName).click()
  await expect(mainHeading()).toContainText(lastName)
}

const applicationKeyOf = async (lastName: string): Promise<string> => {
  const id = await applicantRow(lastName).getAttribute('id')
  if (!id) {
    throw new Error(`Hakemusrivin id puuttuu hakijalta ${lastName}`)
  }
  return id.replace('application-list-row-', '')
}

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)

  const lomake = await lisaaLomake(page)
  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  for (const hakija of HAKIJAT) {
    await Promise.all([
      page.goto(getHakijanNakymanOsoite(lomakkeenAvain)),
      waitForResponse(page, 'GET', (url) =>
        url.includes(getLomakkeenHaunOsoite(lomakkeenAvain))
      ),
    ])
    await taytaHenkilotietomoduuli(page, hakija)
    await Promise.all([
      waitForResponse(page, 'POST', (url) =>
        url.includes(getHakemuksenLahettamisenOsoite())
      ),
      page.getByTestId('send-application-button').click(),
    ])
  }

  await naytaSovelletunLomakkeenTulokset()
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

test('muokkauslinkki osoittaa oikeaan hakemukseen', async () => {
  await avaaHakemus('Yhteinen1')
  const avain = await applicationKeyOf('Yhteinen1')

  // Pääkäyttäjänä (DEVELOPER-tiketti) sivu näyttää KAKSI muokkauslinkkiä
  // samoilla CSS-luokilla: tavallisen ("Muokkaa hakemusta", .../modify) ja
  // rekisterinpitäjän uudelleenkirjoituslinkin ("Muokkaa hakemusta
  // rekisterinpitäjänä", .../rewrite-modify) — erotetaan ne tarkalla
  // linkkitekstillä.
  const muokkauslinkki = page.getByRole('link', {
    name: 'Muokkaa hakemusta',
    exact: true,
  })
  await expect(muokkauslinkki).toHaveAttribute(
    'href',
    `/lomake-editori/api/applications/${avain}/modify`
  )
})

test('näyttää linkin muihin samalla henkilötunnuksella lähetettyihin hakemuksiin', async () => {
  const applicationsLink = page.locator(
    '.application-handling__review-area-main-heading-applications-link'
  )
  await expect(applicationsLink).toContainText('Näytä oppijan hakemukset (2)')

  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes('/lomake-editori/api/applications/list')
    ),
    applicationsLink.click(),
  ])

  await expect(
    page.locator('.application__search-control-search-term-input')
  ).toHaveValue(YHTEINEN_SSN)
  await expect(applicantRows()).toHaveCount(2)
  await expect(applicantRow('Yhteinen1')).toHaveCount(1)
  await expect(applicantRow('Yhteinen2')).toHaveCount(1)
  await expect(applicantRow('ErillinenHenkilo')).toHaveCount(0)
})

test('ei näytä linkkiä hakemukselle, jolla ei ole muita samalla henkilötunnuksella lähetettyjä hakemuksia', async () => {
  await naytaSovelletunLomakkeenTulokset()
  await avaaHakemus('ErillinenHenkilo')

  await expect(
    page.locator(
      '.application-handling__review-area-main-heading-applications-link'
    )
  ).toBeHidden()
})
