import { test, expect, Page } from '@playwright/test'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'
import {
  getHakemuksenLahettamisenOsoite,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  taytaHenkilotietomoduuli,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenAvain: string
let ensimmaisenAvain: string

const HAKIJAT = [
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

// review_states.cljc:n normaalin (ei tutu/astu) lomakkeen 8 käsittelytilaa,
// samassa järjestyksessä.
const KAIKKI_KASITTELYTILAT = [
  'unprocessed',
  'processing',
  'invited-to-interview',
  'invited-to-exam',
  'evaluating',
  'valintaesitys',
  'processed',
  'information-request',
]

// URL-parametri processing-state-filter listaa POISSULJETTAVAT tilat, ei
// mukaan otettavia (ks. cljs-util/get-unselected-review-states) — jotta vain
// yksi tila jää mukaan, pitää loput seitsemän listata parametrissa.
const poissuljetutTilatPaitsi = (sisallytettavaTila: string) =>
  KAIKKI_KASITTELYTILAT.filter((tila) => tila !== sisallytettavaTila).join(',')

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

const processingStateColumn = () =>
  page
    .locator(
      '.application-handling__list-header .application-handling__list-row--state'
    )
    .getByText('Käsittelyvaihe', { exact: true })

const filterPanel = () =>
  page.locator('.application-handling__filter-state-selection')

const includedFilterRows = () =>
  filterPanel().locator('.application-handling__filter-state-selected-row')

test.beforeAll(async ({ browser }) => {
  test.setTimeout(120000)
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

  // Selvitetään ensimmäisen hakijan hakemusavain listarivin id:stä, jotta sitä
  // voidaan käyttää application-key-url-parametrina.
  await page.goto(
    `/lomake-editori/applications/${lomakkeenAvain}?ensisijaisesti=false`
  )
  await clickAndWaitForListReload(page.getByTestId('show-results'))

  const id = await applicantRow('Ensimmäinen').getAttribute('id')
  if (!id) {
    throw new Error('Hakemusrivin id puuttuu hakijalta Ensimmäinen')
  }
  ensimmaisenAvain = id.replace('application-list-row-', '')
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

test('esivalitsee hakemuksen ja esitäyttää käsittelytilasuodattimen url-parametreista', async () => {
  await page.goto(
    `/lomake-editori/applications/${lomakkeenAvain}` +
      `?ensisijaisesti=false` +
      `&application-key=${ensimmaisenAvain}` +
      `&processing-state-filter=${poissuljetutTilatPaitsi('unprocessed')}`
  )
  await clickAndWaitForListReload(page.getByTestId('show-results'))

  // Sovelluksessa on kaksi hakemusta, joten esivalinta ei tapahtuisi
  // automaattisesti ilman application-key-parametria.
  await expect(applicantRows()).toHaveCount(2)
  await expect(mainHeading()).toContainText('Ensimmäinen')

  await processingStateColumn().click()
  await expect(filterPanel()).toBeVisible()
  await expect(includedFilterRows()).toHaveCount(1)
  await expect(includedFilterRows()).toContainText('Käsittelemättä')
})
