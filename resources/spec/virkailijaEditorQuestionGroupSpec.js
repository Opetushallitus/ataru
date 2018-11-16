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
      .find(".editor-form__component-wrapper header:contains('Henkilötiedot')");
  }

  function formComponents() {
    return testFrame().find('.editor-form__component-wrapper')
    // exclude followup question components
      .not('.editor-form__followup-question-overlay .editor-form__component-wrapper')
    // exclude hakukohteet
      .not(function (i, node) { return $(node).find("header:contains('Hakukohteet')").length > 0; })
    // exclude henkilötiedot
      .not(function (i, node) { return $(node).find("header:contains('Henkilötiedot')").length > 0; })
  }

  function menuItem(title) {
    triggerEvent(testFrame().find('.editor-form > .editor-form__add-component-toolbar'), 'mouseover');
    return testFrame().find('.editor-form > .editor-form__add-component-toolbar a:contains("'+ title +'")')
  }

  function subMenuItem(element, title) {
    triggerEvent(element().find('.editor-form__add-component-toolbar'), 'mouseover');
    return element().find('.editor-form__add-component-toolbar a:contains("'+ title +'")');
  }

  function clickComponentMenuItem(title) {
    return clickElement(function() { return menuItem(title) })
  }

  function clickSubComponentMenuItem(element, title) {
    return clickElement(function () { return subMenuItem(element, title); });
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
        wait.until(function() { return formListItems(0).find('span:eq(0)').text() === 'Kysymysryhmä: testilomake' })
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
          setTextFieldValue(function () { return formComponents().find('.editor-form__text-field:eq(0)') }, 'Päätaso: pudotusvalikko'),
          setTextFieldValue(function () { return formComponents().find('.editor-form__text-field:eq(1)') }, 'Päätaso: A'),
          clickElement(function () { return formComponents().find('.editor-form__add-dropdown-item a:contains("Lisää")') }),
          setTextFieldValue(function () { return formComponents().find('.editor-form__text-field:eq(2)') }, 'Päätaso: B'),
          clickElement(function() { return formComponents().find('.editor-form__checkbox + label') })
        )
        it('has expected contents', function () {
          expect(formComponents()).to.have.length(1)
          expect(formComponents().find('.editor-form__text-field:eq(0)').val()).to.equal('Päätaso: pudotusvalikko')
          expect(formComponents().find('.editor-form__checkbox-container input').prop('checked')).to.equal(true)
          expect(formComponents().find('.editor-form__text-field:eq(1)').val()).to.equal('Päätaso: A')
          expect(formComponents().find('.editor-form__text-field:eq(2)').val()).to.equal('Päätaso: B')
        })
      })

      describe('available followup components for dropdown', function () {
        before(
          clickElement(function () { return formComponents().eq(0).find('.editor-form__followup-question:eq(1) a:contains("Lisäkysymykset")') }),
          wait.until(function () {
            triggerEvent(formComponents().find('.editor-form__followup-question-overlay .editor-form__add-component-toolbar'), 'mouseover');
            return true;
          })
        )
        it('opens up toolbar for adding followup components', function () {
          expect(_.map(formComponents().find('.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a'), function (e) { return $(e).text(); }))
            .to.eql([
              'Pudotusvalikko',
              'Painikkeet, yksi valittavissa',
              'Lista, monta valittavissa',
              'Tekstikenttä',
              'Tekstialue',
              'Vierekkäiset tekstikentät',
              'Liitepyyntö',
              'Kysymysryhmä',
              'Infoteksti'
            ]);
        })
      })

      describe('adding question group as a followup element', function () {
        before(
          clickSubComponentMenuItem(function () { return formComponents().eq(0); }, 'Kysymysryhmä'),
          setTextFieldValue(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__text-field') }, 'Kysymysryhmä: ryhmän otsikko'),
          wait.until(function () {
            triggerEvent(formComponents().find('.editor-form__followup-question-overlay .editor-form__add-component-toolbar'), 'mouseover');
            return true;
          })
        )
        it('adds question group as a followup element', function () {
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-main-header').text()).to.equal('Kysymysryhmä')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__text-field').val()).to.equal('Kysymysryhmä: ryhmän otsikko')
          expect(_.map(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .form__add-component-toolbar--list-item a'), function (e) { return $(e).text(); }))
            .to.eql([
              'Pudotusvalikko',
              'Painikkeet, yksi valittavissa',
              'Lista, monta valittavissa',
              'Tekstikenttä',
              'Tekstialue',
              'Vierekkäiset tekstikentät',
              'Liitepyyntö',
              'Infoteksti'
            ]);
        })
      })

      describe('adding dropdown as element to a question group', function () {
        before(
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper'); }, 'Pudotusvalikko'),
          setTextFieldValue(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(1)') }, 'Kysymysryhmä: pudotusvalikko'),
          setTextFieldValue(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(2)') }, 'Pudotusvalikko: A'),
          clickElement(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__add-dropdown-item a:contains("Lisää")') }),
          setTextFieldValue(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(3)') }, 'Pudotusvalikko: B'),
          clickElement(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label') })
        )
        it('adds dropdown as element to a question group', function () {
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(1)').text()).to.equal('Pudotusvalikko')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(1)').val()).to.equal('Kysymysryhmä: pudotusvalikko')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(2)').val()).to.equal('Pudotusvalikko: A')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(3)').val()).to.equal('Pudotusvalikko: B')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox').prop('checked')).to.equal(true)
        })
      })

      describe('adding single choice button as element to a question group', function() {
        before(
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper'); }, 'Painikkeet, yksi valittavissa'),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(4)') }, 'Kysymysryhmä: painikkeet, yksi valittavissa'),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__add-dropdown-item a:contains("Lisää"):eq(1)') }),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(5)')}, 'Painikkeet, yksi valittavissa: A' ),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__add-dropdown-item a:contains("Lisää"):eq(1)') }),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(6)')}, 'Painikkeet, yksi valittavissa: B' ),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(1)') })
        )
        it('adds single choice button as element to a question group', function() {
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(2)').text()).to.equal('Painikkeet, yksi valittavissa')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(4)').val()).to.equal('Kysymysryhmä: painikkeet, yksi valittavissa')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(5)').val()).to.equal('Painikkeet, yksi valittavissa: A')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(6)').val()).to.equal('Painikkeet, yksi valittavissa: B')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(1)').prop('checked')).to.equal(true)
        })
      })

      describe('adding multiple choice button as element to a question group', function() {
        before(
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper'); }, 'Lista, monta valittavissa'),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(7)') }, 'Kysymysryhmä: lista, monta valittavissa'),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__add-dropdown-item a:contains("Lisää"):eq(2)') }),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(8)')}, 'Lista, monta valittavissa: A' ),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__add-dropdown-item a:contains("Lisää"):eq(2)') }),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(9)')}, 'Lista, monta valittavissa: B' ),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(2)') })
        )
        it('adds multiple choice button as element to a question group', function() {
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(3)').text()).to.equal('Lista, monta valittavissa')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(7)').val()).to.equal('Kysymysryhmä: lista, monta valittavissa')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(8)').val()).to.equal('Lista, monta valittavissa: A')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(9)').val()).to.equal('Lista, monta valittavissa: B')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(2)').prop('checked')).to.equal(true)
        })
      })

      describe('adding single-answer text field as element to a question group', function() {
        before(
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper'); }, 'Tekstikenttä'),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(10)') }, 'Tekstikenttä, yksi vastaus'),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(3)') })
        )
        it('adds single-answer text field as element to a question group', function() {
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(4)').text()).to.equal('Tekstikenttä')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(10)').val()).to.equal('Tekstikenttä, yksi vastaus')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(3)').prop('checked')).to.equal(true) // required
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(4)').prop('checked')).to.equal(false) // multiple answers
        })
      })

      describe('adding multi-answer text field as element to a question group', function() {
        before(
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper'); }, 'Tekstikenttä'),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(11)') }, 'Tekstikenttä, monta vastausta'),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(6)') }),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(7)') })
        )
        it('adds multi-answer text field as element to a question group', function() {
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(5)').text()).to.equal('Tekstikenttä')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(11)').val()).to.equal('Tekstikenttä, monta vastausta')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(6)').prop('checked')).to.equal(true) // required
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(7)').prop('checked')).to.equal(true) // multiple answers
        })
      })

      describe('adding text area as element to a question group', function() {
        before(
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper'); }, 'Tekstialue'),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(12)') }, 'Tekstialue'),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(9)') })
        )
        it('adds text area as element to a question group', function() {
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(6)').text()).to.equal('Tekstialue')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(12)').val()).to.equal('Tekstialue')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(9)').prop('checked')).to.equal(true) // required
        })
      })

      describe('adding single-answer adjacent text field to a question group', function() {
        before(
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper'); }, 'Vierekkäiset tekstikentät'),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(14)') }, 'Vierekkäiset tekstikentät, yksi vastaus'),
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container'); }, 'Tekstikenttä'),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(15)') }, 'Vierekkäiset tekstikentät, yksi vastaus: A'),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(11) + label') }),
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container'); }, 'Tekstikenttä'),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(16)') }, 'Vierekkäiset tekstikentät, yksi vastaus: B'),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(12) + label') })
        )
        it('adds single-answer adjacent text field as element to a question group', function() {
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(7)').text()).to.equal('Vierekkäiset tekstikentät')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(14)').val()).to.equal('Vierekkäiset tekstikentät, yksi vastaus')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(10)').prop('checked')).to.equal(false) // multiple answers
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(15)').val()).to.equal('Vierekkäiset tekstikentät, yksi vastaus: A')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(11)').prop('checked')).to.equal(true)
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(16)').val()).to.equal('Vierekkäiset tekstikentät, yksi vastaus: B')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(12)').prop('checked')).to.equal(true)
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container .form__add-component-toolbar--list-item a:contains("Pohjakoulutusmoduuli")').length).to.equal(0)
        })
      })

      describe('adding multi-answer adjacent text field to a question group', function() {
        before(
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper'); }, 'Vierekkäiset tekstikentät'),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(17)') }, 'Vierekkäiset tekstikentät, monta vastausta'),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(13) + label') }),
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container:eq(1)'); }, 'Tekstikenttä'),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(18)') }, 'Vierekkäiset tekstikentät, monta vastausta: A'),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(14) + label') }),
          clickSubComponentMenuItem(function () { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container:eq(1)'); }, 'Tekstikenttä'),
          setTextFieldValue(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(19)') }, 'Vierekkäiset tekstikentät, monta vastausta: B'),
          clickElement(function() { return formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(15) + label') })
        )
        it('adds multi-answer adjacent text field as element to a question group', function() {
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(10)').text()).to.equal('Vierekkäiset tekstikentät')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(17)').val()).to.equal('Vierekkäiset tekstikentät, monta vastausta')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(13)').prop('checked')).to.equal(true) // multiple answers
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(18)').val()).to.equal('Vierekkäiset tekstikentät, monta vastausta: A')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(14)').prop('checked')).to.equal(true)
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(19)').val()).to.equal('Vierekkäiset tekstikentät, monta vastausta: B')
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(15)').prop('checked')).to.equal(true)
          expect(formComponents().find('.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container .form__add-component-toolbar--list-item a:contains("Pohjakoulutusmoduuli")').length).to.equal(0)
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
