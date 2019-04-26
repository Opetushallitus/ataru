(function() {
  afterEach(function() {
    expect(window.uiError || null).to.be.null
  });

  describe('Application handling', function() {
    describe('for first form', function() {
      var firstNotSelected = null;
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
        wait.until(function() {
          firstNotSelected = notSelectedStates().first();
          return eventCaptions().length === 1;
        }),
        clickElement(function () { return firstNotSelected }),
        wait.until(function() { return eventCaptions().length === 2 })
      );
      it('has applications', function() {
        expect(applicationHeader().text()).to.equal('Selaintestilomake1');
        expect(downloadLink().text()).to.equal('Lataa Excel')
      });
      it('stores an event for review state change', function() {
        var firstEventNow = testFrame().find('.application-handling__event-caption').first().text();
        expect(firstEventNow).to.equal('Käsittelyvaihe: Käsittelyssä (TI)')
      });
      it('Successfully stores notes and score for an application', function(done) {
        var scoreForVatanen = Math.floor((Math.random() * 50) + 1);
        var scoreForKuikeloinen = (scoreForVatanen + 5);
        var scoreForTyrni = scoreForKuikeloinen - 10;

        setTextFieldValue(reviewNotes, 'Reipas kaveri')()
        .then(wait.until(function() { return reviewNotesSubmitButton().attr('disabled') !== 'disabled' }))
        .then(clickElement(reviewNotesSubmitButton))
        .then(wait.until(function() { return testFrame().find('.application-handling__review-note-details-row > div:eq(0)').text().startsWith('Testi Ihminen') }))
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
          expect(testFrame().find('.application-handling__review-note-details-row > div:eq(0)').text().startsWith('Testi Ihminen')).to.equal(true);
          expect(score().val()).to.equal(scoreForVatanen + '');
        })
        .then(clickElement(thirdApplication))
        .then(wait.until(applicationHeadingIs('Tyrni, Johanna Irmeli — 020202A0202')))
        .then(setTextFieldValue(score, scoreForTyrni))
        .then(done)
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
          .then(wait.until(function() {
            return elementExists(testFrame().find('.individualization'))
          }))
          .then(done)
          .fail(done)
      });

      describe('successfully changes selection state', function() {
        before(
          wait.until(function() { return selectionStateSelected().text() === "Kesken" }),
          clickElement(selectionStateSelected),
          wait.until(function() { return selectionStateOpened().is(':visible') }),
          clickElement(function() { return selectionStateOpened().find('.application-handling__review-state-row:contains("Hyväksytty")')}),
          wait.until(function() { return selectionStateOpened().length === 0 && selectionStateSelected().text() === "Hyväksytty" }),
          clickElement(firstApplication),
          wait.until(applicationHeadingIs('Vatanen, Ari — 141196-933S')),
          clickElement(thirdApplication),
          wait.until(applicationHeadingIs('Tyrni, Johanna Irmeli — 020202A0202'))
        )
        it('selects new state correctly', function() {
          expect(selectionStateSelected().text()).to.equal("Hyväksytty")
          expect(thirdApplication().find('.application-handling__hakukohde-selection').text()).to.equal("Hyväksytty")
        })
      })

      function selectionStateSelected() { return testFrame().find('.application-handling__review-state-container-selection-state .application-handling__review-state-row--selected') }

      function selectionStateOpened() { return testFrame().find('.application-handling__review-state-container-selection-state .application-handling__review-state-list--opened') }

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
        return testFrame().find('.application-handling__review-state-row--selected')
      }

      function notSelectedStates() {
        return testFrame().find('.application-handling__review-state-row:not(.application-handling__review-state-row--selected)')
      }

      function reviewHeader() {
        return testFrame().find('.application-handling__review-header')
      }

      function downloadLink() {
        return testFrame().find('.application-handling__excel-download-link')
      }
    });

    describe('Application sorting', function () {
      before(
        navigateToApplicationHandling,
        wait.until(directFormHakuListExists),
        function() {
          //clickElement doesn't work for a href here:
          form1OnList()[0].click()
        },
        wait.until(function() { return applicationHeader().text() ===  'Selaintestilomake1' }),
      );
      describe('Default sort', function () {
        before(wait.until(applicantNamesExist));
        it('Descending by applicant name', function () {
          expectApplicants(["Kuikeloinen, Seija Susanna", "Tyrni, Johanna Irmeli", "Vatanen, Ari"]);
        });
      });
      describe('Ascending sort by modification time', function () {
        before(
          clickElement(timeColumn),
          wait.until(firstApplicantNameIs("Kuikeloinen, Seija Susanna"))
        );
        it('works', function () {
          expectApplicants(["Kuikeloinen, Seija Susanna", "Vatanen, Ari", "Tyrni, Johanna Irmeli"]);
        });
      });
      describe('Sort by applicant name', function () {
        before(
          clickElement(applicantColumn),
          clickElement(applicantColumn),
          wait.until(firstApplicantNameIs("Vatanen, Ari"))
        );
        it('works', function () {
          expectApplicants(["Vatanen, Ari", "Tyrni, Johanna Irmeli", "Kuikeloinen, Seija Susanna"]);
        });
      });
      describe('Ascending sort by applicant name', function () {
        before(
          clickElement(applicantColumn),
          wait.until(firstApplicantNameIs("Kuikeloinen, Seija Susanna"))
        );
        it('works', function () {
          expectApplicants(["Kuikeloinen, Seija Susanna", "Tyrni, Johanna Irmeli", "Vatanen, Ari"])
        });
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
        return testFrame().find('.application-handling__list-row--applicant > span')
      }

      function timeColumn() {
        return testFrame().find('.application-handling__list-row--created-time i')
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
          return searchApplicationsBySsnLink()
        })()
        .then(clickElement(searchApplicationsBySsnLink))
        .then(wait.until(ssnSearchFieldHasValue('020202A0202')))
        .then(wait.until(function() {
          return _.isEqual(applicantNames(), ['Kuikeloinen, Seija Susanna', 'Tyrni, Johanna Irmeli'])
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
          expect(editLink().attr('href')).to.equal('/lomake-editori/api/applications/1.2.246.562.11.00000000000000000002/modify');
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
          expect(massUpdateToStateSelectionClosed().text()).to.equal('Käsittelemättä (2)')
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
          expect(massUpdateFromStateSelectionOpened().find('.application-handling__review-state-row--selected').text()).to.equal('Käsittelemättä (2)')

          expect(massUpdateToStateSelectionOpened().find('.application-handling__review-state-row').length === 7)
          expect(massUpdateToStateSelectionOpened().find('.application-handling__review-state-row--disabled').length === 0)
          expect(massUpdateToStateSelectionOpened().find('.application-handling__review-state-row--selected').text()).to.equal('Käsittelemättä (2)')

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

    describe('Mass send information requests', function() {
      describe('popup', function() {
        before(
          navigateToApplicationHandlingForForm,
          clickElement(function() { return testFrame().find('.application-handling__mass-information-request-link') }),
          wait.until(function() {
            return massInformationRequestPopup().is(':visible')
          })
        )

        it('has expected default data', function() {
          expect(massInformationRequestText()).to.eql('Lähetä sähköposti 3 hakijalle:')
          expect(massInformationRequestSubject().val()).to.eql('')
          expect(massInformationRequestContent().val()).to.eql('')
          expect(massInformationRequestSendButton().text()).to.eql('Lähetä')
          expect(massInformationRequestSendButton().attr('disabled')).to.eql('disabled')
        })
      })

      describe('updating inputs', function() {
        before(
          setTextFieldValue(massInformationRequestSubject, "Otsikko!"),
          setTextFieldValue(massInformationRequestContent, "Sisältöä")
        )
        it('enables button', function() {
          expect(massInformationRequestSendButton().text()).to.eql('Lähetä')
          expect(massInformationRequestSendButton().attr('disabled')).to.be.an('undefined')
        })
      })

      describe('recipient filtering', function () {
        before(
          clickElement(selectionStateFilterLink),
          wait.until(function () {
            return includedSelectionStateFilters() === 6 && filteredApplicationsCount() === 3
          }),
          function () {
            testFrame().find('.application-handling__list-row--selection .application-handling__filter-state-selected-row span:contains("Hyväksytty")').click()
          },
          wait.until(function () {
            return includedSelectionStateFilters() === 4 && filteredApplicationsCount() === 2
          }),
          clickElement(function() { return testFrame().find('.application-handling__mass-information-request-link') }),
          wait.until(function() {
            return massInformationRequestPopup().is(':visible')
          })
        )
        it('reduces application list and recipient count', function () {
          expect(testFrame().find('.application-handling__list-row--application-applicant:eq(0)').text()).to.equal('Vatanen, Ari')
          expect(testFrame().find('.application-handling__list-row--application-applicant:eq(1)').text()).to.equal('Kuikeloinen, Seija Susanna')
          expect(massInformationRequestText()).to.eql('Lähetä sähköposti 2 hakijalle:')
        })
      })

      describe('sending messages', function () {
        describe('first click', function() {
          before(
            clickElement(massInformationRequestSendButton),
            wait.until(function() {
              return massInformationRequestSendButton().hasClass('application-handling__send-information-request-button--confirm')
            })
          )
          it('requests confirmation', function() {
            expect(massInformationRequestSendButton().text()).to.equal('Vahvista 2 viestin lähetys')
          })
        })
        describe('second click', function() {
          before(
            clickElement(massInformationRequestSendButton),
            wait.until(function() {
              return massInformationRequestSendButton().length === 0
            })
          )
          it('removes button', function() {
            expect(massInformationRequestStatusText()).to.be.oneOf(['Lähetetään viestejä...', 'Viestit lisätty lähetysjonoon!'])
          })
        })
        describe('after success', function() {
          before(
            wait.until(function() {
              return massInformationRequestSendButton().length === 1
            })
          )
          it('resets form', function() {
            expect(massInformationRequestText()).to.eql('Lähetä sähköposti 2 hakijalle:')
            expect(massInformationRequestSubject().val()).to.eql('')
            expect(massInformationRequestContent().val()).to.eql('')
            expect(massInformationRequestSendButton().text()).to.eql('Lähetä')
            expect(massInformationRequestSendButton().attr('disabled')).to.eql('disabled')
          })
        })

      })
    })

    function massInformationRequestPopup() {
      return testFrame().find('.application-handling__mass-information-request-popup')
    }

    function massInformationRequestText() {
      return massInformationRequestPopup().find('p').first().text()
    }

    function massInformationRequestSubject() {
      return massInformationRequestPopup().find('input.application-handling__information-request-text-input')
    }

    function massInformationRequestContent() {
      return massInformationRequestPopup().find('textarea.application-handling__information-request-message-area')
    }

    function massInformationRequestSendButton() {
      return massInformationRequestPopup().find('button.application-handling__send-information-request-button')
    }

    function massInformationRequestStatusText() {
      return massInformationRequestPopup().find('.application-handling__information-request-status').text()
    }

    function massUpdateSubmitButton() {
      return massUpdatePopup().find('.application-handling__link-button')
    }

    function massUpdateFromState() {
      return massUpdatePopup().children('div').eq(1)
    }

    function massUpdateFromStateSelectionOpened() {
      return massUpdateFromState()
    }

    function massUpdateFromStateSelectionClosed() {
      var sel = '.application-handling__review-state-row--mass-update'
      return massUpdateFromState().find(sel).addBack(sel)
    }

    function massUpdateToState() {
      return massUpdatePopup().children('div').eq(2)
    }

    function massUpdateToStateSelectionOpened() {
      return massUpdateToState()
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
      return testFrame().find('.application-handling__header-haku')
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
      loadInFrame('http://localhost:8350/lomake-editori/applications/foobar1?application-key=1.2.246.562.11.00000000000000000001&processing-state-filter=processing,invited-to-interview')
    }

    function navigateToApplicationHandlingForForm() {
      loadInFrame('http://localhost:8350/lomake-editori/applications/foobar1')
    }

    function includedHakukohdeProcessingStateFilters() {
      return testFrame().find('.application-handling__filter-state:eq(1) .application-handling__filter-state-selected-row').length
    }

    function includedSelectionStateFilters() {
      return testFrame().find('.application-handling__filter-state:eq(2) .application-handling__filter-state-selected-row').length
    }

    function applicationHeadingIs(expected) {
      return function() {
        return testFrame().find('.application-handling__review-area-main-heading').text() === expected
      }
    }

    function hakukohdeProcessingFilterLink() {
      return testFrame().find('.application-handling__filter-state a').eq(1)
    }

    function selectionStateFilterLink() {
      return testFrame().find('.application-handling__filter-state a').eq(2)
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
