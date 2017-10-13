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

  function clickComponentMenuItem(title) {
    function menuItem() { return testFrame().find('.editor-form > .editor-form__add-component-toolbar a:contains("'+ title +'")') }
    return clickElement(menuItem)
  }

  before(function () {
    loadInFrame('http://localhost:8350/lomake-editori/')
  })

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('Editor', function() {
    describe('form creation', function() {
      before(
        wait.until(editorPageIsLoaded, 10000),
        clickElement(addNewFormLink),
        wait.forMilliseconds(1000), // TODO: fix form refresh in frontend so that this isn't required (or check that no AJAX requests are ongoing)
        setTextFieldValue(formTitleField, 'Kysymysryhmä: testilomake'),
        wait.until(function() {
          return formListItems(0).find('span:eq(0)').text() === 'Kysymysryhmä: testilomake'
        })
      )
      it('creates blank form', function () {
        expect(formTitleField().val()).to.equal('Kysymysryhmä: testilomake')
        expect(formComponents()).to.have.length(0)
      })
      it('has person info module', function() {
        expect(personInfoModule()).to.have.length(1)
      })
    })

    describe('adding elements:', function() {
      describe('adding main level dropdown', function () {
        before(
          clickComponentMenuItem('Pudotusvalikko'),
          setTextFieldValue(function () {
            return formComponents().find('.editor-form__text-field:eq(0)')
          }, 'Päätaso: pudotusvalikko'),
          setTextFieldValue(function () {
            return formComponents().find('.editor-form__text-field:eq(1)')
          }, 'Päätaso: A'),
          clickElement(function () {
            return formComponents().find('.editor-form__add-dropdown-item a:contains("Lisää")')
          }),
          setTextFieldValue(function () {
            return formComponents().find('.editor-form__text-field:eq(2)')
          }, 'Päätaso: B')
        )
        it('has expected contents', function () {
          expect(formComponents()).to.have.length(1)
          expect(formComponents().find('.editor-form__text-field:eq(0)').val()).to.equal('Päätaso: pudotusvalikko')
          expect(formComponents().find('.editor-form__checkbox-container input').prop('checked')).to.equal(false)
          expect(formComponents().find('.editor-form__text-field:eq(1)').val()).to.equal('Päätaso: A')
          expect(formComponents().find('.editor-form__text-field:eq(2)').val()).to.equal('Päätaso: B')
        })
      })

      describe('available followup components for dropdown', function () {
        before(
          clickElement(function () {
            return formComponents().eq(0).find('.editor-form__followup-question:eq(1) a:contains("Lisäkysymykset")')
          })
        )
        it('opens up toolbar for adding followup components', function () {
          expect(formComponents().find('.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a')).to.have.length(8)
          expect(formComponents().find('.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a:eq(0)').text()).to.equal('Pudotusvalikko')
          expect(formComponents().find('.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a:eq(1)').text()).to.equal('Painikkeet, yksi valittavissa')
          expect(formComponents().find('.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a:eq(2)').text()).to.equal('Lista, monta valittavissa')
          expect(formComponents().find('.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a:eq(3)').text()).to.equal('Tekstikenttä')
          expect(formComponents().find('.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a:eq(4)').text()).to.equal('Tekstialue')
          expect(formComponents().find('.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a:eq(5)').text()).to.equal('Vierekkäiset tekstikentät')
          expect(formComponents().find('.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a:eq(6)').text()).to.equal('Kysymysryhmä')
          expect(formComponents().find('.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a:eq(7)').text()).to.equal('Infoteksti')
        })
      })

      describe('adding question group as a followup element', function () {
        before(
          clickElement(function () {
            return formComponents().find('.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a:contains("Kysymysryhmä")')
          }),
          setTextFieldValue(function () {
            return formComponents().find('.editor-form__followup-question-overlay .editor-form__text-field')
          }, 'Kysymysryhmä: ryhmän otsikko')
        )
        it('adds question group as a followup element', function () {
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-header').text()).to.equal('Kysymysryhmä')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__text-field').val()).to.equal('Kysymysryhmä: ryhmän otsikko')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .form__add-component-toolbar--list-item a')).to.have.length(7)
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .form__add-component-toolbar--list-item a:eq(0)').text()).to.equal('Pudotusvalikko')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .form__add-component-toolbar--list-item a:eq(1)').text()).to.equal('Painikkeet, yksi valittavissa')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .form__add-component-toolbar--list-item a:eq(2)').text()).to.equal('Lista, monta valittavissa')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .form__add-component-toolbar--list-item a:eq(3)').text()).to.equal('Tekstikenttä')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .form__add-component-toolbar--list-item a:eq(4)').text()).to.equal('Tekstialue')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .form__add-component-toolbar--list-item a:eq(5)').text()).to.equal('Vierekkäiset tekstikentät')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .form__add-component-toolbar--list-item a:eq(6)').text()).to.equal('Infoteksti')
        })
      })

      describe('adding dropdown as element to a question group', function () {
        before(
          clickElement(function () {
            return formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .form__add-component-toolbar--list-item a:contains("Pudotusvalikko")')
          }),
          setTextFieldValue(function () {
            return formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .editor-form__text-field:eq(1)')
          }, 'Kysymysryhmä: pudotusvalikko'),
          setTextFieldValue(function () {
            return formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .editor-form__text-field:eq(2)')
          }, 'Pudotusvalikko: A'),
          clickElement(function () {
            return formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .editor-form__add-dropdown-item a:contains("Lisää")')
          }),
          setTextFieldValue(function () {
            return formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .editor-form__text-field:eq(3)')
          }, 'Pudotusvalikko: B'),
          clickElement(function () {
            return formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .editor-form__checkbox + label')
          })
        )
        it('adds dropdown as element to a question group', function () {
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .editor-form__component-header:eq(1)').text()).to.equal('Pudotusvalikko')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .editor-form__text-field:eq(1)').val()).to.equal('Kysymysryhmä: pudotusvalikko')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .editor-form__text-field:eq(2)').val()).to.equal('Pudotusvalikko: A')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .editor-form__text-field:eq(3)').val()).to.equal('Pudotusvalikko: B')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__section_wrapper .editor-form__checkbox').prop('checked')).to.equal(true)
        })
      })
    })
  })
})();
