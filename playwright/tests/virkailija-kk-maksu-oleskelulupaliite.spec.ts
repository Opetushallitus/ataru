import { test, expect, Page, Request } from '@playwright/test'
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

type FormNode = {
  id?: string
  fieldType?: string
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

const attachmentFixture = (id: string, label: string): FormNode => ({
  fieldClass: 'formField',
  fieldType: 'attachment',
  id,
  label: { fi: label },
  metadata: systemMetadata,
  params: {},
  validators: [],
})

// Merkinnät näytetään hakemusmaksumoduulin oleskelulupaliitteille, jotka
// tunnistetaan moduulin kiinteistä kenttä-id:istä.
const PERMIT_ATTACHMENT_LABEL = 'EU:n sininen kortti'
const OTHER_ATTACHMENT_LABEL = 'Muu liite'

const isoDateYearsFromToday = (years: number) => {
  const date = new Date()
  date.setFullYear(date.getFullYear() + years)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const permitValidUntil = isoDateYearsFromToday(1)

const HAKIJA = {
  'first-name': 'Oleskelu',
  'last-name': 'Luvallinen',
  ssn: '020202A0202',
  email: 'oleskelu@example.com',
  'verify-email': 'oleskelu@example.com',
  phone: '0400000003',
  address: 'Katutie 3',
  'postal-code': '00100',
  'home-town': 'Forssa',
}

let page: Page
let lomakkeenAvain: string

const injectAttachmentFields = async (formId: number) => {
  const getResponse = await page.request.get(
    `/lomake-editori/api/forms/${formId}`
  )
  const form = (await getResponse.json()) as {
    content: FormNode[]
    [key: string]: unknown
  }
  const hakukohteet = form.content.find((node) => node.id === 'hakukohteet')
  const personInfoModule = form.content.find(
    (node) => node.module === 'person-info'
  )
  if (!hakukohteet || !personInfoModule) {
    throw new Error('Lomakkeen sisällön muodostaminen epäonnistui')
  }
  const formWithoutTimestamp = { ...form }
  delete formWithoutTimestamp['created-time']
  const postResponse = await page.request.post('/lomake-editori/api/forms', {
    data: {
      ...formWithoutTimestamp,
      content: [
        hakukohteet,
        personInfoModule,
        attachmentFixture('eu-blue-card-attachment', PERMIT_ATTACHMENT_LABEL),
        attachmentFixture('pw-other-attachment', OTHER_ATTACHMENT_LABEL),
      ],
    },
  })
  if (!postResponse.ok()) {
    throw new Error(`Lomakkeen ${formId} tallennus epäonnistui`)
  }
}

const isPermitCheckPut = (request: Request) =>
  request.method() === 'PUT' &&
  request.url().includes('/kk-payment-permit-check/eu-blue-card-attachment')

const attachmentRow = (label: string) =>
  page.locator('.application__attachment-review-row').filter({
    has: page.locator('.application__attachment-review-row-label', {
      hasText: label,
    }),
  })

const permitCheck = () =>
  attachmentRow(PERMIT_ATTACHMENT_LABEL).locator('.kk-payment-permit-check')

const addInfo = () =>
  permitCheck().getByText(
    'Lisää oleskelulupakortin voimassaolo ja tieto hakijan hakemusmaksuvelvollisuudesta.'
  )

const clearButton = (label: string) =>
  permitCheck().getByRole('button', { name: `Tyhjennä valinta: ${label}` })

const permitCheckUpdatedEvents = () =>
  page.locator('.application-handling__event-row-header > span', {
    hasText: 'Oleskelulupaliitteen käsittelymerkinnät päivitetty',
  })

const openApplicationAttachments = async () => {
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
  await page
    .locator('.application-handling__attachment-review-toggle-container-link')
    .click()
}

test.beforeAll(async ({ browser }) => {
  test.setTimeout(120000)
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)

  const lomake = await lisaaLomake(page)
  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)
  await injectAttachmentFields(unsafeFoldOption(lomake.lomakkeenId))

  await Promise.all([
    page.goto(getHakijanNakymanOsoite(lomakkeenAvain)),
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenAvain))
    ),
  ])
  await taytaHenkilotietomoduuli(page, HAKIJA)
  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])

  await openApplicationAttachments()
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

