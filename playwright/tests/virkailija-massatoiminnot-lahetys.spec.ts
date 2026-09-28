import { test, expect, Page, Locator, Route } from '@playwright/test'
import { fillField, fixtureFromFile } from '../playwright-utils'
import { kirjauduVirkailijanNakymaan } from '../playwright-ataru-utils'

// Tämä tiedosto täydentää virkailijan-massatoiminnot.spec.ts:n kattamaa aluetta
// oikeilla, päästä-päähän-kulkevilla lähetysvuoilla (massamuutoksen
// varsinainen tallennus ja hakemuslistan päivittyminen; massaviestin
// varsinainen lähetys ja tilan eteneminen). Selection-state-suodatin
// (vanhan mocha-testin "recipient filtering" -osio) on nykyisessä koodissa
// kuollutta toiminnallisuutta — ks. virkailija_application_list_view.cljs
// ja handlers.cljs: valittu selection-state-suodatin ei koskaan päädy
// backendille lähetettävään states-and-filters-kutsuun eikä minkään
// client-puolen predikaatin kautta hakemuslistaan — joten sitä ei testata
// tässä.
test.describe.configure({ mode: 'serial' })

let page: Page
let massUpdateApplied = false

const HAKU_OID = '1.2.246.562.29.00000000000000018308'
const HAKUKOHDE_OID = '1.2.246.562.20.00000000000000018314'

type Hakija = {
  key: string
  lastName: string
  firstName: string
  ssn: string
  processingState: string
}

// Hakemuksen (ja henkilön) avain on ajonaikaisesti generoitu, jotta se ei
// koskaan osu paikallisen kehitysympäristön Postgresiin aiemmista
// ajoista kertyneeseen oikeaan hakemusdataan — GET /api/applications/:key
// on tässä tiedostossa mockattu (ks. applicationDetailRoute), mutta
// kiinteä, kaikille testiajoille sama avain olisi silti hauras: mikä
// tahansa mockaamaton reitti tälle samalle avaimelle (esim. jos
// mockaus unohtuu jatkossa jostain uudesta päätepisteestä) palauttaisi
// tällöin suoraan vääriä, oikeita henkilötietoja HAKIJAT-listan sijaan.
const luoHakijaAvain = () =>
  `1.2.246.562.11.${Date.now()}${Math.floor(Math.random() * 10000)}`

// Aakkosjärjestyksessä (oletuslajittelu), jotta massamuutoksen jälkeinen
// tila voidaan todentaa hakemuslistan riviluetteloa vastaan indeksin
// perusteella: kaksi ensimmäistä ovat aluksi "Käsittelemättä", kolmas
// pysyy koskemattomana "Käsittelyssä"-tilassa massamuutoksen ajan.
const HAKIJAT: Array<Hakija> = [
  {
    key: luoHakijaAvain(),
    lastName: 'Aallikko',
    firstName: 'Aino',
    ssn: '020202A0202',
    processingState: 'unprocessed',
  },
  {
    key: luoHakijaAvain(),
    lastName: 'Kokko',
    firstName: 'Kalle',
    ssn: '160600A999C',
    processingState: 'unprocessed',
  },
  {
    key: luoHakijaAvain(),
    lastName: 'Virtanen',
    firstName: 'Ville',
    ssn: '020202A0202',
    processingState: 'processing',
  },
]

