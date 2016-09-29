(function() {
  before(function () {
    var query = location.search.substring(1).split('&')
    var formId = ''

    for (var i = 0; i < query.length; i++) {
      var param = query[i].split('=')
      if (param[0] == 'formId') {
        formId = param[1]
      }
    }

    console.log("form id", formId || 'UNDEFINED')
    loadInFrame('/hakemus/' + formId)
  })

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

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

  function invalidSections() {
    return testFrame().find('.application__banner-wrapper-sections-content')
  }

  describe('hakemus', function() {

    describe('form loads', function () {
      before(
        wait.until(function() { return formSections().length == 2 })
      )
      it('with complete form', function() {
        expect(formFields().length).to.equal(20)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(invalidFieldsStatus().text()).to.equal('12 pakollista tietoa puuttuu')
        expect(invalidSections().find('a').length).to.equal(2)
        expect(invalidSections().find('a.application__banner-wrapper-section-link-not-valid').length).to.equal(2)
      })
    })
  })
})()