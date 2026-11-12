import { test, expect, Page } from '@playwright/test'
import {
  fillField,
  unsafeFoldOption,
  waitForResponse,
} from '../playwright-utils'
import {
  getHakemuksenLahettamisenOsoite,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  taytaHenkilotietomoduuli,
} from '../playwright-ataru-utils'

// Hakukohteettoman muistiinpanon poistolinkki: muokkausoikeudella linkki näkyy
// kaikissa muistiinpanoissa, pelkällä katseluoikeudella (VIEW-ONLY-USER) vain
// käyttäjän omissa. Sama sääntö kuin backendin check-review-note-delete-rightsissa.

test.describe.configure({ mode: 'serial' })

let kasittelijaPage: Page
let lukijaPage: Page
let lomakkeenAvain: string

const HAKIJA = {
  'first-name': 'Muistiinpano Hakija',
  'last-name': 'Poistettava',
  ssn: '020202A0202',
  email: 'poistettava@example.com',
  'verify-email': 'poistettava@example.com',
  phone: '0400000003',
  address: 'Katutie 3',
  'postal-code': '00100',
  'home-town': 'Forssa',
}

const KASITTELIJAN_MUISTIINPANO = 'Käsittelijän muistiinpano'
const LUKIJAN_MUISTIINPANO = 'Lukijan muistiinpano'

const reviewNote = (page: Page, text: string) =>
  page.locator('.application-handling__review-note').filter({ hasText: text })

const removeLink = (page: Page, text: string) =>
  reviewNote(page, text).locator(
    '.application-handling__review-note-remove-link'
  )

const avaaHakemus = async (page: Page) => {
  await page.goto(
    `/lomake-editori/applications/${lomakkeenAvain}?ensisijaisesti=false`
  )
  await page.getByTestId('show-results').click()
  await page
    .locator(
      '.application-handling__list-row:not(.application-handling__list-header)'
    )
    .filter({ hasText: HAKIJA['last-name'] })
    .click()
  await expect(
    page.locator('.application-handling__review-area-main-heading')
  ).toContainText(HAKIJA['last-name'])
}

const lisaaMuistiinpano = async (page: Page, text: string) => {
  await fillField(
    page,
    page.locator('.application-handling__review-note-input'),
    text
  )
  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes('/lomake-editori/api/applications/notes/')
    ),
    page.locator('.application-handling__review-note-submit-button').click(),
  ])
  await expect(reviewNote(page, text)).toBeVisible()
}

test.beforeAll(async ({ browser }) => {
  test.setTimeout(120000)
  kasittelijaPage = await browser.newPage()
  lukijaPage = await browser.newPage()

  await kirjauduVirkailijanNakymaan(kasittelijaPage)

  const lomake = await lisaaLomake(kasittelijaPage)
  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  await Promise.all([
    kasittelijaPage.goto(getHakijanNakymanOsoite(lomakkeenAvain)),
    waitForResponse(kasittelijaPage, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenAvain))
    ),
  ])
  await taytaHenkilotietomoduuli(kasittelijaPage, HAKIJA)
  await Promise.all([
    waitForResponse(kasittelijaPage, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    kasittelijaPage.getByTestId('send-application-button').click(),
  ])

  await avaaHakemus(kasittelijaPage)
  await lisaaMuistiinpano(kasittelijaPage, KASITTELIJAN_MUISTIINPANO)

  await kirjauduVirkailijanNakymaan(lukijaPage, 'VIEW-ONLY-USER')
  await avaaHakemus(lukijaPage)
  await lisaaMuistiinpano(lukijaPage, LUKIJAN_MUISTIINPANO)
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await kasittelijaPage.close()
  await lukijaPage.close()
})

test('katseluoikeudella poistolinkki näkyy juuri lisätyssä omassa muistiinpanossa', async () => {
  await expect(removeLink(lukijaPage, LUKIJAN_MUISTIINPANO)).toBeVisible()
  await expect(removeLink(lukijaPage, KASITTELIJAN_MUISTIINPANO)).toHaveCount(0)
})

test('katseluoikeudella poistolinkki näkyy vain omassa muistiinpanossa hakemuksen uudelleen avaamisen jälkeen', async () => {
  await avaaHakemus(lukijaPage)

  await expect(reviewNote(lukijaPage, KASITTELIJAN_MUISTIINPANO)).toBeVisible()
  await expect(removeLink(lukijaPage, LUKIJAN_MUISTIINPANO)).toBeVisible()
  await expect(removeLink(lukijaPage, KASITTELIJAN_MUISTIINPANO)).toHaveCount(0)
})

test('muokkausoikeudella poistolinkki näkyy kaikissa muistiinpanoissa', async () => {
  await avaaHakemus(kasittelijaPage)

  await expect(
    removeLink(kasittelijaPage, KASITTELIJAN_MUISTIINPANO)
  ).toBeVisible()
  await expect(removeLink(kasittelijaPage, LUKIJAN_MUISTIINPANO)).toBeVisible()
})

test('katseluoikeudella oman muistiinpanon voi poistaa', async () => {
  await removeLink(lukijaPage, LUKIJAN_MUISTIINPANO).click()
  await Promise.all([
    waitForResponse(lukijaPage, 'DELETE', (url) =>
      url.includes('/lomake-editori/api/applications/notes/')
    ),
    removeLink(lukijaPage, LUKIJAN_MUISTIINPANO)
      .filter({ hasText: 'Vahvista poisto' })
      .click(),
  ])

  await expect(reviewNote(lukijaPage, LUKIJAN_MUISTIINPANO)).toHaveCount(0)
  await expect(reviewNote(lukijaPage, KASITTELIJAN_MUISTIINPANO)).toBeVisible()
})