const hakemusJson = (hakija: Hakija, id: number) => ({
  haku: HAKU_OID,
  person: {
    oid: `1.2.246.562.24.${id}`,
    'preferred-name': hakija.firstName,
    'last-name': hakija.lastName,
    yksiloity: false,
    ssn: hakija.ssn,
    dob: '01.01.2000',
  },
  key: hakija.key,
  'eligibility-set-automatically': [],
  state: 'active',
  submitted: '2024-01-01T00:00:00.000Z',
  lang: 'fi',
  'new-application-modifications': 0,
  id,
  score: null,
  hakukohde: [HAKUKOHDE_OID],
  form: 1,
  'application-hakukohde-reviews': [
    {
      requirement: 'processing-state',
      state:
        massUpdateApplied && hakija.processingState === 'unprocessed'
          ? 'processed'
          : hakija.processingState,
      hakukohde: HAKUKOHDE_OID,
    },
    {
      requirement: 'language-requirement',
      state: 'unreviewed',
      hakukohde: HAKUKOHDE_OID,
    },
    {
      requirement: 'degree-requirement',
      state: 'unreviewed',
      hakukohde: HAKUKOHDE_OID,
    },
    {
      requirement: 'eligibility-state',
      state: 'unreviewed',
      hakukohde: HAKUKOHDE_OID,
    },
    {
      requirement: 'payment-obligation',
      state: 'unreviewed',
      hakukohde: HAKUKOHDE_OID,
    },
    {
      requirement: 'selection-state',
      state: 'incomplete',
      hakukohde: HAKUKOHDE_OID,
    },
  ],
  'created-time': '2024-01-01T00:00:00.000Z',
  'base-education': [],
  'application-attachment-reviews': [],
})

const applicationsListRoute = (route: Route) => {
  if (route.request().method() !== 'POST') {
    return route.fallback()
  }
  return route.fulfill({
    json: {
      sort: { 'order-by': 'applicant-name', order: 'asc' },
      applications: HAKIJAT.map((hakija, index) =>
        hakemusJson(hakija, index + 1)
      ),
    },
  })
}

// Hakemuslistalta yksittäisen hakemuksen avaaminen hakee sen kokotiedot
// erikseen (GET /api/applications/:key) — tuo päätepiste ei ole muiden
// tämän tiedoston mockien tapaan hakemuskohtainen listaus vaan aidosti
// hakemusavaimella osoitettu resurssi, joten sekin täytyy mockata, jotta
// avattu hakemus näyttää HAKIJAT-listan (eikä jonkin oikean, kehitys-
// tietokannasta löytyvän hakemuksen) tiedot.
const applicationDetailRoute = (route: Route) => {
  if (route.request().method() !== 'GET') {
    return route.fallback()
  }
  const key = new URL(route.request().url()).pathname.split('/').pop()
  const index = HAKIJAT.findIndex((hakija) => hakija.key === key)
  if (index === -1) {
    return route.fallback()
  }
  return route.fulfill({
    json: {
      application: {
        ...hakemusJson(HAKIJAT[index], index + 1),
        answers: [],
        'rights-by-hakukohde': {},
        tarjonta: null,
      },
      events: [],
      review: {},
      'review-notes': [],
      'attachment-reviews': {},
      'hakukohde-reviews': [],
      form: { key: 'massatoiminnot-lahetys-test-form', content: [] },
      'information-requests': [],
    },
  })
}

const naytaHaunHakemukset = async () => {
  await page.goto(
    `/lomake-editori/applications/haku/${HAKU_OID}?ensisijaisesti=false`
  )
  await page.getByTestId('show-results').click()
}

const applicantNameCells = () =>
  page.locator('.application-handling__list-row--applicant-name')

const applicantRow = (lastName: string) =>
  page
    .locator(
      '.application-handling__list-row:not(.application-handling__list-header)'
    )
    .filter({ hasText: lastName })

const mainHeading = () =>
  page.locator('.application-handling__review-area-main-heading')

const hakukohdeStateCells = () =>
  page.locator('.application-handling__hakukohde-state')

test.beforeAll(async ({ browser }) => {
  test.setTimeout(120000)
  page = await browser.newPage()

  await page.route(
    '**/lomake-editori/api/valinta-tulos-service/valinnan-tulos/hakemus?hakemusOid=*',
    (route) => route.fulfill({ json: [] })
  )
  await page.route(
    '**/lomake-editori/api/applications/list',
    applicationsListRoute
  )
  await page.route(
    '**/lomake-editori/api/applications/1.2.246.562.11.*',
    applicationDetailRoute
  )
  await page.route(
    '**/lomake-editori/api/valintalaskentakoostepalvelu/valintaperusteet/hakukohde/*/kayttaa-valintalaskentaa',
    (route) => route.fulfill({ json: false })
  )
  await page.route(
    '**/lomake-editori/api/haku*',
    fixtureFromFile('hautmassatoiminnoille.json')
  )
  await page.route(
    '**/lomake-editori/api/haut*',
    fixtureFromFile('hautmassatoiminnoille.json')
  )
  await page.route(
    '**/lomake-editori/api/applications/mass-update',
    (route) => {
      massUpdateApplied = true
      return route.fulfill({ json: {} })
    }
  )
  await page.route(
    '**/lomake-editori/api/applications/mass-information-request',
    (route) => route.fulfill({ json: {} })
  )
  await page.route(
    '**/lomake-editori/api/applications/information-request',
    (route) => route.fulfill({ json: {} })
  )

  await kirjauduVirkailijanNakymaan(page, 'OPINTO-OHJAAJA')
})

