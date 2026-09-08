import { test, expect, Page } from '@playwright/test'
import {
  fillField,
  selectOption,
  unsafeFoldOption,
  waitForResponse,
} from '../playwright-utils'
import {
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  taytaHenkilotietomoduuli,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenId: number
let lomakkeenAvain: string

const formName = `Testilomake ${Date.now()}`

type FormNode = {
  id?: string
  fieldType?: string
  children?: FormNode[]
  module?: string
  [key: string]: unknown
}

const metadata = {
  'created-by': { name: 'system', oid: 'system', date: '1970-01-01T00:00:00Z' },
  'modified-by': {
    name: 'system',
    oid: 'system',
    date: '1970-01-01T00:00:00Z',
  },
}

// Vastaa vanhan mocha-testin (resources/spec/virkailijaEditorSpec.js) editorin
// kautta kokoamaa "Testilomake"-lomaketta. Rakennetaan suoraan API:n kautta
// injektoimalla, koska sisällön klikkaaminen kokoon editorissa jokaista
// tätä tarvitsevaa testiä varten olisi sekä hidasta että hauras.
const testilomakeFixture: FormNode[] = [
  {
    fieldClass: 'formField',
    fieldType: 'textField',
    id: 'tl-q1',
    label: { fi: 'Ensimmäinen kysymys' },
    metadata,
    params: {},
    validators: [],
  },
  {
    fieldClass: 'formField',
    fieldType: 'textField',
    id: 'tl-q1-repeat',
    label: { fi: 'Ensimmäinen kysymys, toistuvilla arvoilla' },
    metadata,
    params: { repeatable: true },
    validators: [],
  },
  {
    fieldClass: 'formField',
    fieldType: 'textArea',
    id: 'tl-q2',
    label: { fi: 'Toinen kysymys' },
    metadata,
    params: { 'max-length': '2000', size: 'L' },
    validators: ['required'],
  },
  {
    fieldClass: 'formField',
    fieldType: 'dropdown',
    id: 'tl-q3',
    label: { fi: 'Kolmas kysymys' },
    metadata,
    params: {},
    validators: [],
    options: [
      { value: '0', label: { fi: 'Ensimmäinen vaihtoehto' } },
      { value: '1', label: { fi: 'Toinen vaihtoehto' } },
      {
        value: '2',
        label: { fi: 'Kolmas vaihtoehto' },
        followups: [
          {
            fieldClass: 'formField',
            fieldType: 'textField',
            id: 'tl-q3-followup',
            label: { fi: 'Jatkokysymys' },
            metadata,
            params: {},
            validators: [],
          },
        ],
      },
    ],
  },
  {
    fieldClass: 'formField',
    fieldType: 'dropdown',
    id: 'tl-q4-koodisto',
    label: { fi: 'Neljäs kysymys' },
    metadata,
    params: {},
    validators: [],
    'koodisto-source': { uri: 'pohjakoulutuseditori', version: 1 },
  },
  {
    fieldClass: 'formField',
    fieldType: 'multipleChoice',
    id: 'tl-q5',
    label: { fi: 'Viides kysymys' },
    metadata,
    params: {},
    validators: [],
    options: [
      { value: '0', label: { fi: 'Ensimmäinen vaihtoehto' } },
      {
        value: '1',
        label: { fi: 'Toinen vaihtoehto' },
        followups: [
          {
            fieldClass: 'formField',
            fieldType: 'singleChoice',
            id: 'tl-q5-colorblind',
            label: { fi: 'Oletko punavihervärisokea?' },
            metadata,
            params: {},
            validators: ['required'],
            options: [
              { value: '0', label: { fi: 'Kyllä' } },
              { value: '1', label: { fi: 'En' } },
            ],
          },
          {
            fieldClass: 'wrapperElement',
            fieldType: 'adjacentfieldset',
            id: 'tl-q5-adjacent',
            label: {
              fi: 'Vierekkäinen tekstikenttä monivalinnan jatkokysymyksenä',
            },
            metadata,
            params: { repeatable: true },
            children: [
              {
                fieldClass: 'formField',
                fieldType: 'textField',
                id: 'tl-q5-adjacent-a',
                label: { fi: 'Jatkokysymys A' },
                metadata,
                params: { adjacent: true },
                validators: ['required'],
              },
              {
                fieldClass: 'formField',
                fieldType: 'textField',
                id: 'tl-q5-adjacent-b',
                label: { fi: 'Jatkokysymys B' },
                metadata,
                params: { adjacent: true },
                validators: [],
              },
              {
                fieldClass: 'formField',
                fieldType: 'textField',
                id: 'tl-q5-adjacent-c',
                label: { fi: 'Jatkokysymys C' },
                metadata,
                params: { adjacent: true },
                validators: ['required'],
              },
            ],
          },
        ],
      },
      { value: '2', label: { fi: 'Kolmas vaihtoehto' } },
    ],
  },
  {
    fieldClass: 'formField',
    fieldType: 'multipleChoice',
    id: 'tl-q6-koodisto',
    label: { fi: 'Kuudes kysymys' },
    metadata,
    params: {},
    validators: [],
    'koodisto-source': { uri: 'tutkinto', version: 2 },
  },
  {
    fieldClass: 'wrapperElement',
    fieldType: 'fieldset',
    id: 'tl-section-1',
    label: { fi: 'Testiosio' },
    metadata,
    params: {},
    children: [
      {
        fieldClass: 'formField',
        fieldType: 'textArea',
        id: 'tl-section-1-q',
        label: { fi: 'Osiokysymys' },
        metadata,
        params: { size: 'S' },
        validators: ['required'],
      },
    ],
  },
  {
    fieldClass: 'formField',
    fieldType: 'textField',
    id: 'tl-info',
    label: { fi: 'Infoteksti' },
    metadata,
    params: {},
    validators: [],
  },
  {
    fieldClass: 'formField',
    fieldType: 'dropdown',
    id: 'tl-last-koodisto',
    label: { fi: 'Viimeinen kysymys' },
    metadata,
    params: {},
    validators: [],
    'koodisto-source': { uri: 'tutkinto', version: 2 },
  },
  {
    fieldClass: 'wrapperElement',
    fieldType: 'fieldset',
    id: 'tl-section-2',
    label: { fi: 'Testiosio 2' },
    metadata,
    params: {},
    children: [
      {
        fieldClass: 'formField',
        fieldType: 'singleChoice',
        id: 'tl-section-2-q',
        label: { fi: 'Lyhyen listan kysymys' },
        metadata,
        params: {},
        validators: ['required'],
        options: [
          {
            value: '0',
            label: { fi: 'Ensimmäinen vaihtoehto' },
            followups: [
              {
                fieldClass: 'formField',
                fieldType: 'multipleChoice',
                id: 'tl-section-2-followup-mc',
                label: { fi: 'Monivalinta jatkokysymyksenä' },
                metadata,
                params: {},
                validators: ['required'],
                options: [
                  { value: '0', label: { fi: 'Jatkokysymys A' } },
                  { value: '1', label: { fi: 'Jatkokysymys B' } },
                ],
              },
              {
                fieldClass: 'wrapperElement',
                fieldType: 'adjacentfieldset',
                id: 'tl-section-2-followup-adj',
                label: {
                  fi: 'Vierekkäinen tekstikenttä painikkeiden jatkokysymyksenä',
                },
                metadata,
                params: { repeatable: true },
                children: [
                  {
                    fieldClass: 'formField',
                    fieldType: 'textField',
                    id: 'tl-section-2-adj-a',
                    label: { fi: 'Jatkokysymys A' },
                    metadata,
                    params: { adjacent: true },
                    validators: ['required'],
                  },
                  {
                    fieldClass: 'formField',
                    fieldType: 'textField',
                    id: 'tl-section-2-adj-b',
                    label: { fi: 'Jatkokysymys B' },
                    metadata,
                    params: { adjacent: true },
                    validators: [],
                  },
                  {
                    fieldClass: 'formField',
                    fieldType: 'textField',
                    id: 'tl-section-2-adj-c',
                    label: { fi: 'Jatkokysymys C' },
                    metadata,
                    params: { adjacent: true },
                    validators: ['required'],
                  },
                ],
              },
            ],
          },
          { value: '1', label: { fi: 'Toinen vaihtoehto' } },
        ],
      },
    ],
  },
  {
    fieldClass: 'wrapperElement',
    fieldType: 'adjacentfieldset',
    id: 'tl-adjacent-standalone',
    label: { fi: 'Vierekkäinen tekstikenttä' },
    metadata,
    params: {},
    children: [
      {
        fieldClass: 'formField',
        fieldType: 'textField',
        id: 'tl-adjacent-standalone-1',
        label: { fi: 'Tekstikenttä 1' },
        metadata,
        params: { adjacent: true },
        validators: [],
      },
      {
        fieldClass: 'formField',
        fieldType: 'textField',
        id: 'tl-adjacent-standalone-2',
        label: { fi: 'Tekstikenttä 2' },
        metadata,
        params: { adjacent: true },
        validators: [],
      },
    ],
  },
  {
    fieldClass: 'formField',
    fieldType: 'dropdown',
    id: 'tl-main-dropdown',
    label: { fi: 'Päätason pudotusvalikko' },
    metadata,
    params: {},
    validators: [],
    options: [
      {
        value: '0',
        label: { fi: 'Pudotusvalikon 1. kysymys' },
        followups: [
          {
            fieldClass: 'wrapperElement',
            fieldType: 'adjacentfieldset',
            id: 'tl-main-dropdown-adj',
            label: { fi: 'Vierekkäinen tekstikenttä jatkokysymyksenä' },
            metadata,
            params: { repeatable: true },
            children: [
              {
                fieldClass: 'formField',
                fieldType: 'textField',
                id: 'tl-main-dropdown-adj-a',
                label: { fi: 'Jatkokysymys A' },
                metadata,
                params: { adjacent: true },
                validators: ['required'],
              },
              {
                fieldClass: 'formField',
                fieldType: 'textField',
                id: 'tl-main-dropdown-adj-b',
                label: { fi: 'Jatkokysymys B' },
                metadata,
                params: { adjacent: true },
                validators: [],
              },
              {
                fieldClass: 'formField',
                fieldType: 'textField',
                id: 'tl-main-dropdown-adj-c',
                label: { fi: 'Jatkokysymys C' },
                metadata,
                params: { adjacent: true },
                validators: ['required'],
              },
            ],
          },
        ],
      },
      { value: '1', label: { fi: 'Pudotusvalikon 2. kysymys' } },
    ],
  },
  {
    fieldClass: 'formField',
    fieldType: 'textField',
    id: 'tl-numeric',
    label: { fi: 'Tekstikenttä numeerisilla arvoilla' },
    metadata,
    params: { numeric: true, decimals: 4 },
    validators: ['numeric'],
  },
  {
    fieldClass: 'formField',
    fieldType: 'dropdown',
    id: 'tl-expired-koodisto',
    label: { fi: 'Alasvetovalikko, koodisto, päättyneet' },
    metadata,
    params: {},
    validators: [],
    'koodisto-source': {
      uri: 'maatjavaltiot2',
      version: 2,
      'allow-invalid?': true,
    },
  },
]

const injectTestilomakeFormData = async (
  page: Page,
  formId: number
): Promise<void> => {
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
    throw new Error('Failed to build testilomake fixture content')
  }

  const formWithoutTimestamp = { ...form }
  delete formWithoutTimestamp['created-time']
  const updatedForm = {
    ...formWithoutTimestamp,
    name: { fi: formName },
    content: [hakukohteet, personInfoModule, ...testilomakeFixture],
  }

  const postResponse = await page.request.post('/lomake-editori/api/forms', {
    data: updatedForm,
  })

  if (!postResponse.ok()) {
    throw new Error(
      `Failed to persist form ${formId}: ${postResponse.status()} ${await postResponse.text()}`
    )
  }
}

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)

  const lomake = await lisaaLomake(page)
  lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  await injectTestilomakeFormData(page, lomakkeenId)

  await Promise.all([
    page.goto(getHakijanNakymanOsoite(lomakkeenAvain)),
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenAvain))
    ),
  ])
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

