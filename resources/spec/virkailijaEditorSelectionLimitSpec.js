;(function() {
  function addNewFormLink() {
    return testFrame().find('.editor-form__control-button--enabled')
  }
  function formPreviewLink() {
    return testFrame().find('.editor-form__form-preview-link')
  }
  function formTitleField() {
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
      return formList()
        .find('a')
        .eq(n)
    } else {
      return formList().find('a')
    }
  }

  function personInfoModule() {
    return testFrame().find(
      ".editor-form__component-wrapper header:contains('Henkilötiedot')"
    )
  }

  function formComponents() {
    return (
      testFrame()
        .find('.editor-form__component-wrapper')
        // exclude followup question components
        .not(
          '.editor-form__followup-question-overlay .editor-form__component-wrapper'
        )
        // exclude hakukohteet
        .not(function(i, node) {
          return $(node).find("header:contains('Hakukohteet')").length > 0
        })
        // exclude henkilötiedot
        .not(function(i, node) {
          return $(node).find("header:contains('Henkilötiedot')").length > 0
        })
    )
  }

  function menuItem(title) {
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

  function subMenuItem(element, title) {
    triggerEvent(
      element().find('.editor-form__add-component-toolbar'),
      'mouseover'
    )
    return element().find(
      '.editor-form__add-component-toolbar a:contains("' + title + '")'
    )
  }

  function clickComponentMenuItem(title) {
    return clickElement(function() {
      return menuItem(title)
    })
  }

  function clickSubComponentMenuItem(element, title) {
    return clickElement(function() {
      return subMenuItem(element, title)
    })
  }

  before(function() {
    loadInFrame('http://localhost:8350/lomake-editori/')
  })

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('Selection Limit', function() {
    describe('form creation', function() {
      before(
        wait.until(editorPageIsLoaded, 10000),
        clickElement(addNewFormLink),
        wait.forMilliseconds(1000), // TODO: fix form refresh in frontend so that this isn't required (or check that no AJAX requests are ongoing)
        setTextFieldValue(formTitleField, 'Selection Limit'),
        wait.until(function() {
          return (
            formListItems(0)
              .find('span:eq(0)')
              .text() === 'Selection Limit'
          )
        })
      )
      it('creates blank form', function() {
        expect(formTitleField().val()).to.equal('Selection Limit')
        expect(formComponents()).to.have.length(0)
      })
    })

    describe('adding elements:', function() {
      describe('create single choice with limited selection', function() {
        before(
          clickComponentMenuItem('Painikkeet, yksi valittavissa'),
          setTextFieldValue(function() {
            return formComponents().find('.editor-form__text-field:eq(0)')
          }, 'Rajoitettu valinta'),
          clickElement(function() {
            return formComponents().find('.editor-form__checkbox:eq(0) + label')
          }), // required
          clickElement(function() {
            return formComponents().find('.editor-form__checkbox:eq(1) + label')
          }), // selection limit
          clickElement(function() {
            return formComponents().find(
              '.editor-form__add-dropdown-item a:contains("Lisää")'
            )
          }),
          setTextFieldValue(function() {
            return formComponents().find('.editor-form__text-field:eq(1)')
          }, 'Aina täynnä'),
          setTextFieldValue(function() {
            return formComponents().find('.editor-form__text-field:eq(2)')
          }, '0'),
          clickElement(function() {
            return formComponents().find(
              '.editor-form__add-dropdown-item a:contains("Lisää")'
            )
          }),
          setTextFieldValue(function() {
            return formComponents().find('.editor-form__text-field:eq(3)')
          }, 'Aina tilaa'),
          clickElement(function() {
            return formComponents().find(
              '.editor-form__add-dropdown-item a:contains("Lisää")'
            )
          }),
          setTextFieldValue(function() {
            return formComponents().find('.editor-form__text-field:eq(5)')
          }, 'Yksi paikka'),
          setTextFieldValue(function() {
            return formComponents().find('.editor-form__text-field:eq(6)')
          }, '1')
        )
        it('has expected contents', function() {
          expect(formComponents()).to.have.length(1)
        })
      })
    })

    describe('autosave', function() {
      before(
        wait.until(function() {
          var flasher = testFrame().find('.top-banner .flasher')
          return (
            flasher.css('opacity') !== '0' &&
            flasher.find('span:visible').text() ===
              'Kaikki muutokset tallennettu'
          )
        }, 5000)
      )
      it('notification shows success', function() {
        expect(
          testFrame()
            .find('.top-banner .flasher span')
            .text()
        ).to.equal('Kaikki muutokset tallennettu')
      })
    })
  })
})()