test.afterAll(async () => {
  await page.close()
})

test.describe('Massamuutos hakemusten käsittelyvaiheeseen', () => {
  const massUpdatePopup = () =>
    page.locator('.application-handling__mass-edit-review-states-popup')

  const stateListContainer = (testId: 'from-list' | 'to-list') =>
    massUpdatePopup().getByTestId(testId)

  const selectMassUpdateState = async (container: Locator, label: string) => {
    await container
      .locator('.application-handling__review-state-row--selected')
      .click()
    await container
      .locator(
        '.application-handling__review-state-list--opened .application-handling__review-state-row--mass-update',
        { hasText: label }
      )
      .click()
  }

  const submitButton = () =>
    massUpdatePopup().locator(
      '.application-handling__mass-edit-review-states-submit-button, .application-handling__mass-edit-review-states-submit-button--confirm'
    )

  test('vaihtaa valittujen hakemusten käsittelyvaiheen ja päivittää listan', async () => {
    await naytaHaunHakemukset()
    await expect(applicantNameCells()).toHaveCount(3)
    await expect(hakukohdeStateCells()).toHaveText([
      'Käsittelemättä',
      'Käsittelemättä',
      'Käsittelyssä',
    ])

    await page.getByTestId('mass-update').click()
    await expect(massUpdatePopup()).toBeVisible()

    await selectMassUpdateState(
      stateListContainer('from-list'),
      'Käsittelemättä'
    )
    await selectMassUpdateState(stateListContainer('to-list'), 'Käsitelty')

    await expect(submitButton()).toHaveText('Muuta')
    await submitButton().click()
    await expect(submitButton()).toHaveText('Vahvista muutos')
    await submitButton().click()

    await expect(massUpdatePopup()).toBeHidden({ timeout: 5000 })
    await expect(hakukohdeStateCells()).toHaveText(
      ['Käsitelty', 'Käsitelty', 'Käsittelyssä'],
      { timeout: 5000 }
    )
  })
})

test.describe('Massaviestin lähettäminen hakijoille', () => {
  const massInfoPopup = () =>
    page.locator('.application-handling__mass-information-request-popup')
  const massInfoText = () => massInfoPopup().locator('p').first()
  const massInfoSubject = () =>
    massInfoPopup().locator(
      'input.application-handling__information-request-text-input'
    )
  const massInfoContent = () =>
    massInfoPopup().locator(
      'textarea.application-handling__information-request-message-area'
    )
  const massInfoSendButton = () =>
    massInfoPopup().locator(
      'button.application-handling__send-information-request-button'
    )
  const massInfoStatus = () =>
    massInfoPopup().locator('.application-handling__information-request-status')

  test('lähettää massaviestin ja palauttaa lomakkeen oletustilaan', async () => {
    await naytaHaunHakemukset()

    await page
      .locator('.application-handling__mass-information-request-link')
      .click()
    await expect(massInfoPopup()).toBeVisible()

    await expect(massInfoText()).toHaveText('Lähetä sähköposti 3 hakijalle:')
    await expect(massInfoSubject()).toHaveValue('')
    await expect(massInfoContent()).toHaveValue('')
    await expect(massInfoSendButton()).toHaveText('Lähetä')
    await expect(massInfoSendButton()).toBeDisabled()

    await fillField(page, massInfoSubject(), 'Otsikko!')
    await fillField(page, massInfoContent(), 'Sisältöä')
    await expect(massInfoSendButton()).toBeEnabled()

    await massInfoSendButton().click()
    await expect(massInfoSendButton()).toHaveText('Vahvista 3 viestin lähetys')

    await massInfoSendButton().click()
    await expect(massInfoStatus()).toHaveText(
      'Viestit lisätty lähetysjonoon!',
      {
        timeout: 5000,
      }
    )

    await expect(massInfoText()).toHaveText('Lähetä sähköposti 3 hakijalle:', {
      timeout: 6000,
    })
    await expect(massInfoSubject()).toHaveValue('')
    await expect(massInfoContent()).toHaveValue('')
    await expect(massInfoSendButton()).toHaveText('Lähetä')
    await expect(massInfoSendButton()).toBeDisabled()
  })
})

