;(() => {
  const addNewFormLink = () => {
    return testFrame().find('.editor-form__control-button--enabled')
  }

  const formPreviewLink = () => {
    return testFrame().find('.editor-form__form-preview-link')
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

  const menuItem = (title) => {
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

  const subMenuItem = (element, title) => {
    triggerEvent(
      element().find('.editor-form__add-component-toolbar'),
      'mouseover'
    )
    return element().find(
      '.editor-form__add-component-toolbar a:contains("' + title + '")'
    )
  }

  const clickComponentMenuItem = (title) => {
    return clickElement(() => menuItem(title))
  }

  const clickSubComponentMenuItem = (element, title) => {
    return clickElement(() => subMenuItem(element, title))
  }

  before(() => {
    loadInFrame('http://localhost:8350/lomake-editori/')
  })

  afterEach(() => {
    expect(window.uiError || null).to.be.null
  })

  describe('Selection Limit', () => {
    describe('form creation', () => {
      before(
        wait.until(editorPageIsLoaded),
        clickElement(addNewFormLink),
        wait.forMilliseconds(10 * 1000), // Odota, että autosave on valmistunut
        setTextFieldValue(formTitleField, 'Selection Limit'),
        wait.until(
          () => formListItems(0).find('span:eq(0)').text() === 'Selection Limit'
        )
      )
      it('creates blank form', () => {
        expect(formTitleField().val()).to.equal('Selection Limit')
        expect(formComponents()).to.have.length(0)
      })
    })

    describe('adding elements:', () => {
      describe('create single choice with limited selection', () => {
        before(
          clickComponentMenuItem('Painikkeet, yksi valittavissa'),
          setTextFieldValue(
            () => formComponents().find('.editor-form__text-field:eq(0)'),
            'Rajoitettu valinta'
          ),
          clickElement(() =>
            formComponents().find('.editor-form__checkbox:eq(0) + label')
          ), // required
          clickElement(() =>
            formComponents().find('.editor-form__checkbox:eq(1) + label')
          ), // selection limit
          clickElement(() =>
            formComponents().find(
              '.editor-form__add-dropdown-item a:contains("Lisää")'
            )
          ),
          setTextFieldValue(
            () => formComponents().find('.editor-form__text-field:eq(1)'),
            'Aina täynnä'
          ),
          setTextFieldValue(
            () => formComponents().find('.editor-form__text-field:eq(2)'),
            '0'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__add-dropdown-item a:contains("Lisää")'
            )
          ),
          setTextFieldValue(
            () => formComponents().find('.editor-form__text-field:eq(3)'),
            'Aina tilaa'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__add-dropdown-item a:contains("Lisää")'
            )
          ),
          setTextFieldValue(
            () => formComponents().find('.editor-form__text-field:eq(5)'),
            'Yksi paikka'
          ),
          setTextFieldValue(
            () => formComponents().find('.editor-form__text-field:eq(6)'),
            '1'
          )
        )
        it('has expected contents', () => {
          expect(formComponents()).to.have.length(1)
        })
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
})()
