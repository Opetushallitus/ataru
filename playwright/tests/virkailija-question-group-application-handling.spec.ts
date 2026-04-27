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

const formName = `Kysymysryhmä: testilomake (PW) ${Date.now()}`

type FormNode = {
  id?: string
  fieldType?: string
  children?: FormNode[]
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

const questionGroupFixture: FormNode = {
  fieldClass: 'questionGroup',
  fieldType: 'fieldset',
  id: 'pw-question-group',
  label: { fi: 'Kysymysryhmä: ryhman otsikko' },
  metadata: systemMetadata,
  params: {},
  children: [
    {
      fieldClass: 'formField',
      fieldType: 'dropdown',
      id: 'pw-qg-dropdown',
      label: { fi: 'Kysymysryhmä: pudotusvalikko' },
      metadata: systemMetadata,
      params: {},
      validators: [],
      options: [
        { value: '0', label: { fi: 'Pudotusvalikko: A' } },
        { value: '1', label: { fi: 'Pudotusvalikko: B' } },
      ],
    },
    {
      fieldClass: 'formField',
      fieldType: 'singleChoice',
      id: 'pw-qg-single-choice',
      label: { fi: 'Kysymysryhmä: painikkeet, yksi valittavissa' },
      metadata: systemMetadata,
      params: {},
      validators: [],
      options: [
        { value: '0', label: { fi: 'Painikkeet, yksi valittavissa: A' } },
        { value: '1', label: { fi: 'Painikkeet, yksi valittavissa: B' } },
      ],
    },
    {
      fieldClass: 'formField',
      fieldType: 'multipleChoice',
      id: 'pw-qg-multi-choice',
      label: { fi: 'Kysymysryhmä: lista, monta valittavissa' },
      metadata: systemMetadata,
      params: {},
      validators: [],
      options: [
        { value: '0', label: { fi: 'Lista, monta valittavissa: A' } },
        { value: '1', label: { fi: 'Lista, monta valittavissa: B' } },
      ],
    },
    {
      fieldClass: 'formField',
      fieldType: 'textField',
      id: 'pw-qg-text-single',
      label: { fi: 'Tekstikentta, yksi vastaus' },
      metadata: systemMetadata,
      params: {},
      validators: [],
    },
    {
      fieldClass: 'formField',
      fieldType: 'textField',
      id: 'pw-qg-text-multi',
      label: { fi: 'Tekstikentta, monta vastausta' },
      metadata: systemMetadata,
      params: { repeatable: true },
      validators: [],
    },
    {
      fieldClass: 'formField',
      fieldType: 'textArea',
      id: 'pw-qg-textarea',
      label: { fi: 'Tekstialue' },
      metadata: systemMetadata,
      params: {},
      validators: [],
    },
    {
      fieldClass: 'wrapperElement',
      fieldType: 'adjacentfieldset',
      id: 'pw-qg-adj-single',
      label: { fi: 'Vierekkaiset tekstikentat' },
      metadata: systemMetadata,
      params: {},
      children: [
        {
          fieldClass: 'formField',
          fieldType: 'textField',
          id: 'pw-qg-adj-single-a',
          label: { fi: 'Vierekkaiset tekstikentat, yksi vastaus: A' },
          metadata: systemMetadata,
          params: { adjacent: true },
          validators: [],
        },
        {
          fieldClass: 'formField',
          fieldType: 'textField',
          id: 'pw-qg-adj-single-b',
          label: { fi: 'Vierekkaiset tekstikentat, yksi vastaus: B' },
          metadata: systemMetadata,
          params: { adjacent: true },
          validators: [],
        },
      ],
    },
    {
      fieldClass: 'wrapperElement',
      fieldType: 'adjacentfieldset',
      id: 'pw-qg-adj-multi',
      label: { fi: 'Vierekkaiset tekstikentat' },
      metadata: systemMetadata,
      params: { repeatable: true },
      children: [
        {
          fieldClass: 'formField',
          fieldType: 'textField',
          id: 'pw-qg-adj-multi-a',
          label: { fi: 'Vierekkaiset tekstikentat, monta vastausta: A' },
          metadata: systemMetadata,
          params: { adjacent: true },
          validators: [],
        },
        {
          fieldClass: 'formField',
          fieldType: 'textField',
          id: 'pw-qg-adj-multi-b',
          label: { fi: 'Vierekkaiset tekstikentat, monta vastausta: B' },
          metadata: systemMetadata,
          params: { adjacent: true },
          validators: [],
        },
      ],
    },
  ],
}

const mainLevelDropdownFixture: FormNode = {
  fieldClass: 'formField',
  fieldType: 'dropdown',
  id: 'pw-main-level-dropdown',
  label: { fi: 'Päätaso: pudotusvalikko' },
  metadata: systemMetadata,
  params: {},
  validators: [],
  options: [
    { value: '0', label: { fi: 'Päätaso: A' }, followups: [] },
    {
      value: '1',
      label: { fi: 'Päätaso: B' },
      followups: [questionGroupFixture],
    },
  ],
}

const injectQuestionGroupFormData = async (page: Page, formId: number) => {
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
    throw new Error('Failed to build question group fixture content')
  }

  const updatedForm = {
    ...form,
    name: { fi: formName },
    content: [hakukohteet, personInfoModule, mainLevelDropdownFixture],
  }

  const postResponse = await page.request.post('/lomake-editori/api/forms', {
    data: updatedForm,
  })

  if (!postResponse.ok()) {
    throw new Error(`Failed to persist form ${formId}`)
  }
}

