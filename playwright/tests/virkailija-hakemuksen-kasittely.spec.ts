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

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenAvain: string

// Nimet lainattu entisestä virkailijaApplicationHandlingSpec.js:stä, mutta
// näitä hakemuksia ei luoda kiinteän tarjonta-/tietokantafixtuurin kautta
// (joka on olemassa vain speclj-selaintestien omassa, ajonaikaisessa
// tietokannassa) vaan lähettämällä oikeat hakemukset tämän testin omaan,
// vasta luotuun lomakkeeseen — samaan tapaan kuin muutkin tässä repossa jo
// migratoidut Playwright-testit tekevät.
const HAKIJAT = [
  {
    'first-name': 'Ensimmäinen Hakija',
    'last-name': 'Vatanen',
    ssn: '020202A0202',
    email: 'vatanen@example.com',
    'verify-email': 'vatanen@example.com',
    phone: '0400000001',
    address: 'Katutie 1',
    'postal-code': '00100',
    'home-town': 'Forssa',
  },
  {
    'first-name': 'Toinen Hakija',
    'last-name': 'Kuikeloinen',
    ssn: '160600A999C',
    email: 'kuikeloinen@example.com',
    'verify-email': 'kuikeloinen@example.com',
    phone: '0400000002',
    address: 'Katutie 2',
    'postal-code': '00100',
    'home-town': 'Forssa',
  },
]

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
  await page.getByTestId('show-results').click()
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

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

const selectionStateContainer = () =>
  page.locator('.application-handling__review-state-container-selection-state')

const selectedSelectionState = () =>
  selectionStateContainer().locator(
    '.application-handling__review-state-row--selected'
  )

const setSelectionState = async (label: string) => {
  await selectedSelectionState().click()
  await Promise.all([
    waitForResponse(page, 'PUT', (url) =>
      url.includes('/lomake-editori/api/applications/review')
    ),
    selectionStateContainer()
      .locator('.application-handling__review-state-row', { hasText: label })
      .click(),
  ])
}

const listRowSelectionState = (lastName: string) =>
  applicantRow(lastName).getByTestId('list-hakukohde-selection-state')

const eventCaptions = () =>
  page.locator('.application-handling__event-row-header > span')

const reviewNoteInput = () =>
  page.locator('.application-handling__review-note-input')
const reviewNoteSubmitButton = () =>
  page.locator('.application-handling__review-note-submit-button')
const reviewNoteSummary = () =>
  page.locator('.application-handling__review-note-summary-text')

const scoreInput = () => page.locator('.application-handling__score-input')

const fillScoreAndWaitForSave = async (value: string) => {
  await fillField(page, scoreInput(), value)
  await waitForResponse(page, 'PUT', (url) =>
    url.includes('/lomake-editori/api/applications/review')
  )
}

test('avaa ensimmäisen hakemuksen', async () => {
  await expect(applicantRows()).toHaveCount(2)

  await applicantRow('Vatanen').click()

  await expect(mainHeading()).toContainText('Vatanen')
})

test('vaihtaa käsittelyvaiheen ja tallentaa siitä tapahtuman', async () => {
  await expect(selectedProcessingState()).toHaveText('Käsittelemättä')
  // Hakemuksella on jo lähetyshetkellä vähintään yksi tapahtuma (esim.
  // vastaanottoilmoitus), joten verrataan tapahtumamäärän kasvua eikä
  // oleteta lähtötilanteeksi nollaa.
  const tapahtumiaEnnenMuutosta = await eventCaptions().count()

  await setProcessingState('Käsittelyssä')

  await expect(selectedProcessingState()).toHaveText('Käsittelyssä')
  await expect(eventCaptions()).toHaveCount(tapahtumiaEnnenMuutosta + 1)
  await expect(eventCaptions().first()).toContainText(
    'Käsittelyvaihe: Käsittelyssä'
  )
})

test('tallentaa muistiinpanon hakemukselle', async () => {
  await expect(reviewNoteSubmitButton()).toBeDisabled()

  await fillField(page, reviewNoteInput(), 'Reipas kaveri')
  await expect(reviewNoteSubmitButton()).toBeEnabled()

  await reviewNoteSubmitButton().click()

  await expect(reviewNoteSummary()).toBeVisible()
})

test('pisteet säilyvät hakemuksesta toiseen siirryttäessä', async () => {
  await fillScoreAndWaitForSave('42')

  await applicantRow('Kuikeloinen').click()
  await expect(mainHeading()).toContainText('Kuikeloinen')
  await expect(scoreInput()).toHaveValue('')
  await expect(reviewNoteInput()).toHaveValue('')

  await fillScoreAndWaitForSave('50')

  await applicantRow('Vatanen').click()
  await expect(mainHeading()).toContainText('Vatanen')
  await expect(scoreInput()).toHaveValue('42')
  // Muistiinpanokenttä on aina tyhjä UUDEN muistiinpanon kirjoittamista
  // varten — tallennettu muistiinpano näkyy pysyvästi vain
  // yhteenvetotekstissä, ei tekstialueen arvona.
  await expect(reviewNoteSummary()).toBeVisible()

  await applicantRow('Kuikeloinen').click()
  await expect(mainHeading()).toContainText('Kuikeloinen')
  await expect(scoreInput()).toHaveValue('50')
})

test('tyhjentää pisteet', async () => {
  await fillScoreAndWaitForSave('')

  // Odotetaan otsikon vaihtumista jokaisen klikkauksen jälkeen, jotta
  // seuraava klikkaus ei ehdi lähteä ennen kuin edellinen hakemuksen
  // vaihto on todella tapahtunut (ks. muiden testien vastaava kuvio).
  await applicantRow('Vatanen').click()
  await expect(mainHeading()).toContainText('Vatanen')

  await applicantRow('Kuikeloinen').click()
  await expect(mainHeading()).toContainText('Kuikeloinen')

  await expect(scoreInput()).toHaveValue('')
})

test('vaihtaa valintatilan ja se näkyy sekä hakemuksella että listarivillä', async () => {
  await expect(selectedSelectionState()).toHaveText('Kesken')

  await setSelectionState('Hyväksytty')
  await expect(selectedSelectionState()).toHaveText('Hyväksytty')

  // Valintatilan pitää säilyä myös hakemuksesta toiseen siirryttäessä, ja
  // näkyä myös hakemuslistan omalla rivillä eikä vain avatussa
  // hakemuksessa.
  await applicantRow('Vatanen').click()
  await expect(mainHeading()).toContainText('Vatanen')

  await applicantRow('Kuikeloinen').click()
  await expect(mainHeading()).toContainText('Kuikeloinen')
  await expect(selectedSelectionState()).toHaveText('Hyväksytty')

  await expect(listRowSelectionState('Kuikeloinen')).toHaveText('Hyväksytty')
})

// Vanhan mocha-testin "Tee yksilöinti henkilöpalvelussa." -ilmoitusta ei voi
// testata oikeaa lähetysvuota käyttäen: taustatyöjonon ajaja on tässä
// (fake-dependencies-)ympäristössä täydellinen no-op
// (ataru.background-job.job/FakeJobRunner), joten oikeasti lähetetyn
// hakemuksen person-oid ei koskaan asetu tietokantaan — ilmoitus ei siis
// koskaan voi ilmestyä riippumatta odotusajasta. Ainoa tässä repossa
// aiemmin toiminut tapa testata tätä (cypress/integration/
// hakemuksenTarkasteluSpec.ts) mockaa koko GET-hakemusvastauksen sisältäen
// valmiin person-oid:n ja yksiloity:false-lipun, sen sijaan että hakemus
// lähetettäisiin oikeasti.
