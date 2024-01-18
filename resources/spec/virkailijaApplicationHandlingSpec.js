;(() => {
  afterEach(() => {
    expect(window.uiError || null).to.be.null
  })

  const massInformationRequestPopup = () => {
    return testFrame().find(
      '.application-handling__mass-information-request-popup'
    )
  }

  const massInformationRequestText = () => {
    return massInformationRequestPopup().find('p').first().text()
  }

  const massInformationRequestSubject = () => {
    return massInformationRequestPopup().find(
      'input.application-handling__information-request-text-input'
    )
  }

  const massInformationRequestContent = () => {
    return massInformationRequestPopup().find(
      'textarea.application-handling__information-request-message-area'
    )
  }

  const massInformationRequestSendButton = () => {
    return massInformationRequestPopup().find(
      'button.application-handling__send-information-request-button'
    )
  }

  const massInformationRequestStatusText = () => {
    return massInformationRequestPopup()
      .find('.application-handling__information-request-status')
      .text()
  }

  const massUpdateSubmitButton = () => {
    return massUpdatePopup().find('.application-handling__link-button')
  }

  const massUpdateFromState = () => {
    return massUpdatePopup().children('div').eq(1)
  }

  const massUpdateFromStateSelectionOpened = () => {
    return massUpdateFromState()
  }

  const massUpdateFromStateSelectionClosed = () => {
    const sel = '.application-handling__review-state-row--mass-update'
    return massUpdateFromState().find(sel).addBack(sel)
  }

  const massUpdateToState = () => {
    return massUpdatePopup().children('div').eq(2)
  }

  const massUpdateToStateSelectionOpened = () => {
    return massUpdateToState()
  }

  const massUpdateToStateSelectionClosed = () => {
    const sel = '.application-handling__review-state-row--mass-update'
    return massUpdateToState().find(sel).addBack(sel)
  }

  const massUpdatePopup = () => {
    return testFrame().find(
      '.application-handling__mass-edit-review-states-popup'
    )
  }

  const applicationHakukohdeStates = () => {
    return _.map(
      testFrame().find('.application-handling__hakukohde-state'),
      (o) => $(o).text()
    )
  }

  const editLink = () => {
    return testFrame().find(
      '.application-handling__link-button.application-handling__button:contains("Muokkaa hakemusta")'
    )
  }

  const directFormHakuList = () => {
    return testFrame().find('.application__search-control-direct-form-haku')
  }

  const applicationHeader = () => {
    return testFrame().find('.application-handling__header-haku')
  }

  const form1OnList = () => {
    return testFrame().find(
      '.application__search-control-direct-form-haku a:contains(Selaintestilomake1)'
    )
  }

  const directFormHakuListExists = () => {
    return elementExists(directFormHakuList())
  }

  const navigateToApplicationHandling = () => {
    loadInFrame('http://localhost:8350/lomake-editori/applications/')
  }

  const navigateToApplicationHandlingWithUrlParams = () => {
    loadInFrame(
      'http://localhost:8350/lomake-editori/applications/foobar1?application-key=1.2.246.562.11.00000000000000000001&processing-state-filter=processing,invited-to-interview'
    )
  }

  const navigateToApplicationHandlingForForm = () => {
    loadInFrame('http://localhost:8350/lomake-editori/applications/foobar1')
  }

  const navigateToApplicationHandlingForHaku = () => {
    loadInFrame(
      'http://localhost:8350/lomake-editori/applications/haku/1.2.246.562.29.65950024186'
    )
  }

  const includedHakukohdeProcessingStateFilters = () => {
    return testFrame().find(
      '.application-handling__filter-state:eq(1) .application-handling__filter-state-selected-row'
    ).length
  }

  const includedSelectionStateFilters = () => {
    return testFrame().find(
      '.application-handling__filter-state:eq(2) .application-handling__filter-state-selection-column:eq(0) .application-handling__filter-state-selected-row'
    ).length
  }

  const applicationHeadingIs = (expected) => {
    return () => {
      return (
        testFrame()
          .find('.application-handling__review-area-main-heading')
          .text() === expected
      )
    }
  }

  const hakukohdeProcessingFilterLink = () => {
    return testFrame().find('.application-handling__filter-state a').eq(1)
  }

  const selectionStateFilterLink = () => {
    return testFrame().find('.application-handling__filter-state a').eq(2)
  }

  const applicationRow = () => {
    return testFrame().find(
      '.application-handling__list-row:not(.application-handling__list-header) .application-handling__list-row--applicant-name:contains(Vatanen)'
    )
  }

  const showResults = () => {
    return testFrame().find('[data-test-id=show-results]')
  }

  const reviewHeader = () => {
    return testFrame().find('.application-handling__review-header')
  }

  const filteredApplicationsCount = () => {
    return testFrame()
      .find('.application-handling__list-row')
      .not('.application-handling__list-header').length
  }

  const selectionStateSelected = () =>
    testFrame().find(
      '.application-handling__review-state-container-selection-state .application-handling__review-state-row--selected'
    )

  const selectionStateOpened = () => {
    return testFrame().find(
      '.application-handling__review-state-container-selection-state .application-handling__review-state-list--opened'
    )
  }

  const firstApplication = () => {
    return testFrame()
      .find('.application-handling__list-row--applicant-name:contains(Vatanen)')
      .closest('.application-handling__list-row')
  }

  const secondApplication = () => {
    return testFrame()
      .find(
        '.application-handling__list-row--applicant-name:contains(Kuikeloinen)'
      )
      .closest('.application-handling__list-row')
  }

  const thirdApplication = () => {
    return testFrame()
      .find('.application-handling__list-row--applicant-name:contains(Tyrni)')
      .closest('.application-handling__list-row')
  }

  const reviewNotes = () => {
    return testFrame().find('.application-handling__review-note-input')
  }

  const reviewNotesSubmitButton = () => {
    return testFrame().find('.application-handling__review-note-submit-button')
  }

  const hakukohdeRajausToggleButton = () => {
    return testFrame().find(
      '.application-handling__hakukohde-rajaus-toggle-button'
    )
  }

  const rajausHakukohdeFromList = (hakukohde) => {
    return testFrame()
      .find(
        '.hakukohde-and-hakukohderyhma-list-item-label:contains(' +
          hakukohde +
          ')'
      )
      .closest('.hakukohde-and-hakukohderyhma-category-list-item')
  }

  const applicationPersonNameFromList = (name) => {
    return testFrame()
      .find(
        '.application-handling__list-row--applicant-name:contains(' + name + ')'
      )
      .closest('.application-handling__list-row-person-info')
  }

  const clickNavigateToNextApplicationDetails = () => {
    return clickElement(() =>
      testFrame().find(
        '.application-handling__navigation-link:contains(Seuraava)'
      )
    )
  }

  const clickNavigateToPreviousApplicationDetails = () => {
    return clickElement(() =>
      testFrame().find(
        '.application-handling__navigation-link:contains(Edellinen)'
      )
    )
  }

  const applicationDetailsVisible = (name) => {
    return testFrame()
      .find(
        '.application-handling__review-area-main-heading:contains(' + name + ')'
      )
      .closest('.application-handling__detail-container')
      .find('.application-handling__hakukohde--selectable')
      .is(':visible')
  }

  const toggleApplicationDetailsHakukohdeSelected = (hakukohde) => {
    return clickElement(() =>
      testFrame()
        .find(
          '.application-handling__review-area-hakukohde-heading:contains(' +
            hakukohde +
            ')'
        )
        .closest('.application-handling__hakukohde--selectable')
    )
  }

  const isApplicationDetailsHakukohdeSelected = (hakukohde) => {
    return testFrame()
      .find('.application-handling__hakukohde--selected')
      .find(
        '.application-handling__review-area-hakukohde-heading:contains(' +
          hakukohde +
          ')'
      )
      .is(':visible')
  }

  const score = () => {
    return testFrame().find('.application-handling__score-input')
  }

  const eventCaptions = () =>
    testFrame().find('.application-handling__event-row-header > span')

  const selectedState = () => {
    return testFrame().find('.application-handling__review-state-row--selected')
  }

  const notSelectedStates = () => {
    return testFrame().find(
      '.application-handling__review-state-row:not(.application-handling__review-state-row--selected)'
    )
  }

  const downloadLink = () => {
    return testFrame().find('.application-handling__excel-download-link')
  }

  const expectApplicants = (expected) => {
    expect(applicantNames()).to.eql(expected)
  }

  const firstApplicantName = () => {
    return applicantNames()[0]
  }

  const firstApplicantNameIs = (expected) => {
    return () => {
      return firstApplicantName() === expected
    }
  }

  const applicantNamesExist = (minimumNumberOfApplicants) => {
    return () => {
      return applicantNames().length >= minimumNumberOfApplicants
    }
  }

  const applicantNames = () => {
    const scoreColumnObjects = testFrame().find(
      '.application-handling__list-row--applicant-name'
    )
    return _(scoreColumnObjects)
      .map((obj) => $(obj).text())
      .filter((val) => val !== 'Hakija')
      .value()
  }

  const applicantColumn = () => {
    return testFrame().find('.application-handling__list-row--applicant > span')
  }

  const timeColumn = () => {
    return testFrame().find('.application-handling__list-row--created-time i')
  }

  const multipleApplicationsApplicant = () => {
    return testFrame().find(
      '.application-handling__list-row--applicant-name:contains(Kuikeloinen)'
    )
  }

  const searchApplicationsBySsnLink = () => {
    return testFrame().find(
      '.application-handling__review-area-main-heading-applications-link'
    )
  }

  const ssnSearchField = () => {
    return testFrame().find('.application__search-control-search-term-input')
  }

  const ssnSearchFieldHasValue = (value) => {
    return () => {
      return ssnSearchField().val() === value
    }
  }

  describe('Application handling', () => {
    describe('for first form', () => {
      let firstNotSelected = null
      before(
        navigateToApplicationHandling,
        wait.until(directFormHakuListExists),
        () => {
          //clickElement doesn't work for a href here:
          form1OnList()[0].click()
        },
        wait.until(() => {
          return applicationHeader().text() === 'Selaintestilomake1'
        }),
        clickElement(showResults),
        clickElement(applicationRow),
        wait.until(() => {
          return reviewHeader().length > 0
        }),
        clickElement(() => {
          return selectedState().first()
        }),
        wait.until(() => {
          return notSelectedStates().length === 7
        }),
        wait.until(() => {
          firstNotSelected = notSelectedStates().first()
          return eventCaptions().length === 1
        }),
        clickElement(() => {
          return firstNotSelected
        }),
        wait.until(() => {
          return eventCaptions().length === 2
        })
      )
      it('has applications', () => {
        expect(applicationHeader().text()).to.equal('Selaintestilomake1')
        expect(downloadLink().text()).to.equal('Lataa Excel')
      })
      it('stores an event for review state change', () => {
        const firstEventNow = testFrame()
          .find('.application-handling__event-row-header > span')
          .first()
          .text()
        expect(firstEventNow).to.equal('Käsittelyvaihe: Käsittelyssä (TI)')
      })
      it('Successfully stores notes and score for an application', (done) => {
        const scoreForVatanen = Math.floor(Math.random() * 50 + 1)
        const scoreForKuikeloinen = scoreForVatanen + 5
        const scoreForTyrni = scoreForKuikeloinen - 10

        setTextFieldValue(reviewNotes, 'Reipas kaveri')()
          .then(
            wait.until(
              () => reviewNotesSubmitButton().attr('disabled') !== 'disabled'
            )
          )
          .then(clickElement(reviewNotesSubmitButton))
          .then(
            wait.until(() =>
              testFrame()
                .find('.application-handling__review-note-summary-text')
                .text()
                .endsWith('Testi Ihminen')
            )
          )
          .then(setTextFieldValue(score, scoreForVatanen))
          .then(clickElement(secondApplication))
          .then(
            wait.until(
              applicationHeadingIs('Kuikeloinen, Seija Susanna — 020202A0202')
            )
          )
          .then(() => {
            expect(reviewNotes().val()).to.equal('')
          })
          .then(setTextFieldValue(score, scoreForKuikeloinen))
          .then(clickElement(firstApplication))
          .then(wait.until(applicationHeadingIs('Vatanen, Ari — 141196-933S')))
          .then(() => {
            expect(
              testFrame()
                .find('.application-handling__review-note-summary-text')
                .text()
                .endsWith('Testi Ihminen')
            ).to.equal(true)
            expect(score().val()).to.equal(scoreForVatanen + '')
          })
          .then(clickElement(thirdApplication))
          .then(
            wait.until(
              applicationHeadingIs('Tyrni, Johanna Irmeli — 020202A0202')
            )
          )
          .then(setTextFieldValue(score, scoreForTyrni))
          .then(done)
          .fail(done)
      })

      it('Successfully clears score for an application', (done) => {
        const scoreForVatanen = Math.floor(Math.random() * 50 + 1)

        setTextFieldValue(score, scoreForVatanen)
          .then(clickElement(secondApplication))
          .then(
            wait.until(
              applicationHeadingIs('Kuikeloinen, Seija Susanna — 020202A0202')
            )
          )
          .then(clickElement(firstApplication))
          .then(wait.until(applicationHeadingIs('Vatanen, Ari — 141196-933S')))
          .then(() => {
            expect(score().val()).to.equal(scoreForVatanen + '')
          })
          .then(setTextFieldValue(score, ''))
          .then(clickElement(thirdApplication))
          .then(
            wait.until(
              applicationHeadingIs('Tyrni, Johanna Irmeli — 020202A0202')
            )
          )
          .then(clickElement(firstApplication))
          .then(wait.until(applicationHeadingIs('Vatanen, Ari — 141196-933S')))
          .then(() => {
            expect(score().val()).to.equal('')
          })
          .then(done)
          .fail(done)
      })

      it('shows yksilointitieto for application', (done) => {
        clickElement(firstApplication)()
          .then(wait.until(applicationHeadingIs('Vatanen, Ari — 141196-933S')))
          .then(() => {
            expect(
              elementExists(
                testFrame().find(
                  'span:contains("Tee yksilöinti henkilöpalvelussa.")'
                )
              )
            ).to.equal(false)
          })
          .then(clickElement(thirdApplication))
          .then(
            wait.until(
              applicationHeadingIs('Tyrni, Johanna Irmeli — 020202A0202')
            )
          )
          .then(
            wait.until(() => {
              return elementExists(
                testFrame().find(
                  'span:contains("Tee yksilöinti henkilöpalvelussa.")'
                )
              )
            })
          )
          .then(done)
          .fail(done)
      })

      describe('successfully changes selection state', () => {
        before(
          wait.until(() => {
            return selectionStateSelected().text() === 'Kesken'
          }),
          clickElement(selectionStateSelected),
          wait.until(() => {
            return selectionStateOpened().is(':visible')
          }),
          clickElement(() => {
            return selectionStateOpened().find(
              '.application-handling__review-state-row:contains("Hyväksytty")'
            )
          }),
          wait.until(() => {
            return (
              selectionStateOpened().length === 0 &&
              selectionStateSelected().text() === 'Hyväksytty'
            )
          }),
          clickElement(firstApplication),
          wait.until(applicationHeadingIs('Vatanen, Ari — 141196-933S')),
          clickElement(thirdApplication),
          wait.until(
            applicationHeadingIs('Tyrni, Johanna Irmeli — 020202A0202')
          )
        )
        it('selects new state correctly', () => {
          expect(selectionStateSelected().text()).to.equal('Hyväksytty')
          expect(
            thirdApplication()
              .find('.application-handling__hakukohde-selection')
              .text()
          ).to.equal('Hyväksytty')
        })
      })
    })

    describe('Application sorting', () => {
      before(
        navigateToApplicationHandling,
        wait.until(directFormHakuListExists),
        () => {
          //clickElement doesn't work for a href here:
          form1OnList()[0].click()
        },
        wait.until(() => {
          return applicationHeader().text() === 'Selaintestilomake1'
        }),
        clickElement(showResults)
      )
      describe('Default sort', () => {
        before(wait.until(applicantNamesExist(3)))
        it('Descending by applicant name', () => {
          expectApplicants([
            'Kuikeloinen, Seija Susanna',
            'Tyrni, Johanna Irmeli',
            'Vatanen, Ari',
          ])
        })
      })
      describe('Ascending sort by modification time', () => {
        before(
          clickElement(timeColumn),
          wait.until(firstApplicantNameIs('Kuikeloinen, Seija Susanna'))
        )
        it('works', () => {
          expectApplicants([
            'Kuikeloinen, Seija Susanna',
            'Vatanen, Ari',
            'Tyrni, Johanna Irmeli',
          ])
        })
      })
      describe('Sort by applicant name', () => {
        before(
          clickElement(applicantColumn),
          clickElement(applicantColumn),
          wait.until(firstApplicantNameIs('Vatanen, Ari'))
        )
        it('works', () => {
          expectApplicants([
            'Vatanen, Ari',
            'Tyrni, Johanna Irmeli',
            'Kuikeloinen, Seija Susanna',
          ])
        })
      })
      describe('Ascending sort by applicant name', () => {
        before(
          clickElement(applicantColumn),
          wait.until(firstApplicantNameIs('Kuikeloinen, Seija Susanna'))
        )
        it('works', () => {
          expectApplicants([
            'Kuikeloinen, Seija Susanna',
            'Tyrni, Johanna Irmeli',
            'Vatanen, Ari',
          ])
        })
      })
    })

    describe('application filtering on hakukohde processing state', () => {
      before(clickElement(hakukohdeProcessingFilterLink))
      it('reduces application list', (done) => {
        expect(includedHakukohdeProcessingStateFilters()).to.equal(9)
        expect(filteredApplicationsCount()).to.equal(3)

        const stateOfFirstApplicationHakukohde =
          applicationHakukohdeProcessingStates().eq(0).text()
        const stateOfSecondApplicationHakukohde =
          applicationHakukohdeProcessingStates().eq(2).text()

        filterOutBasedOnFirstApplicationState(stateOfFirstApplicationHakukohde)
        wait
          .until(() => {
            const expectedFilteredCount =
              stateOfFirstApplicationHakukohde ===
              stateOfSecondApplicationHakukohde
                ? 0
                : 1
            return filteredApplicationsCount() === expectedFilteredCount
          })()
          .then(() => {
            filterInBasedOnFirstApplicationState(
              stateOfFirstApplicationHakukohde
            )
            return wait.until(() => {
              return filteredApplicationsCount() === 3
            })()
          })
          .then(clickElement(hakukohdeProcessingFilterLink))
          .then(done)
          .fail(done)
      })

      const filterOutBasedOnFirstApplicationState = (
        stateOfFirstApplication
      ) => {
        testFrame()
          .find(
            '.application-handling__list-row--state .application-handling__filter-state-selected-row span:contains(' +
              stateOfFirstApplication +
              ')'
          )
          .click()
      }

      const filterInBasedOnFirstApplicationState = (
        stateOfFirstApplication
      ) => {
        testFrame()
          .find(
            '.application-handling__list-row--state .application-handling__filter-state-selection-row span:contains(' +
              stateOfFirstApplication +
              ')'
          )
          .click()
      }

      const applicationHakukohdeProcessingStates = () => {
        return testFrame().find(
          '.application-handling__list .application-handling__hakukohde-state'
        )
      }

      const filteredApplicationsCount = () => {
        return applicationHakukohdeProcessingStates().length
      }
    })

    describe('application filtering on selection state', () => {
      describe('adding filters', () => {
        before(
          clickElement(selectionStateFilterLink),
          wait.until(() => {
            return (
              includedSelectionStateFilters() === 6 &&
              filteredApplicationsCount() === 3
            )
          }),
          () => {
            // clickElement doesn't work here..?
            testFrame()
              .find(
                '.application-handling__list-row--selection .application-handling__filter-state-selection-column:eq(0) .application-handling__filter-state-selected-row span:contains("Kesken")'
              )
              .click()
          },
          wait.until(() => {
            return (
              includedSelectionStateFilters() === 4 &&
              filteredApplicationsCount() === 1
            )
          })
        )
        it('reduces application list', () => {
          expect(
            testFrame()
              .find('.application-handling__list-row--applicant-name:eq(0)')
              .text()
          ).to.equal('Tyrni, Johanna Irmeli')
        })
      })

      describe('removing filters', () => {
        before(
          () => {
            // clickElement doesn't work here either..?
            testFrame()
              .find(
                '.application-handling__list-row--selection .application-handling__filter-state-selection-column:eq(0) .application-handling__filter-state-selection-row span:contains("Kaikki")'
              )
              .click()
          },
          wait.until(() => {
            return (
              includedSelectionStateFilters() === 6 &&
              filteredApplicationsCount() === 3
            )
          }),
          clickElement(selectionStateFilterLink)
        )
        it('grows application list', () => {
          expect(
            testFrame()
              .find('.application-handling__list-row--applicant-name:eq(0)')
              .text()
          ).to.equal('Kuikeloinen, Seija Susanna')
        })
      })
    })

    describe('finding all applications belonging to a given ssn', () => {
      before(clickElement(multipleApplicationsApplicant))

      it('shows link to all applications belonging to a given ssn', (done) => {
        wait
          .until(() => {
            return searchApplicationsBySsnLink()
          })()
          .then(clickElement(searchApplicationsBySsnLink))
          .then(wait.until(ssnSearchFieldHasValue('020202A0202')))
          .then(
            wait.until(() => {
              return _.isEqual(applicantNames(), [
                'Kuikeloinen, Seija Susanna',
                'Tyrni, Johanna Irmeli',
              ])
            })
          )
          .then(done)
          .fail(done)
      })
    })

    describe('Virkailija hakemus edit', () => {
      describe('shows correct link', () => {
        before(
          navigateToApplicationHandling,
          wait.until(directFormHakuListExists),
          () => {
            //clickElement doesn't work for a href here:
            form1OnList()[0].click()
          },
          wait.until(() => {
            return applicationHeader().text() === 'Selaintestilomake1'
          }),
          clickElement(showResults),
          clickElement(applicationRow),
          wait.until(() => {
            return reviewHeader().length > 0
          })
        )

        it('shows virkailija edit link', () => {
          expect(editLink().attr('href')).to.equal(
            '/lomake-editori/api/applications/1.2.246.562.11.00000000000000000002/modify'
          )
        })
      })
    })

    describe('Virkailija link share', () => {
      describe('Shows application and correct filters', () => {
        before(
          navigateToApplicationHandlingWithUrlParams,
          clickElement(showResults),
          wait.until(() => {
            return applicationHeader().text() === 'Selaintestilomake1'
          }),
          wait.until(
            applicationHeadingIs('Kuikeloinen, Seija Susanna — 020202A0202')
          ),
          clickElement(hakukohdeProcessingFilterLink)
        )

        it('has correct filters selected', () => {
          expect(includedHakukohdeProcessingStateFilters()).to.equal(6)
        })
      })
    })

    /*describe('Mass application update', () => {
      describe('popup box', () => {
        before(
          navigateToApplicationHandlingForForm,
          clickElement(showResults),
          clickElement(() => {
            return testFrame().find(
              '.application-handling__mass-edit-review-states-link'
            )
          }),
          wait.until(() => {
            return massUpdatePopup().is(':visible')
          })
        )

        it('has expected data in applications and popup', () => {
          expect(applicationHakukohdeStates()).to.eql([
            'Käsittelemättä',
            'Käsittelemättä',
            'Käsittelyssä',
          ])
        })
      })

      describe('selecting to-state and submitting', () => {
        before(
          clickElement(() => {
            return massUpdateFromStateSelectionOpened().find(
              '.application-handling__review-state-row--mass-update:contains("Käsittelemättä")'
            )
          }),
          clickElement(() => {
            return massUpdateToStateSelectionOpened().find(
              '.application-handling__review-state-row--mass-update:contains("Käsitelty")'
            )
          }),
          wait.until(() => {
            return massUpdateSubmitButton().attr('disabled') !== 'disabled'
          }),
          clickElement(massUpdateSubmitButton),
          wait.until(() => {
            return massUpdateSubmitButton().text() === 'Vahvista muutos'
          }),
          clickElement(massUpdateSubmitButton)
        )
        it('closes popup', () => {
          expect(massUpdatePopup().is(':visible')).to.equal(false)
        })
      })

      describe('updates applications', () => {
        before(
          wait.until(() => {
            return _.includes(applicationHakukohdeStates(), 'Käsitelty')
          })
        )
        it('to selected state', () => {
          expect(applicationHakukohdeStates()).to.eql([
            'Käsitelty',
            'Käsitelty',
            'Käsittelyssä',
          ])
        })
      })
    })*/

    describe('Mass send information requests', () => {
      describe('popup', () => {
        before(
          navigateToApplicationHandlingForForm,
          clickElement(showResults),
          clickElement(() => {
            return testFrame().find(
              '.application-handling__mass-information-request-link'
            )
          }),
          wait.until(() => {
            return massInformationRequestPopup().is(':visible')
          })
        )

        it('has expected default data', () => {
          expect(massInformationRequestText()).to.eql(
            'Lähetä sähköposti 3 hakijalle:'
          )
          expect(massInformationRequestSubject().val()).to.eql('')
          expect(massInformationRequestContent().val()).to.eql('')
          expect(massInformationRequestSendButton().text()).to.eql('Lähetä')
          expect(massInformationRequestSendButton().attr('disabled')).to.eql(
            'disabled'
          )
        })
      })

      describe('updating inputs', () => {
        before(
          setTextFieldValue(massInformationRequestSubject, 'Otsikko!'),
          setTextFieldValue(massInformationRequestContent, 'Sisältöä')
        )
        it('enables button', () => {
          expect(massInformationRequestSendButton().text()).to.eql('Lähetä')
          expect(massInformationRequestSendButton().attr('disabled')).to.be.an(
            'undefined'
          )
        })
      })

      describe('recipient filtering', () => {
        before(
          clickElement(selectionStateFilterLink),
          wait.until(() => {
            return (
              includedSelectionStateFilters() === 6 &&
              filteredApplicationsCount() === 3
            )
          }),
          () => {
            testFrame()
              .find(
                '.application-handling__list-row--selection .application-handling__filter-state-selected-row span:contains("Hyväksytty")'
              )
              .click()
          },
          wait.until(() => {
            return (
              includedSelectionStateFilters() === 4 &&
              filteredApplicationsCount() === 2
            )
          }),
          clickElement(() => {
            return testFrame().find(
              '.application-handling__mass-information-request-link'
            )
          }),
          wait.until(() => {
            return massInformationRequestPopup().is(':visible')
          })
        )
        it('reduces application list and recipient count', () => {
          expect(
            testFrame()
              .find('.application-handling__list-row--applicant-name:eq(0)')
              .text()
          ).to.equal('Kuikeloinen, Seija Susanna')
          expect(
            testFrame()
              .find('.application-handling__list-row--applicant-name:eq(1)')
              .text()
          ).to.equal('Vatanen, Ari')
          expect(massInformationRequestText()).to.eql(
            'Lähetä sähköposti 2 hakijalle:'
          )
        })
      })

      describe('sending messages', () => {
        describe('first click', () => {
          before(
            clickElement(massInformationRequestSendButton),
            wait.until(() => {
              return massInformationRequestSendButton().hasClass(
                'application-handling__send-information-request-button--confirm'
              )
            })
          )
          it('requests confirmation', () => {
            expect(massInformationRequestSendButton().text()).to.equal(
              'Vahvista 2 viestin lähetys'
            )
          })
        })
        describe('second click', () => {
          before(
            clickElement(massInformationRequestSendButton),
            wait.until(() => {
              return massInformationRequestSendButton().length === 0
            }),
            wait.until(() => {
              return !_.isEmpty(massInformationRequestStatusText())
            })
          )
          it('removes button', () => {
            expect(massInformationRequestStatusText()).to.be.oneOf([
              'Käsitellään viestejä...',
              'Lähetetään viestejä...',
              'Viestit lisätty lähetysjonoon!',
            ])
          })
        })
        describe('after success', () => {
          before(
            wait.until(() => {
              return massInformationRequestSendButton().length === 1
            })
          )
          it('resets form', () => {
            expect(massInformationRequestText()).to.eql(
              'Lähetä sähköposti 2 hakijalle:'
            )
            expect(massInformationRequestSubject().val()).to.eql('')
            expect(massInformationRequestContent().val()).to.eql('')
            expect(massInformationRequestSendButton().text()).to.eql('Lähetä')
            expect(massInformationRequestSendButton().attr('disabled')).to.eql(
              'disabled'
            )
          })
        })
      })
    })
    describe('Application list filtering', () => {
      describe('filter by hakukohde and then open hakukohde details by pressing candidate name', () => {
        before(
          navigateToApplicationHandlingForHaku,
          clickElement(showResults),
          wait.until(() => {
            return hakukohdeRajausToggleButton().is(':visible')
          }),
          clickElement(hakukohdeRajausToggleButton),
          wait.until(() => {
            return rajausHakukohdeFromList('Testihakukohde 2').is(':visible')
          }),
          clickElement(() => rajausHakukohdeFromList('Testihakukohde 2')),
          wait.until(() => {
            return applicationPersonNameFromList('Maynard').is(':visible')
          }),
          clickElement(() => applicationPersonNameFromList('Maynard')),
          wait.until(() => {
            return applicationDetailsVisible('Maynard')
          })
        )
        it('by default selects the hakukohde according to current filter', () => {
          expect(
            isApplicationDetailsHakukohdeSelected('Testihakukohde 1')
          ).to.eql(false)
          expect(
            isApplicationDetailsHakukohdeSelected('Testihakukohde 2')
          ).to.eql(true)
        })
      })
      describe('move to the next application details', () => {
        before(
          clickNavigateToNextApplicationDetails(),
          wait.until(() => {
            return applicationDetailsVisible('Johanna')
          })
        )
        it('also by default selects the hakukohde according to current filter', () => {
          expect(
            isApplicationDetailsHakukohdeSelected('Testihakukohde 1')
          ).to.eql(false)
          expect(
            isApplicationDetailsHakukohdeSelected('Testihakukohde 2')
          ).to.eql(true)
        })
      })
      describe('modify and move to the previous application details', () => {
        before(
          toggleApplicationDetailsHakukohdeSelected('Testihakukohde 1'),
          wait.until(() => {
            return isApplicationDetailsHakukohdeSelected('Testihakukohde 1')
          }),
          clickNavigateToPreviousApplicationDetails(),
          wait.until(() => {
            return applicationDetailsVisible('Maynard')
          })
        )
        it('selects the default hakukohde according to current filter, regardless of modifications in previous', () => {
          expect(
            isApplicationDetailsHakukohdeSelected('Testihakukohde 1')
          ).to.eql(false)
          expect(
            isApplicationDetailsHakukohdeSelected('Testihakukohde 2')
          ).to.eql(true)
        })
      })
    })
  })
})()