const fillAndSubmitApplication = async (page: Page, formKey: string) => {
  const formFields = page.locator(
    '.application__form-content-area .application__form-field'
  )

  const selectNth = async (fieldIndex: number, value: string) => {
    await selectOption(
      page,
      formFields.nth(fieldIndex).locator('select').first(),
      value
    )
  }

  await Promise.all([
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(formKey))
    ),
    page.goto(getHakijanNakymanOsoite(formKey)),
  ])

  await expect(page.getByTestId('application-header-label')).toHaveText(
    formName
  )

  await fillField(
    page,
    page.getByTestId('first-name-input'),
    'Etunimi Tokanimi'
  )
  await page.getByTestId('first-name-input').press('Tab')
  await expect(page.getByTestId('preferred-name-input')).toHaveValue('Etunimi')

  await fillField(page, page.getByTestId('last-name-input'), 'Sukunimi')
  await fillField(page, page.getByTestId('ssn-input'), '020202A0202')
  await fillField(page, page.getByTestId('email-input'), 'test@example.com')
  await fillField(
    page,
    page.getByTestId('verify-email-input'),
    'test@example.com'
  )
  await fillField(page, page.getByTestId('phone-input'), '050123')
  await fillField(page, page.getByTestId('address-input'), 'Katutie 12 B')
  await fillField(page, page.getByTestId('postal-code-input'), '40100')
  await expect(page.getByTestId('postal-office-input')).toHaveValue(
    /JYV.*SKYL.*/
  )
  await selectOption(page, page.getByTestId('home-town-input'), '179')

  await selectNth(14, '1')
  await page
    .locator(
      '.application__form-dropdown-followups .application__add-question-group-row a'
    )
    .first()
    .click()

  const questionGroupRows = page.locator(
    '.application__form-dropdown-followups .application__question-group-row'
  )
  await expect(questionGroupRows).toHaveCount(2)

  const row0 = questionGroupRows.nth(0)
  const row1 = questionGroupRows.nth(1)

  // Dropdown
  await selectOption(
    page,
    row0.getByRole('combobox', { name: /Kysymysryhmä: pudotusvalikko/i }),
    '0'
  )
  await selectOption(
    page,
    row1.getByRole('combobox', { name: /Kysymysryhmä: pudotusvalikko/i }),
    '1'
  )

  // Single choice
  await row0
    .getByRole('radio', { name: /Painikkeet, yksi valittavissa: A/i })
    .click()
  await row1
    .getByRole('radio', { name: /Painikkeet, yksi valittavissa: B/i })
    .click()

  // Multiple choice
  await row0
    .locator('label', { hasText: 'Lista, monta valittavissa: A' })
    .click()
  await row0
    .locator('label', { hasText: 'Lista, monta valittavissa: B' })
    .click()
  await row1
    .locator('label', { hasText: 'Lista, monta valittavissa: B' })
    .click()

  // Single-answer text field
  const row0SingleText = row0
    .getByRole('textbox', { name: /Tekstikentt.*yksi vastaus/i })
    .first()
  const row1SingleText = row1
    .getByRole('textbox', { name: /Tekstikentt.*yksi vastaus/i })
    .first()
  await fillField(page, row0SingleText, 'Tekstikenttä, yksi vastaus: A')
  await expect(row0SingleText).toHaveValue('Tekstikenttä, yksi vastaus: A')
  await fillField(page, row1SingleText, 'Tekstikenttä, yksi vastaus: B')
  await expect(row1SingleText).toHaveValue('Tekstikenttä, yksi vastaus: B')

  // Multi-answer text field
  const row0MultiText = row0.getByRole('textbox', {
    name: /Tekstikentt.*monta vastausta/i,
  })
  const row1MultiText = row1.getByRole('textbox', {
    name: /Tekstikentt.*monta vastausta/i,
  })
  await fillField(
    page,
    row0MultiText.nth(0),
    'Tekstikenttä, monta vastausta: A'
  )
  await row0MultiText.nth(0).press('Tab')
  await expect(row0MultiText.nth(1)).toBeEnabled()
  await fillField(
    page,
    row0MultiText.nth(1),
    'Tekstikenttä, monta vastausta: B'
  )
  await fillField(
    page,
    row1MultiText.nth(0),
    'Tekstikenttä, monta vastausta: C'
  )
  await row1MultiText.nth(0).press('Tab')
  await expect(row1MultiText.nth(1)).toBeEnabled()
  await fillField(
    page,
    row1MultiText.nth(1),
    'Tekstikenttä, monta vastausta: D'
  )

  // Textareas
  const row0TextArea = row0.getByRole('textbox', { name: /Tekstialue/i })
  const row1TextArea = row1.getByRole('textbox', { name: /Tekstialue/i })
  await row0TextArea.fill('Tekstialue: AAAAA')
  await row0TextArea.press('Tab')
  await expect(row0TextArea).toHaveValue('Tekstialue: AAAAA')
  await row1TextArea.fill('Tekstialue: BBBBB')
  await row1TextArea.press('Tab')
  await expect(row1TextArea).toHaveValue('Tekstialue: BBBBB')

  // Adjacent single-answer fields
  const row0AdjSingle = row0
    .locator('.application__form-adjacent-text-fields-wrapper')
    .nth(0)
    .getByRole('textbox')
  const row1AdjSingle = row1
    .locator('.application__form-adjacent-text-fields-wrapper')
    .nth(0)
    .getByRole('textbox')
  await fillField(
    page,
    row0AdjSingle.nth(0),
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus A'
  )
  await fillField(
    page,
    row0AdjSingle.nth(1),
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus B'
  )
  await fillField(
    page,
    row1AdjSingle.nth(0),
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus C'
  )
  await fillField(
    page,
    row1AdjSingle.nth(1),
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus D'
  )

  // Adjacent multi-answer fields
  const row0AdjMultiField = row0
    .locator('.application__form-field')
    .filter({ hasText: 'Vierekkaiset tekstikentat, monta vastausta: A' })
    .first()
  const row1AdjMultiField = row1
    .locator('.application__form-field')
    .filter({ hasText: 'Vierekkaiset tekstikentat, monta vastausta: A' })
    .first()
  const row0AdjMulti = row0AdjMultiField.getByRole('textbox')
  const row1AdjMulti = row1AdjMultiField.getByRole('textbox')
  await fillField(
    page,
    row0AdjMulti.nth(0),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus A1'
  )
  await fillField(
    page,
    row0AdjMulti.nth(1),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus B1'
  )
  await row0AdjMultiField.getByText('Lisää rivi').click()
  await expect(row0AdjMulti).toHaveCount(4)
  await fillField(
    page,
    row0AdjMulti.nth(2),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus A2'
  )
  await fillField(
    page,
    row0AdjMulti.nth(3),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus B2'
  )

  await fillField(
    page,
    row1AdjMulti.nth(0),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus C1'
  )
  await fillField(
    page,
    row1AdjMulti.nth(1),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus D1'
  )
  await row1AdjMultiField.getByText('Lisää rivi').click()
  await expect(row1AdjMulti).toHaveCount(4)
  await fillField(
    page,
    row1AdjMulti.nth(2),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus C2'
  )
  await fillField(
    page,
    row1AdjMulti.nth(3),
    'Vierekkäiset tekstikentät, monta vastausta: vastaus D2'
  )

  // Dynamic row operations can rerender question-group inputs, so enforce textarea values once more before submit.
  await row0TextArea.fill('Tekstialue: AAAAA')
  await row0TextArea.press('Tab')
  await expect(row0TextArea).toHaveValue('Tekstialue: AAAAA')
  await row1TextArea.fill('Tekstialue: BBBBB')
  await row1TextArea.press('Tab')
  await expect(row1TextArea).toHaveValue('Tekstialue: BBBBB')

  await expect(page.getByTestId('send-application-button')).toBeEnabled()

  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes(getHakemuksenLahettamisenOsoite())
    ),
    page.getByTestId('send-application-button').click(),
  ])

  await expect(page.getByText(/Hakemus/)).toBeVisible()
}

