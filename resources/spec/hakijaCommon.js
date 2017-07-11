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
  return testFrame().find('.application__hakukohde-row')
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
