const testFormApplicationSecret = '{{test-form-application-secret}}'
const virkailijaSecret = '{{virkailija-secret}}'
const virkailijaCreateSecret = '{{virkailija-create-secret}}'

const newForm = formName => {
  const testFormKey = '{{test-form-key}}'
  const testQuestionGroupFormKey = '{{test-question-group-form-key}}'
  const testSelectionLimitFormKey = '{{test-selection-limit-form-key}}'
  const ssnFormKey = '{{ssn-form-key}}'
  let formKey

  switch (formName) {
    case 'testForm':
      formKey = testFormKey
      break
    case 'testQuestionGroupForm':
      formKey = testQuestionGroupFormKey
      break
    case 'testSelectionLimitForm':
      formKey = testSelectionLimitFormKey
      break
    case 'ssnTestForm':
      formKey = ssnFormKey
      break
    default:
      console.log('No valid test form key found! Test will fail.. :(')
  }

  if (!formKey) {
    console.log(
      'Test form key undefined (no form found). Did you run virkailija test first?'
    )
  } else {
    console.log('form key', formKey)
    return () => {
      loadInFrame('/hakemus/' + formKey)
    }
  }
}

const formHeader = () => testFrame().find('.application__header')

const submitButton = () =>
  testFrame().find('.application__send-application-button')

const formSections = () =>
  testFrame().find(
    '.application__form-content-area .application__wrapper-element'
  )

const formFields = () =>
  testFrame().find('.application__form-content-area .application__form-field')

const invalidFieldsStatus = () =>
  testFrame().find('.application__invalid-field-status-title')

const invalidFieldNames = () =>
  _.map(testFrame().find('.application__invalid-fields > a > div'), e =>
    $(e).text()
  )

const selectedHakukohteet = () =>
  testFrame()
    .find('.application__hakukohde-selected-list')
    .find('.application__selected-hakukohde-row')

const hakukohdeSearchInput = () =>
  testFrame().find('.application__hakukohde-selection-search-input input')

const personInfoModule = () => formSections().eq(0)

const selectedHakukohdeName = hakukohdeRow =>
  $(hakukohdeRow)
    .find('.application__hakukohde-header')
    .first()
    .text()

const hasFormField = fieldId =>
  testFrame().find('#scroll-to-' + fieldId).length === 1

const setFieldInputValue = (id, value) =>
  setTextFieldValue(
    () =>
      testFrame()
        .find(id)
        .focus(),
    value
  )

const setNthFieldInputValue = (n, value) =>
  setTextFieldValue(
    () =>
      formFields()
        .eq(n)
        .find('input')
        .focus(),
    value
  )

const selectNthField = n =>
  clickElement(() =>
    formFields()
      .eq(n)
      .find('input')
      .focus()
  )

const setNthFieldValue = (n, selector, value) =>
  setTextFieldValue(
    () =>
      formFields()
        .eq(n)
        .find(selector),
    value
  )

const setNthFieldSubInputValue = (n, sub, value) =>
  setTextFieldValue(
    () =>
      formFields()
        .eq(n)
        .find('input')
        .eq(sub)
        .focus(),
    value
  )

const setNthFieldOption = (n, value) =>
  wait.until(() => {
    const $option = formFields()
      .eq(n)
      .find('option[value="' + value + '"]')
    const $select = formFields()
      .eq(n)
      .find('select')
    if (elementExists($option) && elementExists($select)) {
      $option.prop('selected', true)
      triggerEvent($select, 'change')
      return true
    }
  })

const clickNthFieldRadio = (n, value) => () => {
  formFields()
    .eq(n)
    .find('label:contains(' + value + ')')
    .click()
}

const addHakukohdeLink = () =>
  testFrame().find('.application__hakukohde-selection-open-search')

const hakukohdeSearchHits = () =>
  testFrame().find('.application__search-hit-hakukohde-row')

const nthHakukohdeSearchResultButton = n =>
  hakukohdeSearchHits()
    .eq(n)
    .find('button')

const nthHakukohdePriorityUp = n =>
  testFrame()
    .find('.application__selected-hakukohde-row')
    .eq(n)
    .find('.application__selected-hakukohde-row--priority-increase')

const nthHakukohdePriorityDown = n =>
  testFrame()
    .find('.application__selected-hakukohde-row')
    .eq(n)
    .find('.application__selected-hakukohde-row--priority-decrease')

const selectedHakukohdeTexts = () =>
  testFrame()
    .find('.application__selected-hakukohde-row--content')
    .text()

const searchHakukohdeTexts = () =>
  testFrame()
    .find('.application__search-hit-hakukohde-row--content')
    .text()

const assertOnlyFinnishSsn = () => {
  expect(hasFormField('ssn')).to.equal(true)
  expect(hasFormField('have-finnish-ssn')).to.equal(false)
  expect(hasFormField('gender')).to.equal(false)
  expect(hasFormField('birth-date')).to.equal(false)
  expect(hasFormField('birthplace')).to.equal(false)
  expect(hasFormField('passport-number')).to.equal(false)
  expect(hasFormField('national-id-number')).to.equal(false)
  expect(hasFormField('birthplace')).to.equal(false)
}

const assertHaveFinnishSsn = () => {
  expect(hasFormField('ssn')).to.equal(true)
  expect(hasFormField('have-finnish-ssn')).to.equal(true)
  // should not display non-ssn fields!
  expect(hasFormField('gender')).to.equal(false)
  expect(hasFormField('birth-date')).to.equal(false)
  expect(hasFormField('birthplace')).to.equal(false)
  expect(hasFormField('passport-number')).to.equal(false)
  expect(hasFormField('national-id-number')).to.equal(false)
  expect(hasFormField('birthplace')).to.equal(false)
}

const assertNonFinnishSsnFields = () => {
  expect(hasFormField('ssn')).to.equal(false)
  expect(hasFormField('gender')).to.equal(true)
  expect(hasFormField('birth-date')).to.equal(true)
  expect(hasFormField('birthplace')).to.equal(true)
  expect(hasFormField('passport-number')).to.equal(true)
  expect(hasFormField('national-id-number')).to.equal(true)
  expect(hasFormField('birthplace')).to.equal(true)
}

const assertInvalidFieldCount = count => {
  if (count === 0) {
    return () => {
      expect(invalidFieldsStatus().length).to.equal(0)
    }
  } else {
    return () => {
      expect(invalidFieldsStatus().text()).to.equal(
        'Tarkista ' + count + ' tietoa'
      )
    }
  }
}

const focusInput = index => () => {
  formFields()
    .eq(index)
    .find('input')
    .focus()
}

const readonlyAnswer = index =>
  testFrame()
    .find('.application__text-field-paragraph:eq(' + index + ')')
    .text()

const adjacentReadonlyAnswer = index =>
  testFrame()
    .find('.application__readonly-adjacent-cell:eq(' + index + ')')
    .text()

const submitButtonEnabled = () => !submitButton().prop('disabled')

const submitButtonDisabled = () => !submitButtonEnabled()
