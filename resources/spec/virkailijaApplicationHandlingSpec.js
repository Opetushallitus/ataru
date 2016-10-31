(function() {
  function closedFormList() {
    return testFrame().find('.application-handling__form-list-closed')
  }

  function form1OnList() {
    return testFrame().find('.application-handling__form-list-row:contains(Selaintestilomake1)')
  }

  function form2OnList() {
    return testFrame().find('.application-handling__form-list-row:contains(Selaintestilomake2)')
  }

  function downloadLink() {
    return testFrame().find('.application-handling__excel-download-link')
  }

  function closedFormListExists() {
    return elementExists(closedFormList())
  }

  function navigateToApplicationHandling() {
    loadInFrame('http://localhost:8350/lomake-editori/applications/')
  }

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('Application handling', function() {
    describe('form 1', function() {
      before(
        navigateToApplicationHandling,
        wait.until(closedFormListExists),
        clickElement(closedFormList),
        function() {
          // clickElement doesn't work for a href, jquery's click() does:
          form1OnList().click()
        },
        wait.until(function() { return closedFormList().text() ===  'Selaintestilomake1' })
      )
      it('has applications', function() {
        expect(closedFormList().text()).to.equal('Selaintestilomake1')
        expect(downloadLink().text()).to.equal('Lataa hakemukset Excel-muodossa (1)')
      })
    })
    describe('form 2 (no applications)', function() {
      before(
        function() { closedFormList()[0].click() },
        wait.until(function() {
          return form2OnList().text() === 'Lomake: Selaintestilomake2'
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
