import { randomUUID } from 'crypto'
import { test, expect, Page, Locator } from '@playwright/test'
import { fillField, waitForResponse } from '../playwright-utils'
import {
  asetaTestiHaku,
  asetaTestiHakukohde,
  getHakemuksenLahettamisenOsoite,
  haeOletuslomakkeenSisalto,
  kirjauduVirkailijanNakymaan,
  luoLomakeAvaimella,
  luoTestiHaunOid,
  poistaLomake,
  poistaTestiHaku,
  poistaTestiHakukohde,
  taytaHenkilotietomoduuli,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

let page: Page

// Samat mock_tarjonta_service.clj:n valmiit testihakukohteet, joita
// hakija-hakukohde.spec.ts ja hakija-haku.spec.ts jo käyttävät (nimillä
// "Testihakukohde 1"/"2"). Toisin kuin hakija-hakukohde.spec.ts:ssä (jossa
// vain suoraan navigoitava hakukohde pitää osoittaa uudelleen), TÄSSÄ
// testissä molempien oma :hakuOid-kenttä pitää osoittaa uudelleen tähän
// testihakuun (asetaTestiHakukohde): hakukohde-rajaus-suodattimen
// valitseminen navigoi virkailijan puolella suoraan osoitteeseen
// /lomake-editori/applications/hakukohde/:oid, ja tämä reitti hakee
// hakukohteen (siis myös Testihakukohteen 2) oman :hakuOid-kentän
// ratkaistakseen, minkä haun hakemuksia listataan — vanhalla, oletusarvon
// mukaisella :hakuOid:lla tämän testin omat hakemukset eivät löytyisi.
const HAKUKOHDE_1 = '1.2.246.562.20.49028196523'
const HAKUKOHDE_2 = '1.2.246.562.20.49028196524'

const hakuOid = luoTestiHaunOid()
const lomakkeenAvain = randomUUID()

type Hakija = {
  'first-name': string
  'last-name': string
  ssn: string
  email: string
  'verify-email': string
  phone: string
  address: string
  'postal-code': string
  'home-town': string
}

// Aakkosjärjestyksessä (oletuslajittelu), jotta hakemuslistan ja
// seuraava/edellinen-navigoinnin järjestys on yksiselitteinen. Molemmat
// hakevat sekä Testihakukohteeseen 1 että 2, jotta hakukohde-rajaus ei
// pudota kumpaakaan pois listalta — testin tarkoitus on hakemuksen
// tiedoissa oletuksena valittu hakukohde, ei listan suodattuminen.
const HAKIJAT: Hakija[] = [
  {
    'first-name': 'Hakija',
    'last-name': 'Ensimmäinen',
    ssn: '020202A0202',
    email: 'ensimmainen@example.com',
    'verify-email': 'ensimmainen@example.com',
    phone: '0400000001',
    address: 'Katutie 1',
    'postal-code': '00100',
    'home-town': 'Forssa',
  },
  {
    'first-name': 'Hakija',
    'last-name': 'Toinen',
    ssn: '160600A999C',
    email: 'toinen@example.com',
    'verify-email': 'toinen@example.com',
    phone: '0400000002',
    address: 'Katutie 2',
    'postal-code': '00100',
    'home-town': 'Forssa',
  },
]

const submitHakemus = async (hakija: Hakija) => {
  await Promise.all([
    waitForResponse(page, 'GET', (url) =>
      url.includes(`/hakemus/api/hakukohde/${HAKUKOHDE_1}`)
    ),
    page.goto(`/hakemus/hakukohde/${HAKUKOHDE_1}`),
  ])
  await expect(
    page.locator('.application__selected-hakukohde-row')
  ).toHaveCount(1)

  await page.locator('.application__hakukohde-selection-open-search').click()
  await fillField(
    page,
    page.locator('.application__form-text-input-in-box'),
    'Testihakukohde 2'
  )
  await page
    .locator('.application__search-hit-hakukohde-row--select-button')
    .first()
    .click()
  await expect(
    page.locator('.application__selected-hakukohde-row')
  ).toHaveCount(2)

  await taytaHenkilotietomoduuli(page, hakija)
  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])
}

const applicantRows = () =>
  page.locator(
    '.application-handling__list-row:not(.application-handling__list-header)'
  )