test.describe('Yksittäisen viestin lähettäminen hakijalle', () => {
  const singleInfoButton = () =>
    page.locator('.application-handling__send-message-button')
  const singleInfoPopup = () =>
    page.locator('.application-handling__-information-request-popup')
  const singleInfoText = () => singleInfoPopup().locator('p').first()
  const singleInfoSubject = () =>
    singleInfoPopup().locator(
      'input.application-handling__information-request-text-input'
    )
  const singleInfoContent = () =>
    singleInfoPopup().locator(
      'textarea.application-handling__information-request-message-area'
    )
  const singleInfoSendButton = () =>
    singleInfoPopup().locator(
      'button.application-handling__send-information-request-button'
    )
  const singleInfoStatus = () =>
    singleInfoPopup().locator(
      '.application-handling__information-request-status'
    )

  // "Yksittäisen viestin lähetystoiminto"-toiminto on hakemuskohtainen (näkyy
  // vasta, kun tietty hakemus on avattu tarkasteltavaksi), toisin kuin
  // massaviesti, jonka voi lähettää suoraan hakemuslistalta.
  test('lähettää viestin yhdelle hakijalle ja sulkee ikkunan lähetyksen jälkeen', async () => {
    await naytaHaunHakemukset()

    await applicantRow('Aallikko').click()
    await expect(mainHeading()).toContainText('Aallikko')

    // force: true, koska Playwrightin omat "receives events" -tarkastukset
    // toistuvasti tulkitsevat painikkeen kääre-elementin (.application-
    // handling__single-information-request-container, jonka ainoa lapsi
    // painike on) peittävän klikkauspisteen, vaikka elementFromPoint samassa
    // pisteessä palauttaa aina itse painikkeen — painike on siis aidosti
    // klikattavissa, väärä positiivinen koskee vain Playwrightin omaa
    // esitarkastusta.
    // eslint-disable-next-line playwright/no-force-option
    await singleInfoButton().click({ force: true })
    await expect(singleInfoPopup()).toBeVisible()

    await expect(singleInfoText()).toHaveText(
      'Olet lähettämässä sähköpostia 1 hakijalle: Aallikko, Aino'
    )
    await expect(singleInfoSubject()).toHaveValue('')
    await expect(singleInfoContent()).toHaveValue('')
    await expect(singleInfoSendButton()).toHaveText('Lähetä')
    await expect(singleInfoSendButton()).toBeDisabled()

    await fillField(page, singleInfoSubject(), 'Otsikko!')
    await fillField(page, singleInfoContent(), 'Sisältöä')
    await expect(singleInfoSendButton()).toBeEnabled()

    await singleInfoSendButton().click()
    await expect(singleInfoStatus()).toHaveText(
      'Viesti lisätty lähetysjonoon!',
      { timeout: 5000 }
    )

    // Toisin kuin massaviesti-ikkuna, yksittäisen viestin ikkuna sulkeutuu
    // itsestään lähetyksen jälkeen (ks.
    // virkailija_information_request_handlers.cljs:n
    // reset-submit-single-information-request-state, joka ajetaan 3s
    // viiveellä ja asettaa popupin näkyvyyden pois päältä).
    await expect(singleInfoPopup()).toBeHidden({ timeout: 5000 })
  })
})
