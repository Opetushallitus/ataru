import { expect, Page, test } from '@playwright/test'
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

  const updatedForm = {
    ...form,
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

const getLatestApplicationSecret = async (page: Page): Promise<string> => {
  const response = await page.request.get('/hakemus/latest-application-secret')
  if (!response.ok()) {
    throw new Error('Failed to fetch latest application secret')
  }

  return (await response.text()).trim().replace(/^"|"$/g, '')
}

const getApplicationKeyBySecret = async (
  page: Page,
  applicationSecret: string
): Promise<string> => {
  const response = await page.request.get(
    `/hakemus/api/application?secret=${applicationSecret}`
  )
  if (!response.ok()) {
    throw new Error('Failed to fetch application by secret')
  }

  const body = (await response.json()) as {
    application?: { key?: string }
  }
  const key = body.application?.key
  if (!key) {
    throw new Error('Application key missing in application response')
  }

  return key
}

const getVirkailijaSecretByApplicationKey = async (
  page: Page,
  applicationKey: string
): Promise<string> => {
  const response = await page.request.get(
    `/lomake-editori/api/applications/${applicationKey}/modify`,
    { maxRedirects: 0 }
  )
  if (!(response.status() >= 300 && response.status() < 400)) {
    throw new Error('Expected modify endpoint redirect response')
  }

  const location = response.headers()['location']
  if (!location) {
    throw new Error('Missing Location header from modify endpoint')
  }

  const secret = new URL(location, 'http://localhost:8354').searchParams.get(
    'virkailija-secret'
  )
  if (!secret) {
    throw new Error('Missing virkailija-secret in modify redirect URL')
  }

  return secret
}

const getFormFields = (page: Page) =>
  page.locator('.application__form-content-area .application__form-field')

const getSubmitButton = (page: Page) =>
  page.getByTestId('send-application-button')

const requireValue = <T>(value: T | null, message: string): T => {
  if (value === null) {
    throw new Error(message)
  }

  return value
}

const requireVirkailijaSecret = (secret: string | null): string => {
  if (!secret) {
    throw new Error(
      'Missing virkailija update secret after bootstrap application creation'
    )
  }

  return secret
}

test.describe('Virkailijan hakemuksen muokkaus', () => {
  let page: Page
  let lomakkeenAvain: string | null = null
  let virkailijaSecret: string | null
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
    homeTown: '179',
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

    // Create the baseline submitted application so virkailija edit secret can be generated from latest Testilomake application.
    const formKey = requireValue(
      lomakkeenAvain,
      'Missing created form key for bootstrap application'
    )

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

    // Build virkailija edit secret for the exact application created in this bootstrap.

    let applicationKey: string | null = null
    let applicationSecret: string | null = null

    try {
      const payload = (await submitResponse.json()) as {
        key?: string
        secret?: string
        application?: { key?: string; secret?: string }
      }
      applicationKey = payload.key ?? payload.application?.key ?? null
      applicationSecret = payload.secret ?? payload.application?.secret ?? null
    } catch {
      // ignore JSON parsing errors and try fallback below
    }

    if (!applicationKey && applicationSecret) {
      applicationKey = await getApplicationKeyBySecret(page, applicationSecret)
    }

    if (!applicationKey) {
      const latestSecret = await getLatestApplicationSecret(page)
      applicationKey = await getApplicationKeyBySecret(page, latestSecret)
    }

    virkailijaSecret = await getVirkailijaSecretByApplicationKey(
      page,
      applicationKey
    )
    requireVirkailijaSecret(virkailijaSecret)
  })

  test.afterAll(async ({ request }) => {
    if (lomakkeenAvain) {
      await poistaLomake(request, lomakkeenAvain)
    }
    await page.close()
  })

  test('näyttää hakemuksen salaisella avaimella', async () => {
    const secret = requireVirkailijaSecret(virkailijaSecret)
    await page.goto(`/hakemus?virkailija-secret=${secret}`)
    await expect(page.locator('.application__wrapper-element')).toHaveCount(1)
    await expect(getFormFields(page)).toHaveCount(32)
    await expect(page.locator('.application__header')).toHaveText('Testilomake')
    await expect(getSubmitButton(page)).toBeDisabled()
  })

  test('muuttaa arvoja ja tallentaa hakemuksen', async () => {
    const secret = requireVirkailijaSecret(virkailijaSecret)
    await page.goto(`/hakemus?virkailija-secret=${secret}`)

    await expect(
      page.locator('.application__invalid-field-status-title')
    ).toHaveCount(0)

    editedPhoneNumber = Math.floor(Math.random() * 10000000).toString()
    await fillField(page, page.getByTestId('phone-input'), editedPhoneNumber)
    await page.getByTestId('phone-input').press('Tab')

    const editedField = page.getByRole('textbox', {
      name: /Kenttä 12/i,
    })
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
    const displayedValuesWithLegacyEmptySlot = [
      ...displayedValues.slice(0, 25),
      '',
      ...displayedValues.slice(25),
    ]
    const expectedValues = [
      seededValues.firstName,
      seededValues.preferredName,
      seededValues.lastName,
      'Suomi',
      seededValues.ssn,
      seededValues.email,
      editedPhoneNumber,
      'Suomi',
      seededValues.address,
      seededValues.postalCode,
      'JYVÄSKYLÄ',
      'Jyväskylä',
      'suomi',
      'Tekstikentän vastaus',
      'Toistuva vastaus 1',
      'Toistuva vastaus 2',
      'Toistuva vastaus 3',
      'Pakollisen tekstialueen vastaus',
      'Kolmas vaihtoehto',
      'Jatkokysymyksen vastaus',
      'Lisensiaatin tutkinto',
      'Toinen vaihtoehto',
      'En',
      'Arkkitehti',
      'Muokattu vastaus',
      '',
      'Toinen vaihtoehto',
      'Pudotusvalikon 1. kysymys',
      '1,323',
      'Entinen Neuvostoliitto',
    ]

    expect(displayedValuesWithLegacyEmptySlot).toEqual(expectedValues)
  })

  test('näyttää virheen virheellisellä avaimella', async () => {
    const secret = requireVirkailijaSecret(virkailijaSecret)
    await page.goto(`/hakemus?virkailija-secret=${secret}-invalid`)
    const message = page.locator('.application__message-display')
    await expect(message).toBeVisible()
    await expect(message).toContainText('vanhentunut')
  })
})
