var testFormApplicationSecret = '{{test-form-application-secret}}'
var virkailijaSecret = '{{virkailija-secret}}'
var virkailijaCreateSecret = '{{virkailija-create-secret}}'

function newForm(formName) {
  var testFormKey = '{{test-form-key}}';
  var testQuestionGroupFormKey = '{{test-question-group-form-key}}';
  var ssnFormKey = '{{ssn-form-key}}';
  var formKey;

  switch (formName) {
    case 'testForm':
      formKey = testFormKey;
      break;
    case 'testQuestionGroupForm':
      formKey = testQuestionGroupFormKey;
      break;
    case 'ssnTestForm':
      formKey = ssnFormKey;
      break;
    default: console.log('No valid test form key found! Test will fail.. :(');
  }

  if (!formKey) {
    console.log("Test form key undefined (no form found). Did you run virkailija test first?");
  } else {
    console.log("form key", formKey);
    return function() {loadInFrame('/hakemus/' + formKey)};
  }

}

function formHeader() {
  return testFrame().find('.application__header')
}

function submitButton() {
  return testFrame().find('.application__send-application-button')
}

function formSections() {
  return testFrame().find('.application__form-content-area .application__wrapper-element')
}

function formFields() {
  return testFrame().find('.application__form-content-area .application__form-field')
}

function invalidFieldsStatus() {
  return testFrame().find('.application__invalid-field-status-title')
}

function invalidFieldNames() {
  return _.map(testFrame().find('.application__invalid-fields > a > div'), function (e) { return $(e).text() })
}

function selectedHakukohteet() {
  return testFrame().find('.application__hakukohde-selected-list').find('.application__hakukohde-row')
}

function hakukohdeSearchInput() {
  return testFrame().find('.application__hakukohde-selection-search-input input')
}

function personInfoModule() {
  return formSections().eq(0)
}

function selectedHakukohdeName(hakukohdeRow) {
  return $(hakukohdeRow).find('.application__hakukohde-selected-row-header').first().text()
}

function hasFormField(fieldId) {
  return testFrame().find('#scroll-to-' + fieldId).length === 1;
}

function setNthFieldInputValue(n, value) {
  return setTextFieldValue(function() { return formFields().eq(n).find('input').focus() }, value)
}
function selectNthField(n) {
    return clickElement(function() { return formFields().eq(n).find('input').focus() })
}
function setNthFieldValue(n, selector, value) {
  return function() {
    var $e = formFields().eq(n).find(selector)
    $e.val(value)
    triggerEvent($e, 'input') // needs to be input event because who knows why
  }
}

function setNthFieldSubInputValue(n, sub, value) {
  return setTextFieldValue(function() { return formFields().eq(n).find('input').eq(sub).focus() }, value)
}

function setNthFieldOption(n, value) {
  return wait.until(function() {
    var $option = formFields().eq(n).find('option[value="'+value+'"]')
    var $select = formFields().eq(n).find('select')
    if (elementExists($option) && elementExists($select)) {
      $option.prop('selected', true)
      triggerEvent($select, 'change')
      return true
    }
  })
}

function clickNthFieldRadio(n, value) {
  return function() {
    formFields().eq(n).find('label:contains('+value+')').click()
  }
}

function addHakukohdeLink() {
  return testFrame().find('.application__hakukohde-selection-open-search')
}

function hakukohdeSearchHits() {
  return testFrame().find('.application__hakukohde-row--search-hit')
}

function nthHakukohdeSearchResultButton(n) {
  return hakukohdeSearchHits().eq(n).find('button')
}

function nthHakukohdePriorityUp(n) {
  return testFrame().find('.application__hakukohde-row-priority-container')
    .eq(n)
    .find('span')
    .eq(0)
}

function nthHakukohdePriorityDown(n) {
  return testFrame().find('.application__hakukohde-row-priority-container')
    .eq(n)
    .find('span')
    .eq(1)
}

function selectedHakukohdeTexts() {
  return testFrame().find('.application__hakukohde-row-text-container--selected').text()
}

function hakukohdeTexts() {
    return testFrame().find('.application__hakukohde-row-text-container').text()
}

function assertOnlyFinnishSsn() {
  expect(hasFormField('ssn')).to.equal(true);
  expect(hasFormField('have-finnish-ssn')).to.equal(false);
  expect(hasFormField('gender')).to.equal(false);
  expect(hasFormField('birth-date')).to.equal(false);
  expect(hasFormField('birthplace')).to.equal(false);
  expect(hasFormField('passport-number')).to.equal(false);
  expect(hasFormField('national-id-number')).to.equal(false);
  expect(hasFormField('birthplace')).to.equal(false);
}

function assertHaveFinnishSsn() {
  expect(hasFormField('ssn')).to.equal(true);
  expect(hasFormField('have-finnish-ssn')).to.equal(true);
  // should not display non-ssn fields!
  expect(hasFormField('gender')).to.equal(false);
  expect(hasFormField('birth-date')).to.equal(false);
  expect(hasFormField('birthplace')).to.equal(false);
  expect(hasFormField('passport-number')).to.equal(false);
  expect(hasFormField('national-id-number')).to.equal(false);
  expect(hasFormField('birthplace')).to.equal(false);
}

function assertNonFinnishSsnFields() {
  expect(hasFormField('ssn')).to.equal(false);
  expect(hasFormField('gender')).to.equal(true);
  expect(hasFormField('birth-date')).to.equal(true);
  expect(hasFormField('birthplace')).to.equal(true);
  expect(hasFormField('passport-number')).to.equal(true);
  expect(hasFormField('national-id-number')).to.equal(true);
  expect(hasFormField('birthplace')).to.equal(true);
}

function assertInvalidFieldCount(count) {
  if (count === 0) {
    return function() {
      expect(invalidFieldsStatus().length).to.equal(0);
    }
  } else {
    return function() {
      expect(invalidFieldsStatus().text()).to.equal('Tarkista ' + count + ' tietoa');
    };
  }
}

function focusInput(index) {
  return function() {
    formFields().eq(index).find('input').focus();
  }
}

function readonlyAnswer(index) {
  return testFrame().find('.application__text-field-paragraph:eq(' + index + ')').text()
}

function adjacentReadonlyAnswer(index) {
  return testFrame().find('.application__readonly-adjacent-cell:eq(' + index + ')').text()
}

function submitButtonEnabled() {
  return !submitButton().prop('disabled')
}

function submitButtonDisabled() {
  return !submitButtonEnabled()
}