test.describe('Virkailijan hakemuksen käsittely lomakkeella, jossa on kysymysryhmä', () => {
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

    await injectQuestionGroupFormData(page, lomakkeenTunnisteet.lomakkeenId)
    await fillAndSubmitApplication(page, lomakkeenTunnisteet.lomakkeenAvain)

    await page.goto(
      `/lomake-editori/applications/${lomakkeenTunnisteet.lomakkeenAvain}?ensisijaisesti=false`
    )
    await page.getByTestId('show-results').click()
    await expect(page.locator('.application__wrapper-heading h2')).toBeVisible()
  })

  test.afterAll(async ({ request }) => {
    if (lomakkeenTunnisteet?.lomakkeenAvain) {
      await poistaLomake(request, lomakkeenTunnisteet.lomakkeenAvain)
    }
    await page.close()
  })

  test('näyttää automaattisesti ainoan lomakkeeseen kuuluvan hakemuksen', async () => {
    const readonlyAnswers = await page
      .locator('.application__text-field-paragraph')
      .allTextContents()

    // Keep person-info expectations strict in order and value.
    const expectedPersonInfoPrefix = [
      'Etunimi Tokanimi',
      'Etunimi',
      'Sukunimi',
      'Suomi',
      '020202A0202',
      '02.02.2002',
      'nainen',
      'test@example.com',
      '050123',
      'Suomi',
      'Katutie 12 B',
      '40100',
    ]

    expect(readonlyAnswers.slice(0, expectedPersonInfoPrefix.length)).toEqual(
      expectedPersonInfoPrefix
    )

    const expectedQuestionGroupValues = [
      'Pudotusvalikko: A',
      'Painikkeet, yksi valittavissa: A',
      'Lista, monta valittavissa: A',
      'Lista, monta valittavissa: B',
      'Tekstikenttä, yksi vastaus: A',
      'Tekstikenttä, monta vastausta: A',
      'Tekstikenttä, monta vastausta: B',
      'Pudotusvalikko: B',
      'Painikkeet, yksi valittavissa: B',
      'Lista, monta valittavissa: B',
      'Tekstikenttä, yksi vastaus: B',
      'Tekstikenttä, monta vastausta: C',
      'Tekstikenttä, monta vastausta: D',
    ]

    expect(readonlyAnswers.some((value) => /Päätaso: B/.test(value))).toBe(true)
    for (const value of expectedQuestionGroupValues) {
      expect(readonlyAnswers).toContain(value)
    }

    expect(readonlyAnswers).toEqual(
      expect.arrayContaining([
        'Etunimi Tokanimi',
        'Etunimi',
        'Sukunimi',
        'Suomi',
        '020202A0202',
        '02.02.2002',
        'nainen',
        'test@example.com',
        '050123',
        'Katutie 12 B',
        '40100',
      ])
    )

    // Locale/casing can vary by environment, but both values should be present.
    expect(readonlyAnswers.some((value) => /JYVÄSKYLÄ/.test(value))).toBe(true)
    expect(readonlyAnswers.some((value) => /Jyväskylä/.test(value))).toBe(true)
    expect(readonlyAnswers.some((value) => /suomi/i.test(value))).toBe(true)

    const adjacentAnswerAt = async (
      formFieldIndex: number,
      answerIndex: number
    ) =>
      page
        .locator('.application__readonly-adjacent')
        .nth(formFieldIndex)
        .locator('td')
        .nth(answerIndex)
        .innerText()

    await expect(async () => {
      expect(await adjacentAnswerAt(0, 0)).toBe(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus A'
      )
      expect(await adjacentAnswerAt(0, 1)).toBe(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus B'
      )
      expect(await adjacentAnswerAt(1, 0)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus A1'
      )
      expect(await adjacentAnswerAt(1, 1)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus B1'
      )
      expect(await adjacentAnswerAt(1, 2)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus A2'
      )
      expect(await adjacentAnswerAt(1, 3)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus B2'
      )
      expect(await adjacentAnswerAt(2, 0)).toBe(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus C'
      )
      expect(await adjacentAnswerAt(2, 1)).toBe(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus D'
      )
      expect(await adjacentAnswerAt(3, 0)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus C1'
      )
      expect(await adjacentAnswerAt(3, 1)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus D1'
      )
      expect(await adjacentAnswerAt(3, 2)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus C2'
      )
      expect(await adjacentAnswerAt(3, 3)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus D2'
      )
    }).toPass()
  })

  test('näyttää täydennyspyynnön lomakkeen käyttäjälle', async () => {
    await page
      .locator(
        '.application-handling__review-state-container-processing-state .application-handling__review-state-row--selected'
      )
      .click()

    const informationRequestStateButton = page
      .locator('.application-handling__review-state-row')
      .filter({ hasText: /Täydennyspyyntö/ })
    await expect(informationRequestStateButton).toBeVisible()
    await informationRequestStateButton.click()

    const informationRequestContainer = page.locator(
      '.application-handling__information-request-container'
    )
    const submitInformationRequestButton = informationRequestContainer.locator(
      '.application-handling__send-information-request-button'
    )

    await expect(submitInformationRequestButton).toBeDisabled()

    await informationRequestContainer
      .locator('.application-handling__information-request-text-input')
      .fill('Täydennyspyyntö: otsikko')

    await expect(submitInformationRequestButton).toBeDisabled()

    await informationRequestContainer
      .locator('.application-handling__information-request-message-area')
      .fill('Täydennyspyyntö: viesti')

    await expect(submitInformationRequestButton).toBeEnabled()

    await Promise.all([
      waitForResponse(page, 'POST', (url) =>
        url.includes('/lomake-editori/api/applications/information-request')
      ),
      submitInformationRequestButton.click(),
    ])

    await expect(
      page.locator('.application-handling__information-request-submitted-text')
    ).toBeVisible()

    await expect(
      page.locator(
        '.application-handling__information-request-show-container-link'
      )
    ).toBeVisible()
  })
})
