(function() {
  function addNewFormLink() {
    return testFrame().find('.editor-form__control-button--enabled')
  }

  function formTitleField () {
    return testFrame().find('.editor-form__form-name-input')
  }

  function formList() {
    return testFrame().find('.editor-form__list')
  }

  function editorPageIsLoaded() {
    return elementExists(formList().find('a'))
  }

  function formListItems(n) {
    if ($.isNumeric(n)) {
      return formList().find('a').eq(n)
    } else {
      return formList().find('a')
    }
  }

  function personInfoModule() {
    return testFrame()
      .find(".editor-form__module-wrapper header:contains('Henkilötiedot')");
  }

  function formComponents() {
    return testFrame().find('.editor-form__component-wrapper')
      // exclude followup question components
      .not('.editor-form__followup-question-overlay .editor-form__component-wrapper')
  }

  function formSections() {
    return testFrame().find('.editor-form__section_wrapper')
  }

  function clickComponentMenuItem(title) {
    function menuItem() { return testFrame().find('.editor-form > .editor-form__add-component-toolbar a:contains("'+ title +'")') }
    return clickElement(menuItem)
  }

  function clickSubComponentMenuItem(title, element) {
    function menuItem() {
      return element().find('.editor-form__add-component-toolbar a:contains("'+ title +'")') }
    return clickElement(menuItem)
  }

  function clickRepeatingAnswers(question) {
    return function() {
      return testFrame()
        .find("input.editor-form__text-field")
        .filter(function() {
          return this.value === question
        })
        .parent().parent().parent()
        .find(".editor-form__checkbox-wrapper label:contains('Vastaaja voi')")
        .prev().click()
    }
  }

  function clickInfoTextCheckbox(selector) {
    return function() {
      return selector()
        .find(".editor-form__info-addon-checkbox > input")
        .click()
    }
  }

  before(function () {
    loadInFrame('http://localhost:8350/lomake-editori/')
  })

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('Editor', function() {

    describe('with fixture forms', function() {
      before(
        wait.until(editorPageIsLoaded)
      )
      it('has 4 fixture forms', function() {
        expect(formListItems()).to.have.length(4)
      })
    })

    describe('form creation', function() {
      before(
        clickElement(addNewFormLink),
        wait.forMilliseconds(1000), // TODO: fix form refresh in frontend so that this isn't required (or check that no AJAX requests are ongoing)
        setTextFieldValue(formTitleField, 'Testilomake'),
        wait.until(function() {
          return formListItems(0).find('span:eq(0)').text() === 'Testilomake'
        })
      )
      it('creates blank form', function () {
        expect(formTitleField().val()).to.equal('Testilomake')
        expect(formComponents()).to.have.length(0)
      })
      it('has person info module', function() {
        expect(personInfoModule()).to.have.length(1)
      })
    })

    describe('adding elements:', function() {
      describe('textfield', function() {
        before(
          clickComponentMenuItem('Tekstikenttä'),
          setTextFieldValue(function() { return formComponents().eq(0).find('.editor-form__text-field') }, 'Ensimmäinen kysymys'),
          clickElement(function() { return formComponents().eq(0).find('.editor-form__info-addon-checkbox label') }),
          setTextFieldValue(function() { return formComponents().eq(0).find('.editor-form__info-addon-inputs input') }, 'Ensimmäisen kysymyksen ohjeteksti')
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(1)
          expect(formComponents().eq(0).find('.editor-form__text-field').val()).to.equal('Ensimmäinen kysymys')
          expect(formComponents().eq(0).find('.editor-form__info-addon-checkbox input').prop('checked')).to.equal(true)
          expect(formComponents().eq(0).find('.editor-form__info-addon-inputs input').val()).to.equal('Ensimmäisen kysymyksen ohjeteksti')
          expect(formComponents().eq(0).find('.editor-form__button-group input:checked').val()).to.equal('M')
          expect(formComponents().eq(0).find('.editor-form__checkbox-container input').prop('checked')).to.equal(false)
        })
      })

      describe('textfield with repeating answers', function() {
        before(
          clickComponentMenuItem('Tekstikenttä'),
          setTextFieldValue(function() { return formComponents().eq(1).find('.editor-form__text-field') }, 'Ensimmäinen kysymys, toistuvilla arvoilla'),
          clickRepeatingAnswers('Ensimmäinen kysymys, toistuvilla arvoilla')
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(2)
          expect(formComponents().eq(1).find('.editor-form__text-field').val()).to.equal('Ensimmäinen kysymys, toistuvilla arvoilla')
          expect(formComponents().eq(1).find('.editor-form__button-group input:checked').val()).to.equal('M')
          expect(formComponents().eq(1).find('.editor-form__checkbox-container input').eq(1).prop('checked')).to.equal(true)
        })
      })

      describe('textarea', function() {
        before(
          clickComponentMenuItem('Tekstialue'),
          clickElement(function() { return formComponents().eq(2).find('.editor-form__button-group div:eq(2) label')}),
          clickElement(function() { return formComponents().eq(2).find('.editor-form__checkbox-wrapper label')}),
          setTextFieldValue(function() { return formComponents().eq(2).find('.editor-form__text-field')}, 'Toinen kysymys'),
          clickElement(function() { return formComponents().eq(2).find('.editor-form__info-addon-checkbox label') }),
          setTextFieldValue(function() { return formComponents().eq(2).find('.editor-form__info-addon-inputs input') }, 'Toisen kysymyksen ohjeteksti')
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(3)
          expect(formComponents().eq(2).find('.editor-form__text-field').val()).to.equal('Toinen kysymys')
          expect(formComponents().eq(2).find('.editor-form__info-addon-checkbox input').prop('checked')).to.equal(true)
          expect(formComponents().eq(2).find('.editor-form__info-addon-inputs input').val()).to.equal('Toisen kysymyksen ohjeteksti')
          expect(formComponents().eq(2).find('.editor-form__button-group input:checked').val()).to.equal('L')
          expect(formComponents().eq(2).find('.editor-form__checkbox-container input').prop('checked')).to.equal(true)
        })
      })

      describe('dropdown', function() {
        before(
          clickComponentMenuItem('Pudotusvalikko'),
          setTextFieldValue(function() { return formComponents().eq(3).find('.editor-form__text-field').eq(0)}, 'Kolmas kysymys'),
          setTextFieldValue(function() { return formComponents().eq(3).find('.editor-form__text-field:last')}, 'Ensimmäinen vaihtoehto'),
          clickElement(function() { return formComponents().eq(3).find('.editor-form__add-dropdown-item a') }),
          setTextFieldValue(function() { return formComponents().eq(3).find('.editor-form__text-field:last')}, 'Toinen vaihtoehto'),
          clickElement(function() { return formComponents().eq(3).find('.editor-form__add-dropdown-item a') }),
          setTextFieldValue(function() { return formComponents().eq(3).find('.editor-form__text-field:last')}, 'Kolmas vaihtoehto'),
          clickElement(function() { return formComponents().eq(3).find('.editor-form__add-dropdown-item a') }),
          clickElement(function() { return formComponents().eq(3).find('.editor-form__info-addon-checkbox label') }),
          setTextFieldValue(function() { return formComponents().eq(3).find('.editor-form__info-addon-inputs input') }, 'Kolmannen kysymyksen ohjeteksti'),
          clickElement(function() { return formComponents().eq(3).find('.editor-form__followup-question:eq(2) a') }),
          clickElement(function() { return formComponents().eq(3).find('.editor-form__followup-question-overlay a:contains("Tekstikenttä")') }),
          setTextFieldValue(function() { return formComponents().eq(3).find('.editor-form__followup-question-overlay input.editor-form__text-field') }, "Jatkokysymys")
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(4)
          expect(formComponents().eq(3).find('.editor-form__text-field:first').val()).to.equal('Kolmas kysymys')
          expect(formComponents().eq(3).find('.editor-form__checkbox-container input').prop('checked')).to.equal(false)
          expect(formComponents().eq(3).find('.editor-form__multi-options-container input').not('.editor-form__followup-question-overlay input').length).to.equal(4)
          var options = _.map(formComponents().eq(3).find('.editor-form__multi-options-container input').not('.editor-form__followup-question-overlay input'), function(inputField) {
            return $(inputField).val()
          })
          expect(options).to.eql(["Ensimmäinen vaihtoehto", "Toinen vaihtoehto", "Kolmas vaihtoehto", ""])
          expect(formComponents().eq(3).find('.editor-form__info-addon-checkbox input').prop('checked')).to.equal(true)
          expect(formComponents().eq(3).find('.editor-form__info-addon-inputs input').val()).to.equal('Kolmannen kysymyksen ohjeteksti')
          expect(formComponents().eq(3).find('.editor-form__followup-question-overlay input.editor-form__text-field').val()).to.equal("Jatkokysymys")
        })
      })

      describe('dropdown from koodisto', function() {
        before(
          clickComponentMenuItem('Pudotusvalikko'),
          setTextFieldValue(function() { return formComponents().eq(4).find('.editor-form__text-field')}, 'Neljäs kysymys'),
          clickElement(function() { return formComponents().eq(4).find('.editor-form__multi-options_wrapper label:contains("Koodisto")')}),
          clickElement(function() { return formComponents().eq(4).find('.editor-form__koodisto-popover a:contains("Pohjakoulutus")') })
        )
        it('selected correctly', function() {
          expect(formComponents()).to.have.length(5)
          expect(formComponents().eq(4).find('.editor-form__multi-options_wrapper label:eq(1)').text()).to.equal("Koodisto: Pohjakoulutus")
        })
      })

      describe('multiple choice', function() {
        before(
          clickComponentMenuItem('Lista, monta valittavissa'),
          setTextFieldValue(function () { return formComponents().eq(5).find('.editor-form__text-field').eq(0) }, 'Viides kysymys'),
          clickElement(function () { return formComponents().eq(5).find('.editor-form__add-dropdown-item a') }),
          setTextFieldValue(function () { return formComponents().eq(5).find('.editor-form__text-field:last') }, 'Ensimmäinen vaihtoehto'),
          clickElement(function () { return formComponents().eq(5).find('.editor-form__add-dropdown-item a') }),
          setTextFieldValue(function () { return formComponents().eq(5).find('.editor-form__text-field:last') }, 'Toinen vaihtoehto'),
          clickElement(function () { return formComponents().eq(5).find('.editor-form__add-dropdown-item a') }),
          setTextFieldValue(function () { return formComponents().eq(5).find('.editor-form__text-field:last') }, 'Kolmas vaihtoehto'),
          clickElement(function () { return formComponents().eq(5).find('.editor-form__add-dropdown-item a') }),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question:eq(1) a') }),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay a:contains("Painikkeet, yksi valittavissa")') }),
          setTextFieldValue(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay input.editor-form__text-field') }, "Oletko punavihervärisokea?"),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")') }),
          setTextFieldValue(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(0)') }, 'Kyllä'),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")') }),
          setTextFieldValue(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(1)') }, 'En'),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__checkbox + .editor-form__checkbox-label') }),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay a:contains("Vierekkäiset tekstikentät")') }),
          setTextFieldValue(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__text-field').eq(3) }, 'Vierekkäinen tekstikenttä monivalinnan jatkokysymyksenä'),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__checkbox + label.editor-form__checkbox-label:contains("Vastaaja voi lisätä useita vastauksia")') }),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container a:contains("Tekstikenttä")') }),
          setTextFieldValue(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(0) }, 'Jatkokysymys A'),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")').eq(0) }),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container a:contains("Tekstikenttä")') }),
          setTextFieldValue(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(1) }, 'Jatkokysymys B'),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container a:contains("Tekstikenttä")') }),
          setTextFieldValue(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(2) }, 'Jatkokysymys C'),
          clickElement(function() { return formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")').eq(2) })
        )
        it('has expected contents', function () {
          expect(formComponents()).to.have.length(6)
          expect(formComponents().eq(5).find('.editor-form__text-field:first').val()).to.equal('Viides kysymys')
          expect(formComponents().eq(5).find('.editor-form__checkbox-container input').prop('checked')).to.equal(false)
          expect(formComponents().eq(5).find('.editor-form__multi-option-wrapper input').not('.editor-form__followup-question-overlay input').length).to.equal(4)
          var options = _.map(formComponents().eq(5).find('.editor-form__multi-option-wrapper input').not('.editor-form__followup-question-overlay input'), function (inputField) {
            return $(inputField).val()
          })
          expect(options).to.eql(["Ensimmäinen vaihtoehto", "Toinen vaihtoehto", "Kolmas vaihtoehto", ""])
          expect(formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__text-field').eq(3).val()).to.equal('Vierekkäinen tekstikenttä monivalinnan jatkokysymyksenä')
          expect(formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(0).val()).to.equal('Jatkokysymys A')
          expect(formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(1).val()).to.equal('Jatkokysymys B')
          expect(formComponents().eq(5).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(2).val()).to.equal('Jatkokysymys C')
        })
      })

      describe('multiple choice from koodisto', function() {
        before(
          clickComponentMenuItem('Lista, monta valittavissa'),
          setTextFieldValue(function() { return formComponents().eq(6).find('.editor-form__text-field') }, 'Kuudes kysymys'),
          clickElement(function() { return formComponents().eq(6).find('.editor-form__multi-options_wrapper label:contains("Koodisto")') }),
          clickElement(function() { return formComponents().eq(6).find('.editor-form__koodisto-popover a:contains("Tutkinto")') })
        )
        it('selected correctly', function() {
          expect(formComponents()).to.have.length(7)
          expect(formComponents().eq(6).find('.editor-form__multi-options_wrapper label:eq(1)').text()).to.equal("Koodisto: Tutkinto")
        })
      })

      describe('section with contents', function() {
        before(
          clickComponentMenuItem('Lomakeosio'),
          setTextFieldValue(function() { return formSections().eq(0).find('.editor-form__text-field').eq(0) }, 'Testiosio'),
          clickElement(function() { return formSections().eq(0).find('.form__add-component-toolbar--list li a:contains("Tekstialue")') }),
          clickElement(function() { return formSections().eq(0).find('.editor-form__checkbox-wrapper label')}),
          setTextFieldValue(function() { return formSections().eq(0).find('.editor-form__text-field').eq(1) }, 'Osiokysymys'),
          clickElement(function() { return formSections().eq(0).find('.editor-form__button-group div:eq(0) label')})
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(9)
          expect(formSections().eq(0).find('.editor-form__text-field').eq(0).val()).to.equal('Testiosio')
          expect(formSections().eq(0).find('.editor-form__text-field').eq(1).val()).to.equal('Osiokysymys')
          expect(formSections().eq(0).find('.editor-form__button-group input:checked').val()).to.equal('S')
          expect(formSections().eq(0).find('.editor-form__checkbox-container input').prop('checked')).to.equal(true)
        })
      })

      describe('textfield with info text', function() {
        before(
          clickComponentMenuItem('Tekstikenttä'),
          clickInfoTextCheckbox(function() { return formComponents().eq(9) }),
          setTextFieldValue(function() { return formComponents().eq(9).find('.editor-form__text-field') }, 'Infoteksti'),
          setTextFieldValue(function() {
            return formComponents().eq(9).find('.editor-form__info-addon-inputs input').eq(0) }, 'oikeen pitka infoteksti sitten tassa.')
        )

        it('has expected contents', function() {
          expect(formComponents()).to.have.length(10)
          expect(formComponents().eq(9).find('.editor-form__info-addon-checkbox input').prop('checked')).to.equal(true)
          expect(formComponents().eq(9).find('.editor-form__info-addon-inputs input').eq(0).val()).to.equal('oikeen pitka infoteksti sitten tassa.')
        })
      })

      /*
       * This field is not supposed to be filled in the application tests, they should ignore it and submitting should
       * work because this is optional. This was added because of regression: optional dropdown failed the server-side
       * validation.
       */
      describe('second dropdown from koodisto (optional)', function() {
        before(
            clickComponentMenuItem('Pudotusvalikko'),
            setTextFieldValue(function() { return formComponents().eq(10).find('.editor-form__text-field')}, 'Viimeinen kysymys'),
            clickElement(function() { return formComponents().eq(10).find('.editor-form__multi-options_wrapper label:contains("Koodisto")')}),
            clickElement(function() { return formComponents().eq(10).find('.editor-form__koodisto-popover a:contains("Tutkinto")') })
        )
        it('selected correctly', function() {
          expect(formComponents()).to.have.length(11)
          expect(formComponents().eq(10).find('.editor-form__multi-options_wrapper label:eq(1)').text()).to.equal("Koodisto: Tutkinto")
        })
      })

      describe('semantic radio button', function() {
        before(
          clickComponentMenuItem('Painikkeet, yksi valittavissa'),
          setTextFieldValue(function() { return formComponents().eq(11).find('.editor-form__text-field') }, 'Lyhyen listan kysymys'),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__checkbox-wrapper label') }),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__add-dropdown-item a') }),
          setTextFieldValue(function () { return formComponents().eq(11).find('.editor-form__text-field:last') }, 'Ensimmäinen vaihtoehto'),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__add-dropdown-item a') }),
          setTextFieldValue(function () { return formComponents().eq(11).find('.editor-form__text-field:last') }, 'Toinen vaihtoehto'),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question:eq(0) a:contains("Lisäkysymykset")') }),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay a:contains("Lista, monta valittavissa")') }),
          setTextFieldValue(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay input.editor-form__text-field') }, "Monivalinta jatkokysymyksenä"),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__checkbox + .editor-form__checkbox-label') }),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")') }),
          setTextFieldValue(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(0)') }, 'Jatkokysymys A'),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")') }),
          setTextFieldValue(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(1)') }, 'Jatkokysymys B'),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay a:contains("Vierekkäiset tekstikentät")') }),
          setTextFieldValue(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__text-field').eq(3) }, 'Vierekkäinen tekstikenttä painikkeiden jatkokysymyksenä'),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__checkbox + label.editor-form__checkbox-label:contains("Vastaaja voi lisätä useita vastauksia")') }),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container a:contains("Tekstikenttä")') }),
          setTextFieldValue(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(0) }, 'Jatkokysymys A'),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")').eq(0) }),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container a:contains("Tekstikenttä")') }),
          setTextFieldValue(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(1) }, 'Jatkokysymys B'),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container a:contains("Tekstikenttä")') }),
          setTextFieldValue(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(2) }, 'Jatkokysymys C'),
          clickElement(function() { return formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")').eq(2) })
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(12)
          expect(formComponents().eq(11).find('.editor-form__text-field:first').val()).to.equal('Lyhyen listan kysymys')
          expect(formComponents().eq(11).find('.editor-form__checkbox-container input').prop('checked')).to.equal(true)
          expect(formComponents().eq(11).find('.editor-form__multi-options-container > div:nth-child(1) .editor-form__text-field').not('.editor-form__followup-question-overlay input').val()).to.equal('Ensimmäinen vaihtoehto')
          expect(formComponents().eq(11).find('.editor-form__multi-options-container > div:nth-child(2) .editor-form__text-field').not('.editor-form__followup-question-overlay input').val()).to.equal('Toinen vaihtoehto')
          expect(formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__text-field').eq(3).val()).to.equal('Vierekkäinen tekstikenttä painikkeiden jatkokysymyksenä')
          expect(formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(0).val()).to.equal('Jatkokysymys A')
          expect(formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(1).val()).to.equal('Jatkokysymys B')
          expect(formComponents().eq(11).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(2).val()).to.equal('Jatkokysymys C')
        })
      })

      describe('adjacent fields', function() {
        before(
          clickComponentMenuItem('Vierekkäiset tekstikentät'),
          setTextFieldValue(function() { return formComponents().eq(12).find('.editor-form__text-field') }, 'Vierekkäinen tekstikenttä'),
          clickSubComponentMenuItem('Tekstikenttä', function() { return formComponents().eq(12)}),
          setTextFieldValue(function() {
            return formComponents().eq(12).find('.editor-form__adjacent-fieldset-container .editor-form__text-field')
          }, 'Tekstikenttä 1'),
          clickSubComponentMenuItem('Tekstikenttä', function() { return formComponents().eq(12)}),
          setTextFieldValue(function() {
            return formComponents().eq(12).find('.editor-form__adjacent-fieldset-container .editor-form__text-field').eq(1)
          }, 'Tekstikenttä 2')
        )
        it('🌸  is working so wonderfully 🌸', function() {})
      })

      describe('dropdown with adjacent fields as followup', function() {
        before(
          clickComponentMenuItem('Pudotusvalikko'),
          setTextFieldValue(function() { return formComponents().eq(15).find('.editor-form__text-field').eq(0) }, 'Päätason pudotusvalikko'),
          setTextFieldValue(function() { return formComponents().eq(15).find('.editor-form__multi-options-wrapper-outer .editor-form__text-field').eq(0) }, 'Pudotusvalikon 1. kysymys'),
          clickElement(function() { return formComponents().eq(15).find('.editor-form__multi-options_wrapper a:contains("Lisää")') }),
          setTextFieldValue(function() { return formComponents().eq(15).find('.editor-form__multi-options-wrapper-outer .editor-form__text-field').eq(1) }, 'Pudotusvalikon 2. kysymys'),
          clickElement(function() { return formComponents().eq(15).find('.editor-form__multi-options-container a:contains("Lisäkysymykset")') }),
          clickElement(function() { return formComponents().eq(15).find('.editor-form__followup-question-overlay a:contains("Vierekkäiset tekstikentät")') }),
          setTextFieldValue(function() { return formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__text-field') }, 'Vierekkäinen tekstikenttä jatkokysymyksenä'),
          clickElement(function() { return formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__checkbox + label.editor-form__checkbox-label:contains("Vastaaja voi lisätä useita vastauksia")') }),
          clickElement(function() { return formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container a:contains("Tekstikenttä")') }),
          setTextFieldValue(function() { return formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(0) }, 'Jatkokysymys A'),
          clickElement(function() { return formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")').eq(0) }),
          clickElement(function() { return formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container a:contains("Tekstikenttä")') }),
          setTextFieldValue(function() { return formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(1) }, 'Jatkokysymys B'),
          clickElement(function() { return formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container a:contains("Tekstikenttä")') }),
          setTextFieldValue(function() { return formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(2) }, 'Jatkokysymys C'),
          clickElement(function() { return formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")').eq(2) })
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(16)
          expect(formComponents().eq(15).find('.editor-form__text-field:first').val()).to.equal('Päätason pudotusvalikko')
          expect(formComponents().eq(15).find('.editor-form__multi-options-wrapper-outer .editor-form__text-field').eq(0).val()).to.equal('Pudotusvalikon 1. kysymys')
          expect(formComponents().eq(15).find('.editor-form__multi-options-wrapper-outer .editor-form__text-field').eq(1).val()).to.equal('Pudotusvalikon 2. kysymys')
          expect(formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__text-field').eq(0).val()).to.equal('Vierekkäinen tekstikenttä jatkokysymyksenä')
          expect(formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(0).val()).to.equal('Jatkokysymys A')
          expect(formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(1).val()).to.equal('Jatkokysymys B')
          expect(formComponents().eq(15).find('.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field').eq(2).val()).to.equal('Jatkokysymys C')
        })
      })

      describe('hakukohde specific question', function() {
        var component = function() { return formComponents().eq(16) }
        before(
          clickComponentMenuItem('Tekstikenttä'),
          setTextFieldValue(
            function() {
              return component().find('.editor-form__text-field')
            },
            'Hakukohdekohtainen kysymys'
          ),
          clickElement(function() {
            return component().find('.belongs-to-hakukohteet__modal-toggle')
          }),
          clickElement(function() {
            return component().find('.belongs-to-hakukohteet-modal__hakukohde-list-item')
          })
        )
        it('shows the selected hakukohde', function() {
          expect(component().find('.belongs-to-hakukohteet__hakukohde-label').length).to.equal(1)
        })
      })

      describe('autosave', function () {
        before(
          wait.until(function() {
            var flasher = testFrame().find('.top-banner .flasher')
            return flasher.css('opacity') !== "0" && flasher.find('span:visible').text() === 'Kaikki muutokset tallennettu'
          }, 5000)
        )
        it('notification shows success', function() {
          expect(testFrame().find('.top-banner .flasher span').text()).to.equal('Kaikki muutokset tallennettu')
        })
      })
    })
  })
})();
