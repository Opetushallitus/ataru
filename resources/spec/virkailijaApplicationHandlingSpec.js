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

  function eventCaptions() {
    return testFrame().find('.application-handling__event-caption')
  }

  function applicationRow() {
    return testFrame().find('.application-handling__list-row:not(.application-handling__list-header)')
  }

  function selectedState() {
    return testFrame().find('.application-handling__review-state-selected-row')
  }

  function notSelectedStates() {
    return testFrame().find('.application-handling__review-state-row:not(.application-handling__review-state-selected-row)')
  }

  function reviewHeader() {
    return testFrame().find('.application-handling__review-header')
  }

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('Application handling', function() {
    describe('form 1', function() {
      // Tie these to describe-scope instead of global
      var firstNotSelected = null;
      var eventCountBefore = null;
      var firstNotSelectedCaption = null;
      before(
        navigateToApplicationHandling,
        wait.until(closedFormListExists),
        clickElement(closedFormList),
        function() {
          // clickElement doesn't work for a href, jquery's click() does:
          form1OnList().click()
        },
        wait.until(function() { return closedFormList().text() ===  'Selaintestilomake1' }),
        clickElement(applicationRow),
        wait.until(function() { return reviewHeader().length > 0 }),
        clickElement(selectedState),
        wait.until(function() { return notSelectedStates().length > 1 }),
        function() {
          var notSelected =  notSelectedStates()
          expect(notSelected.length).to.be.at.least(1)
          firstNotSelected = notSelected.first()
          firstNotSelectedCaption = firstNotSelected.text()
          eventCountBefore = eventCaptions().length
          expect(eventCountBefore).to.be.at.least(1)
        },
        clickElement(function () { return firstNotSelected }),
        wait.until(function() { return eventCountBefore < eventCaptions().length })
      )
      it('has applications', function() {
        expect(closedFormList().text()).to.equal('Selaintestilomake1')
        expect(downloadLink().text()).to.equal('Lataa hakemukset Excel-muodossa (2)')
      })
      it('stores an event for review state change', function() {
        expect(eventCountBefore+1).to.equal(eventCaptions().length)
        var lastEventNow = testFrame().find('.application-handling__event-caption').last().text()
        expect(lastEventNow).to.equal(firstNotSelectedCaption)
      })
    })
    describe('application filtering', function() {
      before(clickElement(filterLink))
      it('reduces application list', function(done) {
        expect(includedFilters()).to.equal(9)
        expect(applicationStates().length).to.equal(2)

        var stateOfFirstApplication = applicationStates().eq(0).text()
        var stateOfSecondApplication = applicationStates().eq(1).text()

        filterOutBasedOnFirstApplicationState(stateOfFirstApplication)
        wait.until(function() {
          var expectedFilteredCount = stateOfFirstApplication === stateOfSecondApplication ? 0 : 1
          return filteredApplicationsCount() === expectedFilteredCount
        })()
        .then(function() {
          filterInBasedOnFirstApplicationState(stateOfFirstApplication)
          return wait.until(function() {
            return filteredApplicationsCount() === 2
          })()
        })
        .then(function() {
          done()
        })
      })

      function filterOutBasedOnFirstApplicationState(stateOfFirstApplication) {
        testFrame().find('.application-handling__filter-state-selected-row:contains(' + stateOfFirstApplication + ')').click()
      }

      function filterInBasedOnFirstApplicationState(stateOfFirstApplication) {
        testFrame().find('.application-handling__filter-state-selection-row:contains(' + stateOfFirstApplication + ')').click()
      }

      function includedFilters() {
        return testFrame().find('.application-handling__filter-state-selected-row').length
      }

      function filterLink() {
        return testFrame().find('.application-handling__filter-state a')
      }

      function applicationStates() {
        return testFrame().find('.application-handling__list .application-handling__list-row--state')
      }

      function filteredApplicationsCount() {
        return testFrame().find('.application-handling__list .application-handling__list-row--state').length
      }

    })
    describe('form 2 (no applications)', function() {
      before(
        function() { closedFormList()[0].click() },
        wait.until(function() {
          return form2OnList().text() === 'Lomake – Selaintestilomake2'
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