const getFormFields = () =>
  page.locator('.application__form-content-area .application__form-field')

const getInvalidFieldsStatus = () =>
  page.locator('.application__invalid-field-status-title')

// Klikkauksen jälkeen odotetaan hetki, jotta mahdollisesti paljastuva
// jatkokysymys ehtii renderöityä ennen kuin seuraava askel kysyy formFieldsin
// senhetkistä lukumäärää (ks. myös fillField/selectOption playwright-utils.ts:ssä).
const clickFieldRadio = async (
  field: ReturnType<typeof getFormFields>,
  label: string
) => {
  await field.getByText(label, { exact: true }).click()
  // eslint-disable-next-line playwright/no-wait-for-timeout
  await page.waitForTimeout(50)
}

test('lomake latautuu täydellisenä', async () => {
  await expect(getFormFields()).toHaveCount(29)
  await expect(page.getByTestId('send-application-button')).toBeDisabled()
  await expect(page.getByTestId('application-header-label')).toHaveText(
    formName
  )
  await expect(getInvalidFieldsStatus()).toHaveText('Tarkista 13 tietoa')
  await expect(
    getFormFields().nth(16).locator('.application__form-textarea-max-length')
  ).toHaveText('0 / 2000')
})

test('henkilötietomoduulin täyttäminen', async () => {
  await taytaHenkilotietomoduuli(page, {
    'first-name': 'Etunimi Tokanimi',
    'last-name': 'Sukunimi',
    ssn: '020202A0202',
    email: 'test@example.com',
    'verify-email': 'test@example.com',
    phone: '0123456789',
    address: 'Katutie 12 B',
    'postal-code': '40100',
    'home-town': '179',
  })

  await expect(getInvalidFieldsStatus()).toHaveText('Tarkista 3 tietoa')
})

