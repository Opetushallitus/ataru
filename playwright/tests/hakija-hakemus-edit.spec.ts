import { expect, Page, test } from '@playwright/test'
import {
  fillField,
  unsafeFoldOption,
  waitForResponse,
} from '../playwright-utils'
import {
  getApplicationSecretById,
  getHakemuksenLahettamisenOsoite,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  taytaHenkilotietomoduuli,
  teeJaOdotaLomakkeenTallennusta,
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

const createTextFieldFixture = (
  id: string,
  label: string,
  fieldType: 'textField' | 'textArea' = 'textField'
): FormNode => ({
  fieldClass: 'formField',
  fieldType,
  id,
  label: { fi: label },
  metadata: systemMetadata,
  params: {},
  validators: [],
})

const createSingleChoiceFixture = (id: string, label: string): FormNode => ({
  fieldClass: 'formField',
  fieldType: 'singleChoice',
  id,
  label: { fi: label },
  metadata: systemMetadata,
  params: {},
  options: [
    { value: '0', label: { fi: 'Vaihtoehto A' } },
    { value: '1', label: { fi: 'Vaihtoehto B' } },
  ],
})

const legacyExtraFieldDefinitions = [
  { id: 'pw-f-01', label: 'Kenttä 01', value: 'Tekstikentän vastaus' },
  { id: 'pw-f-02', label: 'Kenttä 02', value: 'Toistuva vastaus 1' },
  { id: 'pw-f-03', label: 'Kenttä 03', value: 'Toistuva vastaus 2' },
  { id: 'pw-f-04', label: 'Kenttä 04', value: 'Toistuva vastaus 3' },
  {
    id: 'pw-f-05',
    label: 'Kenttä 05',
    value: 'Pakollisen tekstialueen vastaus',
    fieldType: 'textArea' as const,
  },
  { id: 'pw-f-06', label: 'Kenttä 06', value: 'Kolmas vaihtoehto' },
  { id: 'pw-f-07', label: 'Kenttä 07', value: 'Jatkokysymyksen vastaus' },
  { id: 'pw-f-08', label: 'Kenttä 08', value: 'Lisensiaatin tutkinto' },
  { id: 'pw-f-09', label: 'Kenttä 09', value: 'Toinen vaihtoehto' },
  { id: 'pw-f-10', label: 'Kenttä 10', value: 'En' },
  { id: 'pw-f-11', label: 'Kenttä 11', value: 'Arkkitehti' },
  {
    id: 'pw-f-12',
    label: 'Kenttä 12',
    value: 'Alkuperäinen vastaus',
    editedValue: 'Muokattu vastaus',
    fieldType: 'textArea' as const,
  },
  {
    id: 'pw-f-13',
    label: 'Kenttä 13',
    value: '',
    fieldType: 'singleChoice' as const,
  },
  { id: 'pw-f-14', label: 'Kenttä 14', value: 'Toinen vaihtoehto' },
  {
    id: 'pw-f-15',
    label: 'Kenttä 15',
    value: 'Pudotusvalikon 1. kysymys',
  },
  { id: 'pw-f-16', label: 'Kenttä 16', value: '1,323' },
  {
    id: 'pw-f-17',
    label: 'Kenttä 17',
    value: 'Entinen Neuvostoliitto',
  },
]

const legacyExtraFieldFixtures: FormNode[] = legacyExtraFieldDefinitions.map(
  ({ id, label, fieldType }) =>
    fieldType === 'singleChoice'
      ? createSingleChoiceFixture(id, label)
      : createTextFieldFixture(id, label, fieldType)
)

const hiddenCountingFieldFixture: FormNode = {
  fieldClass: 'formField',
  fieldType: 'attachment',
  id: 'pw-f-18-attachment',
  label: { fi: 'Liitekenttä' },
  metadata: systemMetadata,
  params: {},
  validators: [],
}

const injectEditFieldFormData = async (page: Page, formId: number) => {
  const getResponse = await page.request.get(
    `/lomake-editori/api/forms/${formId}`
  )
  if (!getResponse.ok()) {
    throw new Error(`Failed to fetch form ${formId}`)
  }

  const form = (await getResponse.json()) as {
    name?: Record<string, string>
    content: FormNode[]
    [key: string]: unknown
  }

  const hakukohteet = form.content.find(
    (node) => node.id === 'hakukohteet' || node.fieldType === 'hakukohteet'
  )
  const personInfoModule = form.content.find(
    (node) => node.module === 'person-info' || node.id === 'onr'
  )

  if (!hakukohteet || !personInfoModule) {
    throw new Error('Failed to build self-contained form content')
  }

  const formWithoutTimestamp = { ...form }
  delete formWithoutTimestamp['created-time']

  const updatedForm = {
    ...formWithoutTimestamp,
    content: [
      hakukohteet,
      personInfoModule,
      ...legacyExtraFieldFixtures,
      hiddenCountingFieldFixture,
    ],
  }

  const postResponse = await page.request.post('/lomake-editori/api/forms', {
    data: updatedForm,
  })

  if (!postResponse.ok()) {
    throw new Error(`Failed to persist form ${formId}`)
  }
}

const getFormFields = (page: Page) =>
  page.locator('.application__form-content-area .application__form-field')

const getSubmitButton = (page: Page) =>
  page.getByTestId('send-application-button')

const requireModifySecret = (secret: string | null): string => {
  if (!secret) {
    throw new Error(
      'Missing modify secret after bootstrap application creation'
    )
  }
  return secret
}

const requireValue = <T>(value: T | null | undefined, message: string): T => {
  if (value == null) {
    throw new Error(message)
  }
  return value
}

const loginWithStrongAuth = async (
  page: Page,
  target: string
): Promise<void> => {
  const bootstrapResponse = await page.request.post(
    '/hakemus/fake-strong-auth-session'
  )
  expect(bootstrapResponse.ok()).toBeTruthy()

  await page.goto(target)

  const sessionResponse = await page.request.get('/hakemus/auth/session')
  expect(sessionResponse.ok()).toBeTruthy()

  const session = (await sessionResponse.json()) as {
    'logged-in': boolean
    'auth-type': string
  }

  expect(session['logged-in']).toBe(true)
  expect(session['auth-type']).toBe('strong')
}

const allowHakeminenTunnistautuneena = async (
  page: Page,
  formId: number
): Promise<void> => {
  const getResponse = await page.request.get(
    `/lomake-editori/api/forms/${formId}`
  )

  const form = (await getResponse.json()) as {
    properties?: Record<string, unknown>
    [key: string]: unknown
  }

  const formWithoutTimestamp = { ...form }
  delete formWithoutTimestamp['created-time']
  const postResponse = await page.request.post('/lomake-editori/api/forms', {
    data: {
      ...formWithoutTimestamp,
      properties: {
        ...form.properties,
        'allow-hakeminen-tunnistautuneena': true,
      },
    },
  })

  expect(postResponse.ok()).toBeTruthy()
}

test.describe('Hakijan hakemuksen muokkaus', () => {
  let page: Page
  let lomakkeenAvain: string | null = null
  let modifySecret: string | null = null
  let editedPhoneNumber: string = ''

  const seededValues = {
    firstName: 'Etunimi Tokanimi',
    preferredName: 'Etunimi',
    lastName: 'Sukunimi',
    ssn: '020202A0202',
    email: 'test@example.com',
    phone: '0123456789',
    address: 'Katutie 12 B',
    postalCode: '40100',
    homeTown: 'Forssa',
  }

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage()

    await kirjauduVirkailijanNakymaan(page)
    const lomake = await lisaaLomake(page)
    lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)
    const lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)

    await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
      await page.getByTestId('form-name-input').fill('Testilomake')
    })
    await injectEditFieldFormData(page, lomakkeenId)

    const formKey = lomakkeenAvain

    await Promise.all([
      waitForResponse(page, 'GET', (url) =>
        url.includes(getLomakkeenHaunOsoite(formKey))
      ),
      page.goto(getHakijanNakymanOsoite(formKey)),
    ])

    await taytaHenkilotietomoduuli(page, {
      'first-name': seededValues.firstName,
      'last-name': seededValues.lastName,
      ssn: seededValues.ssn,
      email: seededValues.email,
      'verify-email': seededValues.email,
      phone: seededValues.phone,
      address: seededValues.address,
      'postal-code': seededValues.postalCode,
      'home-town': seededValues.homeTown,
    })

    await page.getByTestId('first-name-input').press('Tab')
    await expect(page.getByTestId('preferred-name-input')).toHaveValue(
      seededValues.preferredName
    )

    for (const field of legacyExtraFieldDefinitions) {
      if (field.value.length === 0) {
        continue
      }

      const input = page.getByRole('textbox', {
        name: new RegExp(field.label, 'i'),
      })
      await fillField(page, input, field.value)
      await input.press('Tab')
    }

    const [submitResponse] = await Promise.all([
      waitForResponse(page, 'POST', (url) =>
        url.includes(getHakemuksenLahettamisenOsoite())
      ),
      getSubmitButton(page).click(),
    ])
    await expect(
      page.locator('.application__sent-placeholder-text')
    ).toBeVisible()

    const submitPayload = (await submitResponse.json()) as { id?: number }
    const applicationId = submitPayload.id
    if (!applicationId) {
      throw new Error('Missing application id in submit response')
    }

    modifySecret = await getApplicationSecretById(page, applicationId)
    requireModifySecret(modifySecret)
  })

  test.afterAll(async ({ request }) => {
    if (lomakkeenAvain) {
      await poistaLomake(request, lomakkeenAvain)
    }
    await page.close()
  })

  test('näyttää hakemuksen muokkausnäkymässä', async () => {
    const secret = requireModifySecret(modifySecret)
    await page.goto(`/hakemus?modify=${secret}`)
    await expect(page.locator('.application__wrapper-element')).toHaveCount(1)
    await expect(getFormFields(page)).toHaveCount(32)
    await expect(page.locator('.application__header')).toHaveText('Testilomake')
    await expect(getSubmitButton(page)).toBeDisabled()
    await expect(page.getByTestId('ssn-input')).toHaveValue('***********')
    await expect(page.getByTestId('phone-input')).toHaveValue(
      seededValues.phone
    )
  })

  test('näyttää oikeat aiemmin täytetyt vastaukset', async () => {
    const secret = requireModifySecret(modifySecret)
    await page.goto(`/hakemus?modify=${secret}`)

    // Odotetaan, että postinumeron perusteella automaattisesti täyttyvä
    // postitoimipaikka on latautunut – merkki siitä, että lomake on valmis.
    await expect(page.getByTestId('postal-office-input')).toHaveValue(
      'JYVÄSKYLÄ'
    )

    await expect(page.getByTestId('first-name-input')).toHaveValue(
      seededValues.firstName
    )
    await expect(page.getByTestId('preferred-name-input')).toHaveValue(
      seededValues.preferredName
    )
    await expect(page.getByTestId('last-name-input')).toHaveValue(
      seededValues.lastName
    )
    await expect(page.getByTestId('ssn-input')).toHaveValue('***********')
    await expect(page.getByTestId('email-input')).toHaveValue(
      seededValues.email
    )
    await expect(page.getByTestId('verify-email-input')).toHaveValue(
      seededValues.email
    )
    await expect(page.getByTestId('phone-input')).toHaveValue(
      seededValues.phone
    )
    await expect(page.getByTestId('address-input')).toHaveValue(
      seededValues.address
    )
    await expect(page.getByTestId('postal-code-input')).toHaveValue(
      seededValues.postalCode
    )
    await expect(page.getByTestId('home-town-input')).toHaveValue('061')

    const extraFieldsToCheck = legacyExtraFieldDefinitions.filter(
      (field) => field.value.length > 0 && field.fieldType !== 'singleChoice'
    )
    for (const field of extraFieldsToCheck) {
      const input = page.getByRole('textbox', {
        name: new RegExp(field.label, 'i'),
      })
      await expect(input).toHaveValue(field.value)
    }

    // pw-f-13 on singleChoice ilman valittua vaihtoehtoa
    const pw13Field = page
      .locator('.application__form-field')
      .filter({ hasText: 'Kenttä 13' })
    await expect(pw13Field.locator('input[type="radio"]:checked')).toHaveCount(
      0
    )
  })

  test('muuttaa arvoja virheellisiksi ja näyttää virheet', async () => {
    const secret = requireModifySecret(modifySecret)
    await page.goto(`/hakemus?modify=${secret}`)

    await expect(page.getByTestId('phone-input')).toHaveValue(
      seededValues.phone
    )
    await expect(
      page.locator('.application__invalid-field-status-title')
    ).toHaveCount(0)

    await fillField(page, page.getByTestId('phone-input'), '420noscope')
    await page.getByTestId('phone-input').press('Tab')

    await expect(getSubmitButton(page)).toBeDisabled()
    await expect(
      page.locator('.application__invalid-field-status-title')
    ).toHaveCount(1)
    await expect(
      page.locator('.application__invalid-field-status-title')
    ).toContainText('Tarkista')

    await page.locator('.application__invalid-field-status-title').click()
    await expect(page.locator('.application__invalid-fields')).toContainText(
      'Matkapuhelin'
    )
  })

  test('muuttaa arvoja ja tallentaa hakemuksen', async () => {
    const secret = requireModifySecret(modifySecret)
    await page.goto(`/hakemus?modify=${secret}`)

    await expect(
      page.locator('.application__invalid-field-status-title')
    ).toHaveCount(0)

    editedPhoneNumber = Math.floor(Math.random() * 10000000).toString()
    await fillField(page, page.getByTestId('phone-input'), editedPhoneNumber)
    await page.getByTestId('phone-input').press('Tab')

    const editedField = page.getByRole('textbox', { name: /Kenttä 12/i })
    await fillField(page, editedField, 'Muokattu vastaus')
    await editedField.press('Tab')
    await expect(editedField).toHaveValue('Muokattu vastaus')

    await expect(
      page.locator('.application__invalid-field-status-title')
    ).toHaveCount(0)

    await expect(getSubmitButton(page)).toBeEnabled({ timeout: 30000 })
    await getSubmitButton(page).click()
    await expect(
      page.locator('.application__sent-placeholder-text')
    ).toBeVisible()

    const displayedValues = await page
      .locator('.application__text-field-paragraph')
      .allTextContents()
    expect(displayedValues).toContain(editedPhoneNumber)
    expect(displayedValues).toContain('Muokattu vastaus')
    expect(displayedValues).toContain('Tekstikentän vastaus')
    expect(displayedValues).toContain('Toistuva vastaus 1')
    expect(displayedValues).toContain('Toistuva vastaus 2')
    expect(displayedValues).toContain('Toistuva vastaus 3')
    expect(displayedValues).toContain('Pakollisen tekstialueen vastaus')
    expect(displayedValues).toContain('Kolmas vaihtoehto')
    expect(displayedValues).toContain('Jatkokysymyksen vastaus')
    expect(displayedValues).toContain('Lisensiaatin tutkinto')
    expect(displayedValues).toContain('Toinen vaihtoehto')
    expect(displayedValues).toContain('En')
    expect(displayedValues).toContain('Arkkitehti')
    expect(displayedValues).toContain('Pudotusvalikon 1. kysymys')
    expect(displayedValues).toContain('1,323')
    expect(displayedValues).toContain('Entinen Neuvostoliitto')
  })
})

