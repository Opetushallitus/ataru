(function() {
  var closedFormList = function() {
    return testFrame().find('.application-handling__form-list-closed')
  }

  var downloadLink = function() {
    return testFrame().find('.application-handling__excel-download-link')
  }

  function closedFormListExists() {
    return elementExists(closedFormList())
  }

  function navigateToApplicationHandlingForm1Selected() {
    $('#test').attr('src', '/lomake-editori/#/applications/1')
  }

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('Application handling', function() {
    describe('with form 1', function() {
      before(
        navigateToApplicationHandlingForm1Selected,
        wait.until(closedFormListExists)
      )
      it('has applications', function() {
        expect(closedFormList().text()).to.equal('Selaintestilomake1')
        expect(downloadLink().text()).to.equal('Lataa hakemukset Excel-muodossa (1)')
      })
    })
  })

})();