test('käyttäjän määrittelemien kenttien täyttäminen ja lähettäminen', async () => {
  await fillField(
    page,
    getFormFields().nth(14).locator('input'),
    'Tekstikentän vastaus'
  )

  const repeatable = getFormFields().nth(15)
  await fillField(
    page,
    repeatable.locator('input').nth(0),
    'Toistuva vastaus 1'
  )
  await fillField(
    page,
    repeatable.locator('input').nth(1),
    'Toistuva vastaus 2'
  )
  await fillField(
    page,
    repeatable.locator('input').nth(2),
    'Toistuva vastaus 3'
  )

  await fillField(
    page,
    getFormFields().nth(16).locator('textarea'),
    'Pakollisen tekstialueen vastaus'
  )

  await selectOption(page, getFormFields().nth(17).locator('select'), '2')
  await fillField(
    page,
    getFormFields().nth(18).locator('input'),
    'Jatkokysymyksen vastaus'
  )

  await selectOption(page, getFormFields().nth(19).locator('select'), '120')

  await clickFieldRadio(getFormFields().nth(20), 'Toinen vaihtoehto')
  await clickFieldRadio(getFormFields().nth(21), 'En')

  // Kolmisarakkeinen toistuva vierekkäiskenttäryhmä (A/B/C, joista A ja C
  // pakollisia): ensimmäinen rivi täytetään kokonaan, toiselta riviltä
  // jätetään keskimmäinen (B) tarkoituksella tyhjäksi, koska se ei ole
  // pakollinen. Rivin C-sarake täytetään vasta "Lisää rivi" -klikkauksen
  // jälkeen, koska aiemmin täytetyt arvot saattavat kadota rivilisäyksen
  // aiheuttamassa uudelleenrenderöinnissä (ks. myös
  // fillAndSubmitQuestionGroupApplication-funktion tekstialue-kommentti).
  const adjacentMultiRakenne = async (
    field: ReturnType<typeof getFormFields>,
    row0: [string, string, string],
    row1AC: [string, string]
  ) => {
    await fillField(page, field.getByRole('textbox').nth(0), row0[0])
    await fillField(page, field.getByRole('textbox').nth(1), row0[1])
    await field.locator('.application__form-add-new-row').click()
    await expect(field.getByRole('textbox')).toHaveCount(6)
    await fillField(page, field.getByRole('textbox').nth(2), row0[2])
    await fillField(page, field.getByRole('textbox').nth(3), row1AC[0])
    await fillField(page, field.getByRole('textbox').nth(5), row1AC[1])
  }

  await adjacentMultiRakenne(
    getFormFields().nth(22),
    ['A1', 'B1', 'C1'],
    ['A2', 'C2']
  )

  await clickFieldRadio(getFormFields().nth(23), 'Arkkitehti')

  await fillField(
    page,
    getFormFields().nth(24).locator('textarea'),
    'Toisen pakollisen tekstialueen vastaus'
  )

  await clickFieldRadio(getFormFields().nth(27), 'Ensimmäinen vaihtoehto')
  await clickFieldRadio(getFormFields().nth(28), 'Jatkokysymys A')
  await clickFieldRadio(getFormFields().nth(28), 'Jatkokysymys B')

  await adjacentMultiRakenne(
    getFormFields().nth(29),
    ['A1', 'B1', 'C1'],
    ['A2', 'C2']
  )

  const adjacentSingle = getFormFields().nth(30)
  await fillField(
    page,
    adjacentSingle.getByRole('textbox').nth(0),
    'Vasen vierekkäinen'
  )
  await fillField(
    page,
    adjacentSingle.getByRole('textbox').nth(1),
    'Oikea vierekkäinen'
  )

  await selectOption(page, getFormFields().nth(31).locator('select'), '0')

  await adjacentMultiRakenne(
    getFormFields().nth(32),
    ['A1', 'B1', 'C1'],
    ['A2', 'C2']
  )

  await fillField(page, getFormFields().nth(33).locator('input'), '1,323')

  await selectOption(page, getFormFields().nth(34).locator('select'), '810')

  // TILAPÄINEN DIAGNOSTIIKKA: näyttää virheessä, mitkä kentät jäivät
  // virheellisiksi, jos lähetä-painike ei aktivoidu.
  if (await getInvalidFieldsStatus().count()) {
    await getInvalidFieldsStatus().click()
    await expect(page.locator('.application__invalid-fields a div')).toHaveText(
      []
    )
  }

  await expect(page.getByTestId('send-application-button')).toBeEnabled()

  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes('/hakemus/api/application')
    ),
    page.getByTestId('send-application-button').click(),
  ])

  await expect(
    page.locator('.application__sent-placeholder-text')
  ).toBeVisible()

  const readonlyAnswer = page.locator('.application__text-field-paragraph')
  const expectedValues = [
    'Etunimi Tokanimi',
    'Etunimi',
    'Sukunimi',
    'Suomi',
    '020202A0202',
    'test@example.com',
    '0123456789',
    'Suomi',
    'Katutie 12 B',
    '40100',
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
    'Toisen pakollisen tekstialueen vastaus',
    '',
    'Ensimmäinen vaihtoehto',
    'Jatkokysymys A',
    'Jatkokysymys B',
    'Pudotusvalikon 1. kysymys',
    '1,323',
    'Entinen Neuvostoliitto',
  ]
  await expect(readonlyAnswer).toHaveCount(expectedValues.length)
  await expect(readonlyAnswer).toHaveText(expectedValues)

  const tabularValue = page.locator('.application__form-field table td')
  const expectedTabularValues = [
    'A1',
    'B1',
    'C1',
    'A2',
    '',
    'C2',
    'A1',
    'B1',
    'C1',
    'A2',
    '',
    'C2',
    'Vasen vierekkäinen',
    'Oikea vierekkäinen',
    'A1',
    'B1',
    'C1',
    'A2',
    '',
    'C2',
  ]
  await expect(tabularValue).toHaveCount(expectedTabularValues.length)
  await expect(tabularValue).toHaveText(expectedTabularValues)
})
