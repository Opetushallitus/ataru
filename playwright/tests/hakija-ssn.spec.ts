import { test, expect, Page } from '@playwright/test'

import {
  getHakemuksenLahettamisenOsoite,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
} from '../playwright-ataru-utils'
import {
  fillField,
  selectOption,
  unsafeFoldOption,
  waitForResponse,
} from '../playwright-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenTunnisteet: { lomakkeenAvain: string; lomakkeenId: number }

// Generate unique test identifier to prevent conflicts with parallel tests
const testRunId = `${Date.now()}-${Math.random().toString(36).substring(7)}`

const formName = `SSN_testilomake_${testRunId}`

const ssnTestFormFixture = {
  name: { fi: formName },
  'created-by': '1.2.246.562.11.11111111111',
  'organization-oid': '1.2.246.562.10.0439845',
  languages: ['fi'],
  locked: null,
  'locked-by': null,
}

type FormNode = {
  id?: string
  fieldType?: string
  options?: Array<{ value: string; label?: Record<string, string> }>
  children?: FormNode[]
  [key: string]: unknown
}

const getFieldById = (page: Page, id: string) => page.locator(`[id="${id}"]`)

const expectFieldVisible = async (page: Page, id: string) => {
  await expect(getFieldById(page, id)).toBeVisible()
}

const expectFieldHidden = async (page: Page, id: string) => {
  await expect(getFieldById(page, id)).toHaveCount(0)
}

const invalidFieldStatus = (page: Page) =>
  page.locator('.application__invalid-field-status-title')

const withInjectedNationalityOptions = (nodes: FormNode[]): FormNode[] => {
  return nodes.map((node) => {
    const next = { ...node }
    if (next.id === 'nationality' && next.fieldType === 'dropdown') {
      next.options = [
        { value: '246', label: { fi: 'Suomi', sv: 'Finland', en: 'Finland' } },
        {
          value: '740',
          label: { fi: 'Suriname', sv: 'Surinam', en: 'Suriname' },
        },
      ]
    }
    if (Array.isArray(next.children)) {
      next.children = withInjectedNationalityOptions(next.children)
    }
    return next
  })
}

const toSsnFixtureContent = (nodes: FormNode[]): FormNode[] => {
  const hakukohteet = nodes.find(
    (node) => node.id === 'hakukohteet' || node.fieldType === 'hakukohteet'
  )
  const personInfoModule = nodes.find(
    (node) => node.module === 'person-info' || node.id === 'onr'
  )

  if (!hakukohteet || !personInfoModule) {
    throw new Error(
      'Failed to build SSN fixture content: missing hakukohteet or person-info-module'
    )
  }

  return [hakukohteet, personInfoModule]
}

const injectSsnFormData = async (page: Page, formId: number) => {
  const getResponse = await page.request.get(
    `/lomake-editori/api/forms/${formId}`
  )
  if (!getResponse.ok()) {
    throw new Error(`Failed to fetch form ${formId} for SSN data injection`)
  }

  const form = (await getResponse.json()) as {
    id?: number
    key?: string
    name?: Record<string, string>
    'created-by'?: string
    'organization-oid'?: string
    languages?: string[]
    locked?: string | null
    'locked-by'?: string | null
    content: FormNode[]
    [key: string]: unknown
  }

  // Keep the generated form id/key to isolate from other test runs
  // Only override content, name, and fixture metadata
  const updatedForm = {
    ...form,
    name: ssnTestFormFixture.name,
    'created-by': ssnTestFormFixture['created-by'],
    'organization-oid': ssnTestFormFixture['organization-oid'],
    languages: ssnTestFormFixture.languages,
    locked: ssnTestFormFixture.locked,
    'locked-by': ssnTestFormFixture['locked-by'],
    content: withInjectedNationalityOptions(toSsnFixtureContent(form.content)),
  }

  const postResponse = await page.request.post('/lomake-editori/api/forms', {
    data: updatedForm,
  })
  if (!postResponse.ok()) {
    throw new Error(
      `Failed to persist injected SSN form data for form ${formId}`
    )
  }
}

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await Promise.all([
    waitForResponse(page, 'GET', (url) =>
      url.includes('/lomake-editori/api/forms')
    ),
    kirjauduVirkailijanNakymaan(page),
  ])

  const lomake = await lisaaLomake(page)

  lomakkeenTunnisteet = {
    lomakkeenAvain: unsafeFoldOption(lomake.lomakkeenAvain),
    lomakkeenId: unsafeFoldOption(lomake.lomakkeenId),
  }

  try {
    await injectSsnFormData(page, lomakkeenTunnisteet.lomakkeenId)
  } catch (error) {
    // Clean up form if injection fails
    try {
      await poistaLomake(page.request, lomakkeenTunnisteet.lomakkeenAvain)
    } catch {
      // Ignore cleanup errors
    }
    throw error
  }

  await Promise.all([
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenTunnisteet.lomakkeenAvain))
    ),
    page.goto(getHakijanNakymanOsoite(lomakkeenTunnisteet.lomakkeenAvain)),
  ])
})

