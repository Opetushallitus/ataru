var addNewFormLink = function() {
  return testFrame().find('.editor-form__add-new a')
}

var formTitleField = function() {
  return testFrame().find('.editor-form__form-name-input')
}

function editorPageIsLoaded() {
  return elementExists(addNewFormLink())
}

function formList() {
  return testFrame().find('.editor-form__list')
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
}

function formSections() {
  return testFrame().find('.editor-form__section_wrapper')
}

function clickComponentMenuItem(title) {
  function menuItem() { return testFrame().find('.editor-form > form > .editor-form__add-component-toolbar a:contains("'+ title +'")') }
  return clickElement(menuItem)
}

function autosaveSuccessful() {
  return function() { $('.top-banner .flasher span').text() === "Kaikki muutokset tallennettu" }
}

(function() {
  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('Editor', function() {

    describe('with no forms', function() {
      before(
        wait.until(editorPageIsLoaded)
      )
      it('has empty form listing', function() {
        expect(formListItems()).to.have.length(0)
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
          wait.forMilliseconds(1000)
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(1)
          expect(formComponents().eq(0).find('.editor-form__text-field').val()).to.equal('Ensimmäinen kysymys')
          expect(formComponents().eq(0).find('.editor-form__size-button-group input:checked').val()).to.equal('M')
          expect(formComponents().eq(0).find('.editor-form__checkbox-container input').prop('checked')).to.equal(false)
        })
      })

      describe('textarea', function() {
        before(
          clickComponentMenuItem('Tekstialue'),
          clickElement(function() { return formComponents().eq(1).find('.editor-form__size-button-group div:eq(2) label')}),
          clickElement(function() { return formComponents().eq(1).find('.editor-form__checkbox-wrapper label')}),
          setTextFieldValue(function() { return formComponents().eq(1).find('.editor-form__text-field')}, 'Toinen kysymys')
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(2)
          expect(formComponents().eq(1).find('.editor-form__text-field').val()).to.equal('Toinen kysymys')
          expect(formComponents().eq(1).find('.editor-form__size-button-group input:checked').val()).to.equal('L')
          expect(formComponents().eq(1).find('.editor-form__checkbox-container input').prop('checked')).to.equal(true)
        })
      })

      describe('dropdown', function() {
        before(
          clickComponentMenuItem('Pudotusvalikko'),
          setTextFieldValue(function() { return formComponents().eq(2).find('.editor-form__text-field').eq(0)}, 'Kolmas kysymys'),
          setTextFieldValue(function() { return formComponents().eq(2).find('.editor-form__multi-options_wrapper .editor-form__text-field:last')}, 'Ensimmäinen vaihtoehto'),
          clickElement(function() { return formComponents().eq(2).find('.editor-form__add-dropdown-item a') }),
          setTextFieldValue(function() { return formComponents().eq(2).find('.editor-form__multi-options_wrapper .editor-form__text-field:last')}, 'Toinen vaihtoehto'),
          clickElement(function() { return formComponents().eq(2).find('.editor-form__add-dropdown-item a') }),
          setTextFieldValue(function() { return formComponents().eq(2).find('.editor-form__multi-options_wrapper .editor-form__text-field:last')}, 'Kolmas vaihtoehto'),
          clickElement(function() { return formComponents().eq(2).find('.editor-form__add-dropdown-item a') })
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(3)
          expect(formComponents().eq(2).find('.editor-form__text-field:first').val()).to.equal('Kolmas kysymys')
          expect(formComponents().eq(2).find('.editor-form__checkbox-container input').prop('checked')).to.equal(false)
          expect(formComponents().eq(2).find('.editor-form__multi-options_wrapper input').length).to.equal(4)
          var options = _.map(formComponents().eq(2).find('.editor-form__multi-options_wrapper input'), function(inputField) {
            return $(inputField).val()
          })
          expect(options).to.eql(["Ensimmäinen vaihtoehto", "Toinen vaihtoehto", "Kolmas vaihtoehto", ""])
        })
      })

      describe('section with contents', function() {
        before(
          clickComponentMenuItem('Lomakeosio'),
          setTextFieldValue(function() { return formSections().eq(0).find('.editor-form__text-field').eq(0) }, 'Testiosio'),
          clickElement(function() { return formSections().eq(0).find('.form__add-component-toolbar--list li a:contains("Tekstialue")') }),
          clickElement(function() { return formSections().eq(0).find('.editor-form__checkbox-wrapper label')}),
          setTextFieldValue(function() { return formSections().eq(0).find('.editor-form__text-field').eq(1) }, 'Osiokysymys'),
          clickElement(function() { return formSections().eq(0).find('.editor-form__size-button-group div:eq(0) label')})
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(5)
          expect(formSections().eq(0).find('.editor-form__text-field').eq(0).val()).to.equal('Testiosio')
          expect(formSections().eq(0).find('.editor-form__text-field').eq(1).val()).to.equal('Osiokysymys')
          expect(formSections().eq(0).find('.editor-form__size-button-group input:checked').val()).to.equal('S')
          expect(formSections().eq(0).find('.editor-form__checkbox-container input').prop('checked')).to.equal(true)
        })
      })
    })
  })
})();