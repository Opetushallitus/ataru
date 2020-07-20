;(() => {
  const addNewFormLink = () => {
    return testFrame().find('.editor-form__control-button--enabled')
  }

  const formTitleField = () => {
    return testFrame().find('.editor-form__form-name-input')
  }

  const formList = () => {
    return testFrame().find('.editor-form__list')
  }

  const editorPageIsLoaded = () => {
    return elementExists(formList().find('a'))
  }

  const formListItems = (n) => {
    if ($.isNumeric(n)) {
      return formList().find('a').eq(n)
    } else {
      return formList().find('a')
    }
  }

  const personInfoModule = () => {
    return testFrame().find(
      ".editor-form__component-wrapper header:contains('Henkilötiedot')"
    )
  }

  const formComponents = () => {
    return (
      testFrame()
        .find('.editor-form__component-wrapper')
        // exclude followup question components
        .not(
          '.editor-form__followup-question-overlay .editor-form__component-wrapper'
        )
        // exclude hakukohteet
        .not(
          (i, node) => $(node).find("header:contains('Hakukohteet')").length > 0
        )
        // exclude henkilötiedot
        .not(
          (i, node) =>
            $(node).find("header:contains('Henkilötiedot')").length > 0
        )
    )
  }

  const formSections = () => {
    return testFrame()
      .find('.editor-form__component-wrapper')
      .filter(
        (i, node) => $(node).find("header:contains('Lomakeosio')").length > 0
      )
  }

  const clickLomakeForEdit = (name) =>
    clickElement(() =>
      formListItems().find(
        '.editor-form__list-form-name:contains("' + name + '")'
      )
    )

  const clickCopyFormComponent = (name) => () => {
    testFrame()
      .find('.editor-form__component-wrapper:contains("' + name + '")')
      .find('.editor-form__component-button:contains("Kopioi")')
      .click()
  }

  const clickCloseDetailsButton = () => () => {
    testFrame().find('.close-details-button').click()
  }

  const clickPasteFormComponent = (n) => () => {
    triggerEvent(
      testFrame()
        .find('.editor-form__drag_n_drop_spacer_container_for_component')
        .eq(n),
      'mouseover'
    )
    const selector =
      '.editor-form__drag_n_drop_spacer_container_for_component button.editor-form__component-button:visible:enabled:contains("Liitä")'
    return wait
      .until(() => {
        const b = testFrame().find(selector).length !== 0
        return b
      })()
      .then(() => testFrame().find(selector).click())
  }

  const clickComponentMenuItem = (title) => {
    const menuItem = () => {
      triggerEvent(
        testFrame().find('.editor-form > .editor-form__add-component-toolbar'),
        'mouseover'
      )
      return testFrame().find(
        '.editor-form > .editor-form__add-component-toolbar a:contains("' +
          title +
          '")'
      )
    }
    return clickElement(menuItem)
  }

  const clickSubComponentMenuItem = (title, element) => {
    const menuItem = () => {
      triggerEvent(
        element().find('.editor-form__add-component-toolbar'),
        'mouseover'
      )
      return element().find(
        '.editor-form__add-component-toolbar a:contains("' + title + '")'
      )
    }
    return clickElement(menuItem)
  }

  const clickRepeatingAnswers = (question) => {
    return () =>
      testFrame()
        .find('input.editor-form__text-field')
        .filter(function () {
          return this.value === question
        })
        .parent()
        .parent()
        .parent()
        .find(".editor-form__checkbox-wrapper label:contains('Vastaaja voi')")
        .prev()
        .click()
  }

  const clickNumericAnswer = (question) => {
    return () =>
      testFrame()
        .find('input.editor-form__text-field')
        .filter(function () {
          return this.value === question
        })
        .parent()
        .parent()
        .parent()
        .find(
          ".editor-form__checkbox-wrapper label:contains('Kenttään voi täyttää vain numeroita')"
        )
        .prev()
        .click()
  }

  const clickInfoTextCheckbox = (selector) => {
    return () =>
      selector().find('.editor-form__info-addon-checkbox > input').click()
  }

  const clickLockForm = () => {
    return clickElement(() => testFrame().find('#lock-form'))
  }

  const getInputs = (pseudoClass) => {
    return testFrame().find(
      'div.editor-form__panel-container input:not(#editor-form__copy-question-id-container)' +
        pseudoClass
    )
  }

  const getRemoveElementButtons = (pseudoClass) => {
    return testFrame().find(
      'div.editor-form__panel-container .editor-form__component-button:contains("Poista")' +
        pseudoClass
    )
  }

  before(() => {
    loadInFrame(
      'http://localhost:8350/lomake-editori/auth/cas?ticket=DEVELOPER'
    )
  })

  afterEach(() => {
    expect(window.uiError || null).to.be.null
  })

  describe('Editor', () => {
    describe('with fixture forms', () => {
      before(wait.until(editorPageIsLoaded))
      it('has 8 fixture forms', () => {
        expect(formListItems()).to.have.length(8)
      })
    })

    describe('form creation', () => {
      before(
        clickElement(addNewFormLink),
        wait.forMilliseconds(10 * 1000), // Odota, että autosave on valmistunut
        setTextFieldValue(formTitleField, 'Testilomake'),
        wait.until(
          () => formListItems(0).find('span:eq(0)').text() === 'Testilomake'
        ),
        wait.forElement(personInfoModule)
      )
      it('creates blank form', () => {
        expect(formComponents()).to.have.length(0)
      })
      it('has person info module', () => {
        expect(personInfoModule()).to.have.length(1)
      })
    })

    describe('adding elements:', () => {
      describe('textfield', () => {
        before(
          clickComponentMenuItem('Tekstikenttä'),
          setTextFieldValue(
            () => formComponents().eq(0).find('.editor-form__text-field'),
            'Ensimmäinen kysymys'
          ),
          clickElement(() =>
            formComponents()
              .eq(0)
              .find('.editor-form__info-addon-checkbox label')
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(0)
                .find('.editor-form__info-addon-inputs textarea'),
            'Ensimmäisen kysymyksen ohjeteksti'
          )
        )
        it('has expected contents', () => {
          expect(formComponents()).to.have.length(1)
          expect(
            formComponents().eq(0).find('.editor-form__text-field').val()
          ).to.equal('Ensimmäinen kysymys')
          expect(
            formComponents()
              .eq(0)
              .find('.editor-form__info-addon-checkbox input')
              .prop('checked')
          ).to.equal(true)
          expect(
            formComponents()
              .eq(0)
              .find('.editor-form__info-addon-inputs textarea')
              .val()
          ).to.equal('Ensimmäisen kysymyksen ohjeteksti')
          expect(
            formComponents()
              .eq(0)
              .find('.editor-form__button-group input:checked')
              .val()
          ).to.equal('M')
          expect(
            formComponents()
              .eq(0)
              .find('.editor-form__checkbox-container input')
              .prop('checked')
          ).to.equal(false)
        })
      })

      describe('textfield with repeating answers', () => {
        before(
          clickComponentMenuItem('Tekstikenttä'),
          setTextFieldValue(
            () => formComponents().eq(1).find('.editor-form__text-field'),
            'Ensimmäinen kysymys, toistuvilla arvoilla'
          ),
          clickRepeatingAnswers('Ensimmäinen kysymys, toistuvilla arvoilla')
        )
        it('has expected contents', () => {
          expect(formComponents()).to.have.length(2)
          expect(
            formComponents().eq(1).find('.editor-form__text-field').val()
          ).to.equal('Ensimmäinen kysymys, toistuvilla arvoilla')
          expect(
            formComponents()
              .eq(1)
              .find('.editor-form__button-group input:checked')
              .val()
          ).to.equal('M')
          expect(
            formComponents()
              .eq(1)
              .find('.editor-form__checkbox-container input')
              .eq(1)
              .prop('checked')
          ).to.equal(true)
        })
      })

      describe('textarea', () => {
        before(
          clickComponentMenuItem('Tekstialue'),
          clickElement(() =>
            formComponents()
              .eq(2)
              .find('.editor-form__button-group div:eq(2) label')
          ),
          clickElement(() =>
            formComponents().eq(2).find('.editor-form__checkbox-wrapper label')
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(2)
                .find('.editor-form__text-field-auto-width'),
            '2000'
          ),
          setTextFieldValue(
            () => formComponents().eq(2).find('.editor-form__text-field'),
            'Toinen kysymys'
          ),
          clickElement(() =>
            formComponents()
              .eq(2)
              .find('.editor-form__info-addon-checkbox label')
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(2)
                .find('.editor-form__info-addon-inputs textarea'),
            'Toisen kysymyksen ohjeteksti'
          )
        )
        it('has expected contents', () => {
          expect(formComponents()).to.have.length(3)
          expect(
            formComponents().eq(2).find('.editor-form__text-field').val()
          ).to.equal('Toinen kysymys')
          expect(
            formComponents()
              .eq(2)
              .find('.editor-form__info-addon-checkbox input')
              .prop('checked')
          ).to.equal(true)
          expect(
            formComponents()
              .eq(2)
              .find('.editor-form__info-addon-inputs textarea')
              .val()
          ).to.equal('Toisen kysymyksen ohjeteksti')
          expect(
            formComponents()
              .eq(2)
              .find('.editor-form__button-group input:checked')
              .val()
          ).to.equal('L')
          expect(
            formComponents()
              .eq(2)
              .find('.editor-form__max-length-container input')
              .val()
          ).to.equal('2000')
          expect(
            formComponents()
              .eq(2)
              .find('.editor-form__checkbox-container input')
              .prop('checked')
          ).to.equal(true)
        })
      })

      describe('dropdown', () => {
        before(
          clickComponentMenuItem('Pudotusvalikko'),
          setTextFieldValue(
            () => formComponents().eq(3).find('.editor-form__text-field').eq(0),
            'Kolmas kysymys'
          ),
          setTextFieldValue(
            () => formComponents().eq(3).find('.editor-form__text-field:last'),
            'Ensimmäinen vaihtoehto'
          ),
          clickElement(() =>
            formComponents()
              .eq(3)
              .find('.editor-form__multi-options-arrow--up:last')
          ),
          setTextFieldValue(
            () => formComponents().eq(3).find('.editor-form__text-field:last'),
            'Toinen vaihtoehto'
          ),
          clickElement(() =>
            formComponents().eq(3).find('.editor-form__add-dropdown-item a')
          ),
          setTextFieldValue(
            () => formComponents().eq(3).find('.editor-form__text-field:last'),
            'Kolmas vaihtoehto'
          ),
          clickElement(() =>
            formComponents().eq(3).find('.editor-form__add-dropdown-item a')
          ),
          clickElement(() =>
            formComponents()
              .eq(3)
              .find('.editor-form__info-addon-checkbox label')
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(3)
                .find('.editor-form__info-addon-inputs textarea'),
            'Kolmannen kysymyksen ohjeteksti'
          ),
          clickElement(() =>
            formComponents()
              .eq(3)
              .find(
                '.editor-form__followup-question:eq(2) a:contains("Lisäkysymykset")'
              )
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents().eq(3)
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(3)
                .find(
                  '.editor-form__followup-question-overlay input.editor-form__text-field'
                ),
            'Jatkokysymys'
          )
        )
        it('has expected contents', () => {
          expect(formComponents()).to.have.length(4)
          expect(
            formComponents().eq(3).find('.editor-form__text-field:first').val()
          ).to.equal('Kolmas kysymys')
          expect(
            formComponents()
              .eq(3)
              .find('.editor-form__checkbox-container input')
              .prop('checked')
          ).to.equal(false)
          expect(
            formComponents()
              .eq(3)
              .find('.editor-form__multi-options-container input')
              .not('.editor-form__followup-question-overlay input').length
          ).to.equal(4)
          const options = _.map(
            formComponents()
              .eq(3)
              .find('.editor-form__multi-options-container input')
              .not('.editor-form__followup-question-overlay input'),
            (inputField) => $(inputField).val()
          )
          expect(options).to.eql([
            'Ensimmäinen vaihtoehto',
            'Toinen vaihtoehto',
            'Kolmas vaihtoehto',
            '',
          ])
          expect(
            formComponents()
              .eq(3)
              .find('.editor-form__info-addon-checkbox input')
              .prop('checked')
          ).to.equal(true)
          expect(
            formComponents()
              .eq(3)
              .find('.editor-form__info-addon-inputs textarea')
              .val()
          ).to.equal('Kolmannen kysymyksen ohjeteksti')
          expect(
            formComponents()
              .eq(3)
              .find(
                '.editor-form__followup-question-overlay input.editor-form__text-field'
              )
              .val()
          ).to.equal('Jatkokysymys')
        })
      })

      describe('dropdown from koodisto', () => {
        before(
          clickComponentMenuItem('Pudotusvalikko, koodisto'),
          setTextFieldValue(
            () => formComponents().eq(4).find('.editor-form__text-field'),
            'Neljäs kysymys'
          ),
          () => {
            const e = formComponents()
              .eq(4)
              .find('.editor-form__select-koodisto-dropdown')
            e.val('pohjakoulutuseditori')
            triggerEvent(e, 'change')
            return
          }
        )
        it('selected correctly', () => {
          expect(formComponents()).to.have.length(5)
          expect(
            formComponents()
              .eq(4)
              .find('.editor-form__select-koodisto-dropdown')
              .val()
          ).to.equal('pohjakoulutuseditori')
        })
      })

      describe('multiple choice', () => {
        before(
          clickComponentMenuItem('Lista, monta valittavissa'),
          setTextFieldValue(
            () => formComponents().eq(5).find('.editor-form__text-field').eq(0),
            'Viides kysymys'
          ),
          clickElement(() =>
            formComponents().eq(5).find('.editor-form__add-dropdown-item a')
          ),
          setTextFieldValue(
            () => formComponents().eq(5).find('.editor-form__text-field:last'),
            'Ensimmäinen vaihtoehto'
          ),
          clickElement(() =>
            formComponents().eq(5).find('.editor-form__add-dropdown-item a')
          ),
          setTextFieldValue(
            () => formComponents().eq(5).find('.editor-form__text-field:last'),
            'Toinen vaihtoehto'
          ),
          clickElement(() =>
            formComponents().eq(5).find('.editor-form__add-dropdown-item a')
          ),
          setTextFieldValue(
            () => formComponents().eq(5).find('.editor-form__text-field:last'),
            'Kolmas vaihtoehto'
          ),
          clickElement(() =>
            formComponents().eq(5).find('.editor-form__add-dropdown-item a')
          ),
          clickElement(() =>
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question:eq(1) a:contains("Lisäkysymykset")'
              )
          ),
          clickSubComponentMenuItem('Painikkeet, yksi valittavissa', () =>
            formComponents().eq(5)
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(5)
                .find(
                  '.editor-form__followup-question-overlay input.editor-form__text-field'
                ),
            'Oletko punavihervärisokea?'
          ),
          clickElement(() =>
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")'
              )
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(5)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(0)'
                ),
            'Kyllä'
          ),
          clickElement(() =>
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")'
              )
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(5)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(1)'
                ),
            'En'
          ),
          clickElement(() =>
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__checkbox + .editor-form__checkbox-label:first'
              )
          ),
          clickSubComponentMenuItem('Vierekkäiset tekstikentät', () =>
            formComponents().eq(5)
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(5)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__text-field'
                )
                .eq(3),
            'Vierekkäinen tekstikenttä monivalinnan jatkokysymyksenä'
          ),
          clickElement(() =>
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__checkbox + label.editor-form__checkbox-label:contains("Vastaaja voi lisätä useita vastauksia")'
              )
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container'
              )
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(5)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
                )
                .eq(0),
            'Jatkokysymys A'
          ),
          clickElement(() =>
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
              )
              .eq(0)
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container'
              )
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(5)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
                )
                .eq(1),
            'Jatkokysymys B'
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container'
              )
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(5)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
                )
                .eq(2),
            'Jatkokysymys C'
          ),
          clickElement(() =>
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
              )
              .eq(2)
          )
        )
        it('has expected contents', () => {
          expect(formComponents()).to.have.length(6)
          expect(
            formComponents().eq(5).find('.editor-form__text-field:first').val()
          ).to.equal('Viides kysymys')
          expect(
            formComponents()
              .eq(5)
              .find('.editor-form__checkbox-container input')
              .prop('checked')
          ).to.equal(false)
          expect(
            formComponents()
              .eq(5)
              .find('.editor-form__multi-option-wrapper input')
              .not('.editor-form__followup-question-overlay input').length
          ).to.equal(4)
          const options = _.map(
            formComponents()
              .eq(5)
              .find('.editor-form__multi-option-wrapper input')
              .not('.editor-form__followup-question-overlay input'),
            (inputField) => $(inputField).val()
          )
          expect(options).to.eql([
            'Ensimmäinen vaihtoehto',
            'Toinen vaihtoehto',
            'Kolmas vaihtoehto',
            '',
          ])
          expect(
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__text-field'
              )
              .eq(3)
              .val()
          ).to.equal('Vierekkäinen tekstikenttä monivalinnan jatkokysymyksenä')
          expect(
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
              )
              .eq(0)
              .val()
          ).to.equal('Jatkokysymys A')
          expect(
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
              )
              .eq(1)
              .val()
          ).to.equal('Jatkokysymys B')
          expect(
            formComponents()
              .eq(5)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
              )
              .eq(2)
              .val()
          ).to.equal('Jatkokysymys C')
        })
      })

      describe('multiple choice from koodisto', () => {
        before(
          clickComponentMenuItem('Lista, monta valittavissa, koodisto'),
          setTextFieldValue(
            () => formComponents().eq(6).find('.editor-form__text-field'),
            'Kuudes kysymys'
          ),
          () => {
            const e = formComponents()
              .eq(6)
              .find('.editor-form__select-koodisto-dropdown')
            e.val('tutkinto')
            triggerEvent(e, 'change')
            return
          }
        )
        it('selected correctly', () => {
          expect(formComponents()).to.have.length(7)
          expect(
            formComponents()
              .eq(6)
              .find('.editor-form__select-koodisto-dropdown')
              .val()
          ).to.equal('tutkinto')
        })
      })

      describe('section with contents', () => {
        before(
          clickComponentMenuItem('Lomakeosio'),
          setTextFieldValue(
            () => formSections().eq(0).find('.editor-form__text-field').eq(0),
            'Testiosio'
          ),
          clickSubComponentMenuItem('Tekstialue', () => formSections().eq(0)),
          clickElement(() =>
            formSections().eq(0).find('.editor-form__checkbox-wrapper label')
          ),
          setTextFieldValue(
            () => formSections().eq(0).find('.editor-form__text-field').eq(1),
            'Osiokysymys'
          ),
          clickElement(() =>
            formSections()
              .eq(0)
              .find('.editor-form__button-group div:eq(0) label')
          )
        )
        it('has expected contents', () => {
          expect(formComponents()).to.have.length(9)
          expect(
            formSections().eq(0).find('.editor-form__text-field').eq(0).val()
          ).to.equal('Testiosio')
          expect(
            formSections().eq(0).find('.editor-form__text-field').eq(1).val()
          ).to.equal('Osiokysymys')
          expect(
            formSections()
              .eq(0)
              .find('.editor-form__button-group input:checked')
              .val()
          ).to.equal('S')
          expect(
            formSections()
              .eq(0)
              .find('.editor-form__checkbox-container input')
              .prop('checked')
          ).to.equal(true)
        })
      })

      describe('textfield with info text', () => {
        before(
          clickComponentMenuItem('Tekstikenttä'),
          clickInfoTextCheckbox(() => formComponents().eq(9)),
          setTextFieldValue(
            () => formComponents().eq(9).find('.editor-form__text-field'),
            'Infoteksti'
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(9)
                .find('.editor-form__info-addon-inputs textarea')
                .eq(0),
            'oikeen pitka infoteksti sitten tassa.'
          )
        )

        it('has expected contents', () => {
          expect(formComponents()).to.have.length(10)
          expect(
            formComponents()
              .eq(9)
              .find('.editor-form__info-addon-checkbox input')
              .prop('checked')
          ).to.equal(true)
          expect(
            formComponents()
              .eq(9)
              .find('.editor-form__info-addon-inputs textarea')
              .eq(0)
              .val()
          ).to.equal('oikeen pitka infoteksti sitten tassa.')
        })
      })

      /*
       * This field is not supposed to be filled in the application tests, they should ignore it and submitting should
       * work because this is optional. This was added because of regression: optional dropdown failed the server-side
       * validation.
       */
      describe('second dropdown from koodisto (optional)', () => {
        before(
          clickComponentMenuItem('Pudotusvalikko, koodisto'),
          setTextFieldValue(
            () => formComponents().eq(10).find('.editor-form__text-field'),
            'Viimeinen kysymys'
          ),
          () => {
            const e = formComponents()
              .eq(10)
              .find('.editor-form__select-koodisto-dropdown')
            e.val('tutkinto')
            triggerEvent(e, 'change')
            return
          }
        )
        it('selected correctly', () => {
          expect(formComponents()).to.have.length(11)
          expect(
            formComponents()
              .eq(10)
              .find('.editor-form__select-koodisto-dropdown')
              .val()
          ).to.equal('tutkinto')
        })
      })

      describe('semantic radio button', () => {
        before(
          clickComponentMenuItem('Painikkeet, yksi valittavissa'),
          setTextFieldValue(
            () => formComponents().eq(11).find('.editor-form__text-field'),
            'Lyhyen listan kysymys'
          ),
          clickElement(() =>
            formComponents()
              .eq(11)
              .find('.editor-form__checkbox-wrapper label:first')
          ),
          clickElement(() =>
            formComponents().eq(11).find('.editor-form__add-dropdown-item a')
          ),
          setTextFieldValue(
            () => formComponents().eq(11).find('.editor-form__text-field:last'),
            'Ensimmäinen vaihtoehto'
          ),
          clickElement(() =>
            formComponents().eq(11).find('.editor-form__add-dropdown-item a')
          ),
          setTextFieldValue(
            () => formComponents().eq(11).find('.editor-form__text-field:last'),
            'Toinen vaihtoehto'
          ),
          clickElement(() =>
            formComponents()
              .eq(11)
              .find(
                '.editor-form__followup-question:eq(0) a:contains("Lisäkysymykset")'
              )
          ),
          clickSubComponentMenuItem('Lista, monta valittavissa', () =>
            formComponents().eq(11)
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(11)
                .find(
                  '.editor-form__followup-question-overlay input.editor-form__text-field'
                ),
            'Monivalinta jatkokysymyksenä'
          ),
          clickElement(() =>
            formComponents()
              .eq(11)
              .find(
                '.editor-form__followup-question-overlay .editor-form__checkbox + .editor-form__checkbox-label'
              )
          ),
          clickElement(() =>
            formComponents()
              .eq(11)
              .find(
                '.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")'
              )
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(11)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(0)'
                ),
            'Jatkokysymys A'
          ),
          clickElement(() =>
            formComponents()
              .eq(11)
              .find(
                '.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")'
              )
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(11)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(1)'
                ),
            'Jatkokysymys B'
          ),
          clickSubComponentMenuItem('Vierekkäiset tekstikentät', () =>
            formComponents().eq(11)
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(11)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__text-field'
                )
                .eq(3),
            'Vierekkäinen tekstikenttä painikkeiden jatkokysymyksenä'
          ),
          clickElement(() =>
            formComponents()
              .eq(11)
              .find(
                '.editor-form__followup-question-overlay .editor-form__checkbox + label.editor-form__checkbox-label:contains("Vastaaja voi lisätä useita vastauksia")'
              )
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents()
              .eq(11)
              .find('.editor-form__adjacent-fieldset-container')
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(11)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
                )
                .eq(0),
            'Jatkokysymys A'
          ),
          clickElement(() =>
            formComponents()
              .eq(11)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
              )
              .eq(0)
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents()
              .eq(11)
              .find('.editor-form__adjacent-fieldset-container')
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(11)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
                )
                .eq(1),
            'Jatkokysymys B'
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents()
              .eq(11)
              .find('.editor-form__adjacent-fieldset-container')
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(11)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
                )
                .eq(2),
            'Jatkokysymys C'
          ),
          clickElement(() =>
            formComponents()
              .eq(11)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
              )
              .eq(2)
          )
        )
        it('has expected contents', () => {
          expect(formComponents()).to.have.length(12)
          expect(
            formComponents().eq(11).find('.editor-form__text-field:first').val()
          ).to.equal('Lyhyen listan kysymys')
          expect(
            formComponents()
              .eq(11)
              .find('.editor-form__checkbox-container input')
              .prop('checked')
          ).to.equal(true)
          expect(
            formComponents()
              .eq(11)
              .find(
                '.editor-form__multi-options-container > div:nth-child(1) .editor-form__text-field'
              )
              .not('.editor-form__followup-question-overlay input')
              .val()
          ).to.equal('Ensimmäinen vaihtoehto')
          expect(
            formComponents()
              .eq(11)
              .find(
                '.editor-form__multi-options-container > div:nth-child(2) .editor-form__text-field'
              )
              .not('.editor-form__followup-question-overlay input')
              .val()
          ).to.equal('Toinen vaihtoehto')
          expect(
            formComponents()
              .eq(11)
              .find(
                '.editor-form__followup-question-overlay .editor-form__text-field'
              )
              .eq(3)
              .val()
          ).to.equal('Vierekkäinen tekstikenttä painikkeiden jatkokysymyksenä')
          expect(
            formComponents()
              .eq(11)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
              )
              .eq(0)
              .val()
          ).to.equal('Jatkokysymys A')
          expect(
            formComponents()
              .eq(11)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
              )
              .eq(1)
              .val()
          ).to.equal('Jatkokysymys B')
          expect(
            formComponents()
              .eq(11)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
              )
              .eq(2)
              .val()
          ).to.equal('Jatkokysymys C')
        })
      })

      describe('adjacent fields', () => {
        before(
          clickComponentMenuItem('Vierekkäiset tekstikentät'),
          setTextFieldValue(
            () => formComponents().eq(12).find('.editor-form__text-field'),
            'Vierekkäinen tekstikenttä'
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents().eq(12)
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(12)
                .find(
                  '.editor-form__adjacent-fieldset-container .editor-form__text-field'
                ),
            'Tekstikenttä 1'
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents().eq(12)
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(12)
                .find(
                  '.editor-form__adjacent-fieldset-container .editor-form__text-field'
                )
                .eq(1),
            'Tekstikenttä 2'
          )
        )
        it('🌸  is working so wonderfully 🌸', () => {})
      })

      describe('dropdown with adjacent fields as followup', () => {
        before(
          clickComponentMenuItem('Pudotusvalikko'),
          setTextFieldValue(
            () =>
              formComponents().eq(15).find('.editor-form__text-field').eq(0),
            'Päätason pudotusvalikko'
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(15)
                .find(
                  '.editor-form__multi-options-wrapper-outer .editor-form__text-field'
                )
                .eq(0),
            'Pudotusvalikon 1. kysymys'
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(15)
                .find(
                  '.editor-form__multi-options-wrapper-outer .editor-form__text-field'
                )
                .eq(1),
            'Pudotusvalikon 2. kysymys'
          ),
          clickElement(() =>
            formComponents()
              .eq(15)
              .find(
                '.editor-form__multi-options-container a:contains("Lisäkysymykset")'
              )
          ),
          clickSubComponentMenuItem('Vierekkäiset tekstikentät', () =>
            formComponents().eq(15)
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(15)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__text-field'
                ),
            'Vierekkäinen tekstikenttä jatkokysymyksenä'
          ),
          clickElement(() =>
            formComponents()
              .eq(15)
              .find(
                '.editor-form__followup-question-overlay .editor-form__checkbox + label.editor-form__checkbox-label:contains("Vastaaja voi lisätä useita vastauksia")'
              )
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents()
              .eq(15)
              .find('.editor-form__adjacent-fieldset-container')
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(15)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
                )
                .eq(0),
            'Jatkokysymys A'
          ),
          clickElement(() =>
            formComponents()
              .eq(15)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
              )
              .eq(0)
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents()
              .eq(15)
              .find('.editor-form__adjacent-fieldset-container')
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(15)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
                )
                .eq(1),
            'Jatkokysymys B'
          ),
          clickSubComponentMenuItem('Tekstikenttä', () =>
            formComponents()
              .eq(15)
              .find('.editor-form__adjacent-fieldset-container')
          ),
          setTextFieldValue(
            () =>
              formComponents()
                .eq(15)
                .find(
                  '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
                )
                .eq(2),
            'Jatkokysymys C'
          ),
          clickElement(() =>
            formComponents()
              .eq(15)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
              )
              .eq(2)
          )
        )
        it('has expected contents', () => {
          expect(formComponents()).to.have.length(16)
          expect(
            formComponents().eq(15).find('.editor-form__text-field:first').val()
          ).to.equal('Päätason pudotusvalikko')
          expect(
            formComponents()
              .eq(15)
              .find(
                '.editor-form__multi-options-wrapper-outer .editor-form__text-field'
              )
              .eq(0)
              .val()
          ).to.equal('Pudotusvalikon 1. kysymys')
          expect(
            formComponents()
              .eq(15)
              .find(
                '.editor-form__multi-options-wrapper-outer .editor-form__text-field'
              )
              .eq(1)
              .val()
          ).to.equal('Pudotusvalikon 2. kysymys')
          expect(
            formComponents()
              .eq(15)
              .find(
                '.editor-form__followup-question-overlay .editor-form__text-field'
              )
              .eq(0)
              .val()
          ).to.equal('Vierekkäinen tekstikenttä jatkokysymyksenä')
          expect(
            formComponents()
              .eq(15)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
              )
              .eq(0)
              .val()
          ).to.equal('Jatkokysymys A')
          expect(
            formComponents()
              .eq(15)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
              )
              .eq(1)
              .val()
          ).to.equal('Jatkokysymys B')
          expect(
            formComponents()
              .eq(15)
              .find(
                '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
              )
              .eq(2)
              .val()
          ).to.equal('Jatkokysymys C')
        })
      })

      describe('numeric textfield', () => {
        before(
          clickComponentMenuItem('Tekstikenttä'),
          setTextFieldValue(
            () => formComponents().eq(16).find('.editor-form__text-field'),
            'Tekstikenttä numeerisilla arvoilla'
          ),
          clickNumericAnswer('Tekstikenttä numeerisilla arvoilla'),
          () => {
            formComponents().eq(16).find('option').eq(4).prop('selected', true)
            triggerEvent(formComponents().eq(16).find('select'), 'change')
          }
        )
        it('has expected contents', () => {
          expect(formComponents()).to.have.length(17)
          expect(
            formComponents().eq(16).find('.editor-form__text-field').val()
          ).to.equal('Tekstikenttä numeerisilla arvoilla')
          expect(
            formComponents()
              .eq(16)
              .find('.editor-form__checkbox-container input')
              .eq(2)
              .prop('checked')
          ).to.equal(true)
          expect(
            formComponents().eq(16).find('select')[0].selectedIndex
          ).to.equal(4)
        })
      })

      describe('dropdown from koodisto, with invalid options', () => {
        before(
          clickComponentMenuItem('Pudotusvalikko, koodisto'),
          setTextFieldValue(
            () => formComponents().eq(17).find('.editor-form__text-field'),
            'Alasvetovalikko, koodisto, päättyneet'
          ),
          () => {
            const e = formComponents()
              .eq(17)
              .find('.editor-form__select-koodisto-dropdown')
            e.val('maatjavaltiot2')
            triggerEvent(e, 'change')
            return
          },
          clickElement(() =>
            formComponents()
              .eq(17)
              .find(
                '.editor-form__checkbox + label:contains("Sisällytä päättyneet koodit")'
              )
          ),
          clickElement(() =>
            formComponents().eq(17).find('.editor-form__show-koodisto-values a')
          ),
          wait.until(() =>
            elementExists(
              formComponents()
                .eq(17)
                .find('.editor-form__koodisto-field:contains("Suomi")')
            )
          )
        )
        it('selected correctly', () => {
          expect(formComponents()).to.have.length(18)
          expect(
            formComponents()
              .eq(17)
              .find('.editor-form__select-koodisto-dropdown')
              .val()
          ).to.equal('maatjavaltiot2')
          expect(
            formComponents()
              .eq(17)
              .find(
                '.editor-form__checkbox + label:contains("Sisällytä päättyneet koodit")'
              )
              .siblings()
              .prop('checked')
          ).to.equal(true)
          expect(
            elementExists(
              formComponents()
                .eq(17)
                .find(
                  '.editor-form__koodisto-field:contains("Entinen Neuvostoliitto")'
                )
            )
          ).to.equal(true)
        })
      })

      describe('locking form', () => {
        before(
          clickLomakeForEdit('Testilomake'),
          wait.forMilliseconds(1000), // wait abit since
          clickLockForm(), // this locking is sometimes so fast that the previous request gets blocked.
          wait.until(() =>
            elementExists(testFrame().find('.editor-form__form-editing-locked'))
          ),
          wait.until(() => {
            return getInputs(':enabled').length === 0
          })
        )
        it('all inputs are locked', () => {
          expect(getInputs(':disabled').length).to.equal(getInputs('').length)
          expect(getInputs(':enabled').length).to.equal(0)
          expect(getRemoveElementButtons(':disabled').length).to.equal(
            getRemoveElementButtons('').length
          )
          expect(getRemoveElementButtons(':enabled').length).to.equal(0)
          expect(
            testFrame().find(
              '.editor-form__add-component-toolbar .plus-component--disabled'
            ).length
          ).to.equal(1)
          expect(
            elementExists(testFrame().find('.editor-form__form-editing-locked'))
          ).to.equal(true)
        })
      })

      describe('releasing form lock', () => {
        before(
          clickLockForm(),
          wait.until(
            () =>
              !elementExists(
                testFrame().find('.editor-form__form-editing-locked')
              )
          )
        )
        it('all inputs are unlocked', () => {
          expect(getInputs(':disabled').length).to.equal(0)
          expect(getInputs(':enabled').length).to.equal(getInputs('').length)
          expect(
            testFrame().find(
              '.editor-form__add-component-toolbar .plus-component--disabled'
            ).length
          ).to.equal(0)
          expect(
            elementExists(testFrame().find('.editor-form__form-editing-locked'))
          ).to.equal(false)
        })
      })

      describe('autosave', () => {
        before(
          wait.until(() => {
            const flasher = testFrame().find('.top-banner .flasher')
            return (
              flasher.css('opacity') !== '0' &&
              flasher.find('span:visible').text() ===
                'Kaikki muutokset tallennettu'
            )
          })
        )
        it('notification shows success', () => {
          expect(testFrame().find('.top-banner .flasher span').text()).to.equal(
            'Kaikki muutokset tallennettu'
          )
        })
      })
    })

    describe('Copy from a form and paste into another after closing the first form', () => {
      before(
        clickLomakeForEdit('Testilomake'),
        wait.forMilliseconds(1000),
        clickCopyFormComponent('Testiosio'),
        wait.forMilliseconds(1000),
        clickCloseDetailsButton(),
        wait.forMilliseconds(500),
        clickLomakeForEdit('Selaintestilomake4'),
        wait.forMilliseconds(1000),
        clickPasteFormComponent(0),
        wait.forMilliseconds(500)
      )
      it('creates the copy in another form', () => {
        expect(
          formSections().eq(0).find('.editor-form__text-field').eq(0).val()
        ).to.equal('Testiosio')
      })
    })

    describe('hakukohde specific question', () => {
      const component = () => formComponents().eq(0)
      before(
        clickLomakeForEdit('belongs-to-hakukohteet-test-form'),
        wait.forMilliseconds(1000),
        clickElement(() =>
          component().find('.editor-form__component-fold-button')
        ),
        clickElement(() =>
          component().find('.belongs-to-hakukohteet__modal-toggle')
        ),
        wait.forMilliseconds(1000),
        clickElement(() =>
          component().find(
            '.hakukohde-and-hakukohderyhma-category-list-item:first'
          )
        )
      )
      it('shows the selected hakukohde', () => {
        expect(
          component().find('.belongs-to-hakukohteet__hakukohde-label').length
        ).to.equal(1)
      })
    })
  })
})()