test('näyttää merkinnät vain oleskelulupaliitteelle', async () => {
  await expect(permitCheck()).toBeVisible()
  await expect(
    attachmentRow(OTHER_ATTACHMENT_LABEL).locator('.kk-payment-permit-check')
  ).toHaveCount(0)
})

test('ohjaa puuttuvan liitteen tiedot Migrin lupatietopalvelusta', async () => {
  const row = attachmentRow(PERMIT_ATTACHMENT_LABEL)
  await expect(
    row.locator('.kk-payment-permit-check__notification').first()
  ).toHaveText(
    'Liite puuttuu. Tarkista tiedot Migrin lupatietopalvelusta (avautuu uuteen välilehteen) ja merkitse tiedot alle.'
  )
  await expect(
    row.locator(
      '.kk-payment-permit-check__side .application-handling__review-state-row'
    )
  ).toBeVisible()
  const migriLink = row.getByRole('link', {
    name: 'Migrin lupatietopalvelusta (avautuu uuteen välilehteen)',
    exact: true,
  })
  await expect(migriLink).toHaveAttribute(
    'href',
    'https://miso1.sovellukset.migri.fi'
  )
  await expect(migriLink).toHaveAttribute('target', '_blank')
})

test('tallentaa Migri-tarkistuksen ja näyttää lisäysohjeen', async () => {
  await expect(addInfo()).toHaveCount(0)

  const [request] = await Promise.all([
    page.waitForRequest(isPermitCheckPut),
    permitCheck().getByLabel('Kyllä').click(),
  ])
  await expect(permitCheck().getByLabel('Kyllä')).toBeChecked()

  expect(request.postDataJSON()).toEqual({
    'found-in-migri': true,
    'permit-valid-until': null,
    'payment-obligation-basis': null,
    'payment-obligation-other-reason': null,
  })
  await expect(addInfo()).toBeVisible()
  await expect(permitCheckUpdatedEvents()).toHaveCount(1)
})

test('tallentaa lupakortin voimassaolon ja maksuvelvollisuuden perusteen', async () => {
  await Promise.all([
    page.waitForRequest(isPermitCheckPut),
    permitCheck().getByLabel('Maksuvelvollinen, muu syy').click(),
  ])
  await Promise.all([
    page.waitForRequest(isPermitCheckPut),
    permitCheck().getByLabel('Lupakortti voimassa').fill(permitValidUntil),
  ])

  const otherReason = permitCheck().getByLabel('Tarkenne')
  await expect(otherReason).toHaveAttribute('maxlength', '500')
  await otherReason.fill('Työnantajan maksama koulutus')
  const [request] = await Promise.all([
    page.waitForRequest(isPermitCheckPut),
    otherReason.blur(),
  ])

  expect(request.postDataJSON()).toEqual({
    'found-in-migri': true,
    'permit-valid-until': permitValidUntil,
    'payment-obligation-basis': 'obligated-other',
    'payment-obligation-other-reason': 'Työnantajan maksama koulutus',
  })
  expect((await request.response())?.status()).toBe(200)
  // Ohje piilotetaan, kun molemmat sen pyytämät tiedot on täytetty
  await expect(addInfo()).toHaveCount(0)
})

test('merkinnät säilyvät hakemuksen uudelleenavauksessa', async () => {
  await openApplicationAttachments()

  await expect(permitCheck().getByLabel('Kyllä')).toBeChecked()
  await expect(permitCheck().getByLabel('Lupakortti voimassa')).toHaveValue(
    permitValidUntil
  )
  await expect(
    permitCheck().getByLabel('Maksuvelvollinen, muu syy')
  ).toBeChecked()
  await expect(permitCheck().getByLabel('Tarkenne')).toHaveValue(
    'Työnantajan maksama koulutus'
  )
  await expect(permitCheckUpdatedEvents()).not.toHaveCount(0)
})