test.describe('Hakijan hakemuksen muokkaus vahvasti tunnistautuneena', () => {
  let page: Page
  let lomakkeenAvain: string | null = null
  let lomakkeenId: number | null = null

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage()
    await kirjauduVirkailijanNakymaan(page)
    const lomake = await lisaaLomake(page)
    lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)
    lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)

    await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
      await page
        .getByTestId('form-name-input')
        .fill('Vahvasti tunnistautuneen testilomake')
    })
  })

  test.afterAll(async ({ request }) => {
    if (lomakkeenAvain) {
      await poistaLomake(request, lomakkeenAvain)
    }
    await page.close()
  })

  test('lukitsee kotikunnan hakijan muokkausnäkymässä kun haetaan vahvasti tunnistautuneena', async () => {
    const formKey = requireValue(lomakkeenAvain, 'Missing form key')
    const formId = requireValue(lomakkeenId, 'Missing form id')

    await allowHakeminenTunnistautuneena(page, formId)
    await loginWithStrongAuth(page, getHakijanNakymanOsoite(formKey))

    await fillField(
      page,
      page.getByTestId('email-input'),
      'auth-test@example.com'
    )
    await fillField(
      page,
      page.getByTestId('verify-email-input'),
      'auth-test@example.com'
    )
    await fillField(page, page.getByTestId('phone-input'), '0401234567')
    await fillField(page, page.getByTestId('address-input'), 'Testikatu 1 A 2')
    await fillField(page, page.getByTestId('postal-code-input'), '40100')
    await page.getByTestId('postal-code-input').press('Tab')
    await fillField(page, page.getByTestId('postal-office-input'), 'JYVÄSKYLÄ')
    await page
      .getByRole('combobox', { name: /Kansalaisuus/i })
      .selectOption({ label: 'Suomi' })
    await page.getByTestId('language-input').selectOption({ label: 'suomi' })

    await expect(getSubmitButton(page)).toBeEnabled()

    const [submitResponse] = await Promise.all([
      waitForResponse(page, 'POST', (url) =>
        url.includes(getHakemuksenLahettamisenOsoite())
      ),
      getSubmitButton(page).click(),
    ])
    await expect(
      page.locator('.application__sent-placeholder-text')
    ).toBeVisible()

    const submitPayload = (await submitResponse.json()) as { id?: number }
    const applicationId = requireValue(
      submitPayload.id,
      'Missing application id in authenticated hakija submit response'
    )

    const hakijaSecret = await getApplicationSecretById(page, applicationId)

    // Clear the strong-auth session to verify that locking is based on
    // the application's :tunnistautuminen key, not the current session.
    await page.context().clearCookies()

    await page.goto(`/hakemus?modify=${hakijaSecret}`)

    const homeTownInput = page.getByTestId('home-town-input')
    await expect(homeTownInput).toBeDisabled()
    await expect(homeTownInput).toHaveValue('853')
  })
})
