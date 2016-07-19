(function() {
  var closedFormList = function() {
    return testFrame().find('.application-handling__form-list-closed')
  }

  var form2OnList = function() {
    return testFrame().find('[href="#/applications/2"] .application-handling__form-list-row')
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
    describe('form 1', function() {
      before(
        navigateToApplicationHandlingForm1Selected,
        wait.until(closedFormListExists)
      )
      it('has applications', function() {
        expect(closedFormList().text()).to.equal('Selaintestilomake1')
        expect(downloadLink().text()).to.equal('Lataa hakemukset Excel-muodossa (1)')
      })
    })
    describe('form 2 (no applications)', function() {
      before(
        navigateToApplicationHandlingForm1Selected,
        wait.until(closedFormListExists),
        function() { closedFormList()[0].click() },
        wait.until(function() {
          return form2OnList().text() === 'Selaintestilomake2'
        }),
        function() { form2OnList()[0].click() },
        wait.until(function() { return closedFormList().text() === 'Selaintestilomake2' })
      )
      it('has no applications', function() {
        expect(closedFormList().text()).to.equal('Selaintestilomake2')
        expect(downloadLink()).to.have.length(0)
      })
    })
  })
})();