test('ei tallenna lupakortin voimassaoloa sallitun välin ulkopuolelta', async () => {
  const validUntil = permitCheck().getByLabel('Lupakortti voimassa')
  await validUntil.fill(isoDateYearsFromToday(10))
  await expect(
    permitCheck().getByText('Päivämäärää ei tallennettu. Sallittu väli on')
  ).toBeVisible()
  await expect(validUntil).toHaveAttribute('aria-invalid', 'true')

  await openApplicationAttachments()
  await expect(validUntil).toHaveValue(permitValidUntil)
})

test('siirtää näppäimistön fokuksen näkyvästi radiopainikkeisiin', async () => {
  await clearButton('Tiedot löytyvät Migrin lupatietopalvelusta').focus()
  await page.keyboard.press('Tab')

  const foundInMigri = permitCheck().getByLabel('Kyllä')
  await expect(foundInMigri).toBeFocused()
  await expect(foundInMigri).toHaveCSS('outline-style', 'auto')
})

test('tyhjentää hakemusmaksuvelvollisuuden ja Migri-tarkistuksen valinnat', async () => {
  const obligation = permitCheck().getByLabel('Maksuvelvollinen, muu syy')
  await expect(obligation).toBeChecked()
  // Tyhjennysnappi ei kuulu kenttäryhmän nimeen
  await expect(
    permitCheck().getByRole('group', {
      name: /^Hakemusmaksuvelvollisuus( \*)?$/,
    })
  ).toBeVisible()

  const [obligationRequest] = await Promise.all([
    page.waitForRequest(isPermitCheckPut),
    clearButton('Hakemusmaksuvelvollisuus').click(),
  ])
  expect(obligationRequest.postDataJSON()).toEqual({
    'found-in-migri': true,
    'permit-valid-until': permitValidUntil,
    'payment-obligation-basis': null,
    'payment-obligation-other-reason': null,
  })
  expect((await obligationRequest.response())?.status()).toBe(200)
  await expect(obligation).not.toBeChecked()
  await expect(clearButton('Hakemusmaksuvelvollisuus')).toHaveCount(0)
  await expect(permitCheck().getByLabel('Ei maksuvelvollinen')).toBeFocused()
  await expect(addInfo()).toBeVisible()

  // Tarkenne tyhjennetään perusteen mukana
  await Promise.all([page.waitForRequest(isPermitCheckPut), obligation.click()])
  await expect(permitCheck().getByLabel('Tarkenne')).toHaveValue('')
  await Promise.all([
    page.waitForRequest(isPermitCheckPut),
    clearButton('Hakemusmaksuvelvollisuus').click(),
  ])

  const [foundInMigriRequest] = await Promise.all([
    page.waitForRequest(isPermitCheckPut),
    clearButton('Tiedot löytyvät Migrin lupatietopalvelusta').click(),
  ])
  expect(foundInMigriRequest.postDataJSON()).toMatchObject({
    'found-in-migri': null,
  })
  expect((await foundInMigriRequest.response())?.status()).toBe(200)
  await expect(permitCheck().getByLabel('Kyllä')).toBeFocused()

  await openApplicationAttachments()
  await expect(permitCheck().getByLabel('Kyllä')).not.toBeChecked()
  await expect(
    permitCheck().getByLabel('Ei', { exact: true })
  ).not.toBeChecked()
  await expect(obligation).not.toBeChecked()
  await expect(permitCheck().getByLabel('Lupakortti voimassa')).toHaveValue(
    permitValidUntil
  )
})

// Periytyminen tehdään maksuajossa, joten periytynyt merkintä lisätään
// hakemuksen hakuvastaukseen.
const isApplicationFetch = (url: URL) =>
  /^\/lomake-editori\/api\/applications\/[0-9.]+$/.test(url.pathname)