const applicantRow = (lastName: string) =>
  applicantRows().filter({ hasText: lastName })

const mainHeading = () =>
  page.locator('.application-handling__review-area-main-heading')

const clickAndWaitForListReload = async (locator: Locator) => {
  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes('/lomake-editori/api/applications/list')
    ),
    locator.click(),
  ])
}

const naytaHaunHakemukset = async () => {
  await page.goto(
    `/lomake-editori/applications/haku/${hakuOid}?ensisijaisesti=false`
  )
  await page.getByTestId('show-results').click()
}

const rajausToggleButton = () =>
  page.locator('.application-handling__hakukohde-rajaus-toggle-button')

const rajausListItem = (label: string) =>
  page
    .locator('.hakukohde-and-hakukohderyhma-category-list-item')
    .filter({ hasText: label })

const navigationLinks = () =>
  page.locator('.application-handling__navigation-link')

const clickSeuraava = () => navigationLinks().nth(1).click()
const clickEdellinen = () => navigationLinks().nth(0).click()

const reviewHakukohdeContainer = () =>
  page.locator('.application-handling__review-state-container--columnar')

const reviewHakukohdeToggle = () =>
  reviewHakukohdeContainer()
    .locator('.application-handling__review-state-row-hakukohde')
    .first()

const reviewHakukohdeRow = (oid: string) =>
  reviewHakukohdeContainer().locator(`[data-hakukohde-oid="${oid}"]`)

const hakukohdeSelectedIcon = (oid: string) =>
  reviewHakukohdeRow(oid).locator('i.zmdi-check')

// Suljettuna näkyy vain kulloinkin valittu hakukohderivi, joten molempien
// tarkistaminen (myös ei-valitun) vaatii listan avaamisen ensin.
const ensureHakukohdeListaAuki = async () => {
  if (!(await reviewHakukohdeRow(HAKUKOHDE_1).isVisible())) {
    await reviewHakukohdeToggle().click()
    await expect(reviewHakukohdeRow(HAKUKOHDE_1)).toBeVisible()
  }
}

const expectOletushakukohdeValittuna = async () => {
  await ensureHakukohdeListaAuki()
  await expect(hakukohdeSelectedIcon(HAKUKOHDE_2)).toBeVisible()
  await expect(hakukohdeSelectedIcon(HAKUKOHDE_1)).toHaveCount(0)
}

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
    hakukohdeOids: [HAKUKOHDE_1, HAKUKOHDE_2],
  })
  await asetaTestiHakukohde(page, { oid: HAKUKOHDE_1, hakuOid })
  await asetaTestiHakukohde(page, { oid: HAKUKOHDE_2, hakuOid })

  for (const hakija of HAKIJAT) {
    await submitHakemus(hakija)
  }

  await naytaHaunHakemukset()
})

test.afterAll(async ({ request }) => {
  await poistaTestiHakukohde(request, HAKUKOHDE_1)
  await poistaTestiHakukohde(request, HAKUKOHDE_2)
  await poistaTestiHaku(request, hakuOid)
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

test('suodattaa hakukohteen mukaan ja valitsee sen oletuksena hakemuksen tiedoissa', async () => {
  await expect(applicantRows()).toHaveCount(2)

  await rajausToggleButton().click()
  const testihakukohde2 = rajausListItem('Testihakukohde 2')
  await expect(testihakukohde2).toBeVisible()
  await clickAndWaitForListReload(testihakukohde2)

  await applicantRow('Ensimmäinen').click()
  await expect(mainHeading()).toContainText('Ensimmäinen')
  await expectOletushakukohdeValittuna()
})

test('siirtyy seuraavaan hakemukseen ja valitsee saman oletushakukohteen', async () => {
  await clickSeuraava()
  await expect(mainHeading()).toContainText('Toinen')
  await expectOletushakukohdeValittuna()
})

test('muokkaus toisessa hakemuksessa ei säily siirryttäessä edelliseen', async () => {
  await ensureHakukohdeListaAuki()
  await reviewHakukohdeRow(HAKUKOHDE_1).click()
  await expect(hakukohdeSelectedIcon(HAKUKOHDE_1)).toBeVisible()

  await clickEdellinen()
  await expect(mainHeading()).toContainText('Ensimmäinen')
  await expectOletushakukohdeValittuna()
})
