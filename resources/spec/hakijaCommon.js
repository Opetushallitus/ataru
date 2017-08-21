var testFormApplicationSecret = '{{test-form-application-secret}}'

function newForm(formName) {
  var testFormKey = '{{test-form-key}}';
  var ssnFormKey = '{{ssn-form-key}}';
  var formKey;

  switch (formName) {
    case 'testForm':
      formKey = testFormKey;
      break;
    case 'ssnTestForm':
      formKey = ssnFormKey;
      break;
    default: console.log('No valid test form key found! Test will fail.. :(');
  }

  console.log("form key", formKey || 'UNDEFINED')
  return function() {loadInFrame('/hakemus/' + formKey)};
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

function selectedHakukohteet() {
  return testFrame().find('.application__hakukohde-row--selected')
}

function hakukohdeSearchInput() {
  return testFrame().find('.application__hakukohde-selection-search-input input')
}

function invalidSections() {
  return testFrame().find('.application__banner-wrapper-sections-content')
}

function personInfoModule() {
  return formSections().eq(0)
}

function selectedHakukohdeName(hakukohdeRow) {
  return $(hakukohdeRow).find('.application__hakukohde-selected-row-header')[0].textContent
}

function hasFormField(fieldId) {
  return testFrame().find('#scroll-to-' + fieldId).length === 1;
}

function setNthFieldInputValue(n, value) {
  return setTextFieldValue(function() { return formFields().eq(n).find('input').focus() }, value)
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
  return function() {
    formFields().eq(n).find('option[value="'+value+'"]').prop('selected', true)
    triggerEvent(formFields().eq(n).find('select'), 'change')
  }
}

function clickNthFieldRadio(n, value) {
  return function() {
    formFields().eq(n).find('label:contains('+value+')').click()
  }
}

function hakukohdeSearchHits() {
  return testFrame().find('.application__hakukohde-row--search-hit')
}

function nthHakukohdeSearchResultButton(n) {
  return hakukohdeSearchHits().eq(n).find('a')
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