const inheritedCheck = {
  'attachment-key': 'eu-blue-card-attachment',
  'found-in-migri': true,
  'permit-valid-until': permitValidUntil,
  'payment-obligation-basis': 'not-obligated',
  'payment-obligation-other-reason': null,
  'checked-time': '2025-01-12T08:00:00.000Z',
  'inherited-from-application-key': '1.2.246.562.11.00000000000000000099',
  'inherited-time': '2025-01-14T08:00:00.000Z',
  'original-check': {
    'application-key': '1.2.246.562.11.00000000000000000099',
    'checked-time': '2025-01-12T08:00:00.000Z',
    'virkailija-name': 'Hemuli Hemuli?',
    organization: {
      oid: '1.2.246.562.10.2000000002',
      name: { fi: 'Testikorkeakoulu' },
    },
  },
}

const inheritedNotification = () =>
  attachmentRow(PERMIT_ATTACHMENT_LABEL).locator(
    '.kk-payment-permit-check__inherited'
  )

test('näyttää periytyneiden merkintöjen alkuperäisen tarkastuksen, kunnes virkailija muuttaa merkintöjä', async () => {
  await page.route(isApplicationFetch, async (route) => {
    const response = await route.fetch()
    const body = await response.json()
    await route.fulfill({
      response,
      json: { ...body, 'kk-payment-permit-checks': [inheritedCheck] },
    })
  })
  await openApplicationAttachments()
  await page.unroute(isApplicationFetch)

  await expect(inheritedNotification()).toContainText(
    'Luvan tarkistustiedot tuotu toisesta hakemuksesta.'
  )
  // Ohje Migrin lupatietopalvelusta korvautuu periytymisen ilmoituksella
  await expect(
    attachmentRow(PERMIT_ATTACHMENT_LABEL).getByRole('link', {
      name: 'Migrin lupatietopalvelusta (avautuu uuteen välilehteen)',
    })
  ).toHaveCount(0)
  const details = inheritedNotification().locator('dl')
  await expect(details).toHaveCount(0)

  const toggle = inheritedNotification().getByRole('button', {
    name: 'Näytä tarkemmat tiedot.',
  })
  await expect(toggle).toHaveAttribute('aria-expanded', 'false')
  await toggle.click()
  await expect(
    inheritedNotification().getByRole('button', {
      name: 'Sulje tarkemmat tiedot.',
    })
  ).toHaveAttribute('aria-expanded', 'true')
  await expect(details.locator('dt')).toHaveText([
    'Hakemusnumero',
    'Korkeakoulu',
    'Käsittelypvm',
    'Käsittelijä',
  ])
  await expect(details.locator('dd')).toHaveText([
    '1.2.246.562.11.00000000000000000099',
    'Testikorkeakoulu',
    '12.1.2025',
    'Hemuli Hemuli?',
  ])
  await expect(permitCheck().getByLabel('Kyllä')).toBeChecked()

  const [request] = await Promise.all([
    page.waitForRequest(isPermitCheckPut),
    permitCheck().getByLabel('Ei', { exact: true }).click(),
  ])
  expect((await request.response())?.status()).toBe(200)
  await expect(inheritedNotification()).toHaveCount(0)
})

test('näyttää alkuperäisen tarkastuksen korkeakoulun tuntemattomana, kun sitä ei ole tiedossa', async () => {
  await page.route(isApplicationFetch, async (route) => {
    const response = await route.fetch()
    const body = await response.json()
    await route.fulfill({
      response,
      json: {
        ...body,
        'kk-payment-permit-checks': [
          {
            ...inheritedCheck,
            'original-check': {
              ...inheritedCheck['original-check'],
              organization: null,
            },
          },
        ],
      },
    })
  })
  await openApplicationAttachments()
  await page.unroute(isApplicationFetch)

  await inheritedNotification()
    .getByRole('button', { name: 'Näytä tarkemmat tiedot.' })
    .click()
  await expect(
    inheritedNotification().locator('dl').locator('dd').nth(1)
  ).toHaveText('Ei tiedossa')
})
