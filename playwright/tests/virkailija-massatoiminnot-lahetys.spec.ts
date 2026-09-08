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

// Aakkosjärjestyksessä (oletuslajittelu), jotta massamuutoksen jälkeinen
// tila voidaan todentaa hakemuslistan riviluetteloa vastaan indeksin
// perusteella: kaksi ensimmäistä ovat aluksi "Käsittelemättä", kolmas
// pysyy koskemattomana "Käsittelyssä"-tilassa massamuutoksen ajan.
const HAKIJAT: Array<Hakija> = [
  {
    key: '1.2.246.562.11.00000000000000000101',
    lastName: 'Aallikko',
    firstName: 'Aino',
    ssn: '020202A0202',
    processingState: 'unprocessed',
  },
  {
    key: '1.2.246.562.11.00000000000000000102',
    lastName: 'Kokko',
    firstName: 'Kalle',
    ssn: '160600A999C',
    processingState: 'unprocessed',
  },
  {
    key: '1.2.246.562.11.00000000000000000103',
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

const naytaHaunHakemukset = async () => {
  await page.goto(
    `/lomake-editori/applications/haku/${HAKU_OID}?ensisijaisesti=false`
  )
  await page.getByTestId('show-results').click()
}

const applicantNameCells = () =>
  page.locator('.application-handling__list-row--applicant-name')

const hakukohdeStateCells = () =>
  page.locator('.application-handling__hakukohde-state')

test.beforeAll(async ({ browser }) => {
  test.setTimeout(120000)
  page = await browser.newPage()

  await page.route(
    '**/lomake-editori/api/valinta-tulos-service/valinnan-tulos/hakemus/?hakemusOid=*',
    (route) => route.fulfill({ json: [] })
  )
  await page.route(
    '**/lomake-editori/api/applications/list',
    applicationsListRoute
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
