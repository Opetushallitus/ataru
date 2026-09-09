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

// Kolme hakijaa, joille asetetaan setupissa kolme eri käsittelytilaa, jotta
// suodatuksen vaikutusta listaan voi todentaa yksiselitteisesti sukunimen
// perusteella (ensimmäinen jää oletustilaan "Käsittelemättä").
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
  {
    'first-name': 'Hakija',
    'last-name': 'Kolmas',
    ssn: '020202A0202',
    email: 'kolmas@example.com',
    'verify-email': 'kolmas@example.com',
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

const processingStateContainer = () =>
  page.locator('.application-handling__review-state-container-processing-state')

const selectedProcessingState = () =>
  processingStateContainer().locator(
    '.application-handling__review-state-row--selected'
  )

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

const setProcessingState = async (label: string) => {
  await selectedProcessingState().click()
  await Promise.all([
    waitForResponse(page, 'PUT', (url) =>
      url.includes('/lomake-editori/api/applications/review')
    ),
    processingStateContainer()
      .locator('.application-handling__review-state-row', { hasText: label })
      .click(),
  ])
}

// Sarakkeen otsikon data-test-id ("processing-state-filter") on kuollutta
// koodia (when-lauseke, joka hukkaa attribuutin). Ulompi
// .application-handling__list-row--state -elementti (skoopattuna
// otsikkoriviin, koska sama luokka esiintyy myös jokaisen hakemusrivin
// omassa tilasolussa) on koko sarakkeen levyinen, mutta klikattava
// avauslinkki on vain otsikkotekstin kokoinen sen sisällä — klikataan siksi
// suoraan otsikkotekstiä.
const processingStateColumn = () =>
  page
    .locator(
      '.application-handling__list-header .application-handling__list-row--state'
    )
    .getByText('Käsittelyvaihe', { exact: true })

const filterPanel = () =>
  page.locator('.application-handling__filter-state-selection')

const filterStateRow = (label: string) =>
  filterPanel().locator('.application-handling__filter-state-selection-row', {
    hasText: label,
  })

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

  await page.goto(
    `/lomake-editori/applications/${lomakkeenAvain}?ensisijaisesti=false`
  )
  await clickAndWaitForListReload(page.getByTestId('show-results'))

  await applicantRow('Toinen').click()
  await expect(mainHeading()).toContainText('Toinen')
  await setProcessingState('Käsittelyssä')

  await applicantRow('Kolmas').click()
  await expect(mainHeading()).toContainText('Kolmas')
  await setProcessingState('Käsitelty')

  // Listan tilakohtaiset laskurit lasketaan vain silloin, kun hakemuslista
  // haetaan uudelleen (esim. lajittelun tai suodattimen vaihtuessa) — pelkkä
  // yksittäisen hakemuksen tilan tallennus ei päivitä niitä, joten haetaan
  // lista tässä vielä kertaalleen uudelleen samalla tavalla kuin alussa
  // ennen suodattimen testaamista.
  await page.goto(
    `/lomake-editori/applications/${lomakkeenAvain}?ensisijaisesti=false`
  )
  await clickAndWaitForListReload(page.getByTestId('show-results'))
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

test('avaa käsittelytilasuodattimen ja näyttää oikeat määrät kullekin tilalle', async () => {
  await expect(applicantRows()).toHaveCount(3)

  await processingStateColumn().click()

  await expect(filterPanel()).toBeVisible()
  await expect(filterStateRow('Käsittelemättä')).toContainText(
    'Käsittelemättä (1)'
  )
  await expect(filterStateRow('Käsittelyssä')).toContainText('Käsittelyssä (1)')
  await expect(filterStateRow('Käsitelty')).toContainText('Käsitelty (1)')
})

test('tilan poistaminen suodattimesta piilottaa vain sitä tilaa olevan hakemuksen', async () => {
  await clickAndWaitForListReload(filterStateRow('Käsittelemättä'))

  await expect(applicantRows()).toHaveCount(2)
  await expect(applicantRow('Ensimmäinen')).toHaveCount(0)
  await expect(applicantRow('Toinen')).toHaveCount(1)
  await expect(applicantRow('Kolmas')).toHaveCount(1)
})

test('tilan lisääminen takaisin suodattimeen tuo hakemuksen takaisin listalle', async () => {
  await clickAndWaitForListReload(filterStateRow('Käsittelemättä'))

  await expect(applicantRows()).toHaveCount(3)
  await expect(applicantRow('Ensimmäinen')).toHaveCount(1)
})