test('Hakijan SSN-lomake näyttää oikeat kentät ja voidaan lähettää ilman suomalaista henkilötunnusta', async () => {
  await expect(page.getByTestId('application-header-label')).toHaveText(
    formName
  )
  await expect(page.locator('.application__form-field')).toHaveCount(14)
  await expect(page.getByTestId('send-application-button')).toBeDisabled()

  await expectFieldVisible(page, 'ssn')
  await expectFieldHidden(page, 'have-finnish-ssn')
  await expectFieldHidden(page, 'gender')
  await expectFieldHidden(page, 'birth-date')
  await expectFieldHidden(page, 'birthplace')
  await expectFieldHidden(page, 'passport-number')
  await expectFieldHidden(page, 'national-id-number')
  await expect(invalidFieldStatus(page)).toHaveText('Tarkista 11 tietoa')

  await selectOption(page, getFieldById(page, '0-nationality'), '740')
  await expectFieldVisible(page, 'have-finnish-ssn')
  await expectFieldVisible(page, 'ssn')
  await expectFieldHidden(page, 'gender')
  await expectFieldHidden(page, 'birth-date')
  await expectFieldHidden(page, 'birthplace')
  await expectFieldHidden(page, 'passport-number')
  await expectFieldHidden(page, 'national-id-number')
  await expect(invalidFieldStatus(page)).toHaveText('Tarkista 10 tietoa')

  await selectOption(page, getFieldById(page, 'have-finnish-ssn'), 'false')
  await expectFieldHidden(page, 'ssn')
  await expectFieldVisible(page, 'gender')
  await expectFieldVisible(page, 'birth-date')
  await expectFieldVisible(page, 'birthplace')
  await expectFieldVisible(page, 'passport-number')
  await expectFieldVisible(page, 'national-id-number')
  await expect(invalidFieldStatus(page)).toHaveText('Tarkista 12 tietoa')

  await selectOption(page, getFieldById(page, 'have-finnish-ssn'), 'true')
  await expectFieldVisible(page, 'have-finnish-ssn')
  await expectFieldVisible(page, 'ssn')
  await expectFieldHidden(page, 'gender')
  await expectFieldHidden(page, 'birth-date')
  await expectFieldHidden(page, 'birthplace')
  await expectFieldHidden(page, 'passport-number')
  await expectFieldHidden(page, 'national-id-number')
  await expect(invalidFieldStatus(page)).toHaveText('Tarkista 10 tietoa')

  await selectOption(page, getFieldById(page, '0-nationality'), '246')
  await expectFieldVisible(page, 'ssn')
  await expectFieldHidden(page, 'have-finnish-ssn')
  await expectFieldHidden(page, 'gender')
  await expectFieldHidden(page, 'birth-date')
  await expectFieldHidden(page, 'birthplace')
  await expectFieldHidden(page, 'passport-number')
  await expectFieldHidden(page, 'national-id-number')
  await expect(invalidFieldStatus(page)).toHaveText('Tarkista 10 tietoa')

  await fillField(page, getFieldById(page, 'first-name'), 'Etunimi Tokanimi')
  await getFieldById(page, 'first-name').press('Tab')
  await expect(page.getByTestId('preferred-name-input')).toHaveValue('Etunimi')

  await fillField(page, getFieldById(page, 'last-name'), 'Sukunimi')

  await selectOption(page, getFieldById(page, '0-nationality'), '740')
  await expectFieldVisible(page, 'have-finnish-ssn')

  await selectOption(page, getFieldById(page, 'have-finnish-ssn'), 'false')
  await expectFieldVisible(page, 'birth-date')

  await fillField(page, getFieldById(page, 'birth-date'), '01.01.1990')

  await selectOption(page, getFieldById(page, 'gender'), '1')

  await fillField(
    page,
    getFieldById(page, 'birthplace'),
    'Paramaribo, Suriname'
  )

  await fillField(page, getFieldById(page, 'passport-number'), '12345')

  await fillField(page, getFieldById(page, 'national-id-number'), 'id-12345')

  await fillField(page, page.getByTestId('email-input'), 'test@example.com')

  await fillField(
    page,
    page.getByTestId('verify-email-input'),
    'test@example.com'
  )

  await fillField(page, page.getByTestId('phone-input'), '0123456789')

  await fillField(page, page.getByTestId('address-input'), 'Katutie 12 B')

  await fillField(page, page.getByTestId('postal-code-input'), '40100')
  await expect(page.getByTestId('postal-office-input')).toHaveValue('JYVÄSKYLÄ')

  await selectOption(page, page.getByTestId('home-town-input'), '179')

  await expect(invalidFieldStatus(page)).toHaveCount(0)
  await expect(page.getByTestId('send-application-button')).toBeEnabled()

  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])

  await expect(page.getByText('Hakemus lähetetty')).toBeVisible()
  await page.getByTestId('send-feedback-button').click()

  const closeFeedbackFormButton = page.getByTestId('close-feedback-form-button')
  await closeFeedbackFormButton.click({ timeout: 5000 }).catch(() => undefined)

  await expect(page.getByTestId('application-header-label')).toHaveText(
    formName
  )

  await expect(page.locator('.application__text-field-paragraph')).toHaveText([
    'Etunimi Tokanimi',
    'Etunimi',
    'Sukunimi',
    'Suriname',
    '01.01.1990',
    'mies',
    'Paramaribo, Suriname',
    '12345',
    'id-12345',
    'test@example.com',
    '0123456789',
    'Suomi',
    'Katutie 12 B',
    '40100',
    'JYVÄSKYLÄ',
    'Jyväskylä',
    'suomi',
  ])
})
