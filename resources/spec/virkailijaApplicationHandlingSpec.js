(function() {
  afterEach(function() {
    expect(window.uiError || null).to.be.null
  });

  describe('Application handling', function() {
    describe('for first form', function() {
      var firstNotSelected = null;
      var eventCountBefore = null;
      before(
        navigateToApplicationHandling,
        wait.until(directFormHakuListExists),
        function() {
          //clickElement doesn't work for a href here:
          form1OnList()[0].click()
        },
        wait.until(function() { return applicationHeader().text() ===  'Selaintestilomake1' }),
        clickElement(applicationRow),
        wait.until(function() { return reviewHeader().length > 0 }),
        clickElement(function () { return selectedState().first() }),
        wait.until(function() { return notSelectedStates().length === 6 }),
        function() {
          firstNotSelected = notSelectedStates().first();
          expect(eventCaptions().length).to.equal(1);
        },
        clickElement(function () { return firstNotSelected }),
        wait.until(function() { return eventCaptions().length == 2 })
      );
      it('has applications', function() {
        expect(applicationHeader().text()).to.equal('Selaintestilomake1');
        expect(downloadLink().text()).to.equal('Lataa Excel')
      });
      it('stores an event for review state change', function() {
        var lastEventNow = testFrame().find('.application-handling__event-caption').last().text();
        expect(lastEventNow).to.equal('Käsittelyvaihe: Käsittelyssä (VV)')
      });
      it('Successfully stores notes and score for an application', function(done) {
        var scoreForVatanen = Math.floor((Math.random() * 50) + 1);
        var scoreForKuikeloinen = (scoreForVatanen + 5);
        var scoreForTyrni = scoreForKuikeloinen - 10;

        setTextFieldValue(reviewNotes, 'Reipas kaveri')()
        .then(wait.until(function() { return reviewNotesSubmitButton().attr('disabled') !== 'disabled' }))
        .then(clickElement(reviewNotesSubmitButton))
        .then(wait.until(function() { return testFrame().find('.application-handling__review-details-column > span:eq(0)').text() === 'Veijo Virkailija' }))
        .then(setTextFieldValue(score, scoreForVatanen))
        .then(clickElement(secondApplication))
        .then(wait.until(applicationHeadingIs('Kuikeloinen, Seija Susanna — 020202A0202')))
        .then(function() {
          expect(reviewNotes().val()).to.equal('')
        })
        .then(setTextFieldValue(score, scoreForKuikeloinen))
        .then(clickElement(firstApplication))
        .then(wait.until(applicationHeadingIs('Vatanen, Ari — 141196-933S')))
        .then(function () {
          expect(testFrame().find('.application-handling__review-details-column > span:eq(0)').text()).to.equal('Veijo Virkailija');
          expect(score().val()).to.equal(scoreForVatanen + '');
          done()
        })
        .then(clickElement(thirdApplication))
        .then(wait.until(applicationHeadingIs('Tyrni, Johanna Irmeli — 020202A0202')))
        .then(setTextFieldValue(score, scoreForTyrni))
        .fail(done)
      });

      it('shows yksilointitieto for application', function(done) {
        clickElement(firstApplication)()
          .then(wait.until(applicationHeadingIs('Vatanen, Ari — 141196-933S')))
          .then(function() {
            expect(elementExists(testFrame().find('.individualization'))).to.equal(false);
          })
          .then(clickElement(thirdApplication))
          .then(wait.until(applicationHeadingIs('Tyrni, Johanna Irmeli — 020202A0202')))
          .then(function () {
            expect(elementExists(testFrame().find('.individualization'))).to.equal(true);
            done()
          })
          .fail(done)
      });

      describe('successfully changes selection state', function() {
        before(
          wait.until(function() { return selectionStateSelected().text() === "Kesken" }),
          clickElement(selectionStateSelected),
          wait.until(function() { return selectionStateOpened().is(':visible') }),
          clickElement(function() { return selectionStateOpened().find('.application-handling__review-state-row:contains("Hyväksytty")')}),
          wait.until(function() { return selectionStateOpened().length === 0 && selectionStateSelected().text() === "Hyväksytty" })
        )
        it('selects new state correctly', function() {
          expect(selectionStateSelected().text()).to.equal("Hyväksytty")
          expect(thirdApplication().find('.application-handling__hakukohde-selection').text()).to.equal("Hyväksytty")
        })
      })

      function selectionStateSelected() { return testFrame().find('.application-handling__review-state-container-selection-state .application-handling__review-state-selected-row') }

      function selectionStateOpened() { return testFrame().find('.application-handling__review-state-container-selection-state .application-handling__review-state-list-opened') }

      function firstApplication() { return testFrame().find('.application-handling__list-row--application-applicant:contains(Vatanen)').closest('.application-handling__list-row') }

      function secondApplication() { return testFrame().find('.application-handling__list-row--application-applicant:contains(Kuikeloinen)').closest('.application-handling__list-row') }

      function thirdApplication() { return testFrame().find('.application-handling__list-row--application-applicant:contains(Tyrni)').closest('.application-handling__list-row') }

      function reviewNotes() { return testFrame().find('.application-handling__review-note-input') }

      function reviewNotesSubmitButton() { return testFrame().find('.application-handling__review-note-submit-button') }

      function score() { return testFrame().find('.application-handling__score-input') }

      function form1OnList() {
        return testFrame().find(".application__search-control-direct-form-haku a:contains(Selaintestilomake1)")
      }

      function directFormHakuListExists() {
        return elementExists(directFormHakuList())
      }

      function navigateToApplicationHandling() {
        loadInFrame('http://localhost:8350/lomake-editori/applications/')
      }

      function eventCaptions() {
        return testFrame().find('.application-handling__event-caption')
      }

      function applicationRow() {
        return testFrame().find('.application-handling__list-row:not(.application-handling__list-header) .application-handling__list-row--application-applicant:contains(Vatanen)')
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

      function downloadLink() {
        return testFrame().find('.application-handling__excel-download-link')
      }
    });

    describe('Application sorting', function () {
      it('Sorting by sortable columns works', function(done) {
        var firstApplicantNameBeforeAnySorting = null;
        wait.until(applicantNamesExist)()
            .then(function() {
              firstApplicantNameBeforeAnySorting = firstApplicantName()
            })
            .then(clickElement(timeColumn))
            .then(wait.until(function() {
              // We can't really know the initial time order exactly, the inserts in
              // fixture are so close
              return firstApplicantName() !== firstApplicantNameBeforeAnySorting
            }))
            .then(clickElement(applicantColumn))
            .then(wait.until(firstApplicantNameIs("Vatanen, Ari")))
            .then(function() {
              expectApplicants(["Vatanen, Ari", "Tyrni, Johanna Irmeli", "Kuikeloinen, Seija Susanna"])
            })
            .then(clickElement(applicantColumn))
            .then(wait.until(firstApplicantNameIs("Kuikeloinen, Seija Susanna")))
            .then(function() {
              expectApplicants(["Kuikeloinen, Seija Susanna", "Tyrni, Johanna Irmeli", "Vatanen, Ari"])
            })
            .then(done)
            .fail(done)
      });

      function expectApplicants(expected) {
        expect(applicantNames()).to.eql(expected)
      }

      function firstApplicantName() {
        return applicantNames()[0]
      }

      function firstApplicantNameIs(expected) {
        return function() { return firstApplicantName() === expected }
      }

      function applicantNamesExist() {
        return function() { return applicantNames().length > 0 }
      }

      function applicantNames() {
        var scoreColumnObjects = testFrame().find('.application-handling__list-row--application-applicant');
        return _(scoreColumnObjects)
            .map(function (obj) { return $(obj).text() })
            .filter(function (val) { return val !== 'Hakija' })
            .value()
      }

      function applicantColumn() {
        return testFrame().find('.application-handling__list-row--applicant')
      }

      function timeColumn() {
        return testFrame().find('.application-handling__list-row--time')
      }

    });

    describe('application filtering on hakukohde processing state', function() {
      before(clickElement(hakukohdeProcessingFilterLink));
      it('reduces application list', function(done) {
        expect(includedHakukohdeProcessingStateFilters()).to.equal(8);
        expect(filteredApplicationsCount()).to.equal(3);

        var stateOfFirstApplicationHakukohde = applicationHakukohdeProcessingStates().eq(0).text();
        var stateOfSecondApplicationHakukohde = applicationHakukohdeProcessingStates().eq(2).text();

        filterOutBasedOnFirstApplicationState(stateOfFirstApplicationHakukohde);
        wait.until(function() {
          var expectedFilteredCount = stateOfFirstApplicationHakukohde === stateOfSecondApplicationHakukohde ? 0 : 1;
          return filteredApplicationsCount() === expectedFilteredCount
        })()
        .then(function() {
          filterInBasedOnFirstApplicationState(stateOfFirstApplicationHakukohde);
          return wait.until(function() {
            return filteredApplicationsCount() === 3
          })()
        })
        .then(clickElement(hakukohdeProcessingFilterLink))
        .then(done)
        .fail(done)
      });

      function filterOutBasedOnFirstApplicationState(stateOfFirstApplication) {
        testFrame().find('.application-handling__list-row--state .application-handling__filter-state-selected-row span:contains(' + stateOfFirstApplication + ')').click()
      }

      function filterInBasedOnFirstApplicationState(stateOfFirstApplication) {
        testFrame().find('.application-handling__list-row--state .application-handling__filter-state-selection-row span:contains(' + stateOfFirstApplication + ')').click()
      }

      function applicationHakukohdeProcessingStates() {
        return testFrame().find('.application-handling__list .application-handling__hakukohde-state')
      }

      function filteredApplicationsCount() {
        return applicationHakukohdeProcessingStates().length
      }
    });

    describe('application filtering on selection state', function () {
      describe('adding filters', function () {
        before(
          clickElement(selectionStateFilterLink),
          wait.until(function () {
            return includedSelectionStateFilters() === 6 && filteredApplicationsCount() === 3
          }),
          function() {
            // clickElement doesn't work here..?
            testFrame().find('.application-handling__list-row--selection .application-handling__filter-state-selected-row span:contains("Kesken")').click()
          },
          wait.until(function () {
            return includedSelectionStateFilters() === 4 && filteredApplicationsCount() === 1
          })
        )
        it('reduces application list', function () {
          expect(testFrame().find('.application-handling__list-row--application-applicant:eq(0)').text()).to.equal('Tyrni, Johanna Irmeli')
        })
      })

      describe('removing filters', function () {
        before(
          function() {
            // clickElement doesn't work here either..?
            testFrame().find('.application-handling__list-row--selection .application-handling__filter-state-selection-row span:contains("Kaikki")').click()
          },
          wait.until(function () {
            return includedSelectionStateFilters() === 6 && filteredApplicationsCount() === 3
          }),
          clickElement(selectionStateFilterLink)
        )
        it('grows application list', function () {
          expect(testFrame().find('.application-handling__list-row--application-applicant:eq(0)').text()).to.equal('Kuikeloinen, Seija Susanna')
        })
      })
    })

    describe('finding all applications belonging to a given ssn', function() {
      before(
        clickElement(multipleApplicationsApplicant)
      );

      it('shows link to all applications belonging to a given ssn', function(done) {
        wait.until(function() {
          return searchApplicationsBySsnLink().text() === '2 hakemusta'
        })()
        .then(clickElement(searchApplicationsBySsnLink))
        .then(wait.until(ssnSearchFieldHasValue('020202A0202')))
        .then(wait.until(function() {
          return _.isEqual(applicantNames(), ['Tyrni, Johanna Irmeli', 'Kuikeloinen, Seija Susanna'])
        }))
        .then(done)
        .fail(done)
      });

      function multipleApplicationsApplicant() {
        return testFrame().find('.application-handling__list-row--application-applicant:contains(Kuikeloinen)')
      }

      function searchApplicationsBySsnLink() {
        return testFrame().find('.application-handling__review-area-main-heading-applications-link')
      }

      function ssnSearchField() {
        return testFrame().find('.application__search-control-search-term-input')
      }

      function ssnSearchFieldHasValue(value) {
        return function() {
          return ssnSearchField().val() === value
        }
      }

      function applicantNames() {
        var scoreColumnObjects = testFrame().find('.application-handling__list-row--application-applicant');
        return _.map(scoreColumnObjects, function (obj) { return $(obj).text() })
      }
    });

    describe('Virkailija hakemus edit', function () {
      describe('shows correct link', function () {
        before(
          navigateToApplicationHandling,
          wait.until(directFormHakuListExists),
          function () {
            //clickElement doesn't work for a href here:
            form1OnList()[0].click()
          },
          wait.until(function () {
            return applicationHeader().text() === 'Selaintestilomake1'
          }),
          clickElement(applicationRow),
          wait.until(function () {
            return reviewHeader().length > 0
          })
        );

        it('shows virkailija edit link', function() {
          expect(editLink().attr('href')).to.equal('/lomake-editori/api/applications/application-key2/modify');
        })
      });
    });

    describe('Virkailija link share', function () {
      describe('Shows application and correct filters', function () {
        before(
          navigateToApplicationHandlingWithUrlParams,
          wait.until(function () {
            return applicationHeader().text() === 'Selaintestilomake1'
          }),
          wait.until(applicationHeadingIs('Kuikeloinen, Seija Susanna — 020202A0202')),
          clickElement(hakukohdeProcessingFilterLink)
        );

        it('has correct filters selected', function() {
          expect(includedHakukohdeProcessingStateFilters()).to.equal(5);
        })
      });
    });

    describe('Mass application update', function() {
      describe('popup box', function() {
        before(
          navigateToApplicationHandlingForForm,
          clickElement(function() { return testFrame().find('.application-handling__mass-edit-review-states-link') }),
          wait.until(function() {
            return massUpdatePopup().is(':visible')
          })
        )

        it('has expected data in applications and popup', function() {
          expect(applicationHakukohdeStates()).to.eql(['Käsittelemättä', 'Käsittelyssä', 'Käsittelemättä'])
          expect(massUpdateFromStateSelectionClosed().text()).to.equal('Käsittelemättä (2)')
          expect(massUpdateToStateSelectionClosed().text()).to.equal('Käsittelemättä')
        })
      })

      describe('state selection boxes', function() {
        before(
          clickElement(massUpdateFromStateSelectionClosed),
          clickElement(massUpdateToStateSelectionClosed)
        )

        it('have the correct contents', function() {
          expect(massUpdateFromStateSelectionOpened().find('.application-handling__review-state-row').length === 7)
          expect(massUpdateFromStateSelectionOpened().find('.application-handling__review-state-row--disabled').length === 5)
          expect(massUpdateFromStateSelectionOpened().find('.application-handling__review-state-selected-row').text()).to.equal('Käsittelemättä (2)')

          expect(massUpdateToStateSelectionOpened().find('.application-handling__review-state-row').length === 7)
          expect(massUpdateToStateSelectionOpened().find('.application-handling__review-state-row--disabled').length === 0)
          expect(massUpdateToStateSelectionOpened().find('.application-handling__review-state-selected-row').text()).to.equal('Käsittelemättä')

          expect(massUpdateSubmitButton().attr('disabled')).to.equal('disabled')
        })
      })

      describe('selecting to-state and submitting', function() {
        before(
          clickElement(function() {
            return massUpdateFromStateSelectionOpened().find('.application-handling__review-state-row--mass-update:contains("Käsittelemättä")')
          }),
          clickElement(function() {
            return massUpdateToStateSelectionOpened().find('.application-handling__review-state-row--mass-update:contains("Käsitelty")')
          }),
          wait.until(function() {
            return massUpdateSubmitButton().attr('disabled') !== 'disabled'
          }),
          clickElement(massUpdateSubmitButton),
          wait.until(function() {
            return massUpdateSubmitButton().text() === 'Vahvista muutos'
          }),
          clickElement(massUpdateSubmitButton)
        )
        it('closes popup', function() {
          expect(massUpdatePopup().is(':visible')).to.equal(false)
        })
      })

      describe('updates applications', function () {
        before(
          wait.until(function() {
            return applicationHakukohdeStates().length > 0
          })
        )
        it('to selected state', function() {
          expect(applicationHakukohdeStates()).to.eql(['Käsitelty', 'Käsittelyssä', 'Käsitelty'])
        })
      })
    })

    function massUpdateSubmitButton() {
      return massUpdatePopup().find('.application-handling__link-button')
    }

    function massUpdateFromState() {
      return massUpdatePopup().children('div').eq(1)
    }

    function massUpdateFromStateSelectionOpened() {
      return massUpdateFromState().find('.application-handling__review-state-list-opened')
    }

    function massUpdateFromStateSelectionClosed() {
      var sel = '.application-handling__review-state-row--mass-update'
      return massUpdateFromState().find(sel).addBack(sel)
    }

    function massUpdateToState() {
      return massUpdatePopup().children('div').eq(2)
    }

    function massUpdateToStateSelectionOpened() {
      return massUpdateToState().find('.application-handling__review-state-list-opened')
    }

    function massUpdateToStateSelectionClosed() {
      var sel = '.application-handling__review-state-row--mass-update'
      return massUpdateToState().find(sel).addBack(sel)
    }

    function massUpdatePopup() {
      return testFrame().find('.application-handling__mass-edit-review-states-popup')
    }

    function applicationHakukohdeStates() {
      return _.map(testFrame().find('.application-handling__hakukohde-state'), function(o) {
        return $(o).text()
      })
    }

    function editLink() {
      return testFrame().find('.application-handling__link-button.application-handling__button:contains("Muokkaa hakemusta")')
    }

    function directFormHakuList() {
      return testFrame().find('.application__search-control-direct-form-haku')
    }

    function applicationHeader() {
      return testFrame().find('.application-handling__header-haku-name')
    }

    function form1OnList() {
      return testFrame().find(".application__search-control-direct-form-haku a:contains(Selaintestilomake1)")
    }

    function directFormHakuListExists() {
      return elementExists(directFormHakuList())
    }

    function navigateToApplicationHandling() {
      loadInFrame('http://localhost:8350/lomake-editori/applications/')
    }

    function navigateToApplicationHandlingWithUrlParams() {
      loadInFrame('http://localhost:8350/lomake-editori/applications/foobar1?application-key=application-key1&processing-state-filter=processing,invited-to-interview')
    }

    function navigateToApplicationHandlingForForm() {
      loadInFrame('http://localhost:8350/lomake-editori/applications/foobar1')
    }

    function includedHakukohdeProcessingStateFilters() {
      return testFrame().find('.application-handling__filter-state:eq(0) .application-handling__filter-state-selected-row').length
    }

    function includedSelectionStateFilters() {
      return testFrame().find('.application-handling__filter-state:eq(1) .application-handling__filter-state-selected-row').length
    }

    function applicationHeadingIs(expected) {
      return function() {
        return testFrame().find('.application-handling__review-area-main-heading').text() === expected
      }
    }

    function hakukohdeProcessingFilterLink() {
      return testFrame().find('.application-handling__filter-state a').eq(0)
    }

    function selectionStateFilterLink() {
      return testFrame().find('.application-handling__filter-state a').eq(1)
    }

    function applicationRow() {
      return testFrame().find('.application-handling__list-row:not(.application-handling__list-header) .application-handling__list-row--application-applicant:contains(Vatanen)')
    }

    function reviewHeader() {
      return testFrame().find('.application-handling__review-header')
    }

    function selectionStates() {
      return testFrame().find('.application-handling__list .application-handling__hakukohde-selection')
    }

    function filteredApplicationsCount() {
      return testFrame().find('.application-handling__list-row').not('.application-handling__list-header').length
    }
  })
})();
