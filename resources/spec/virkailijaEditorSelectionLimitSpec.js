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
  const formSections = () => {
    return testFrame()
      .find('.editor-form__component-wrapper')
      .filter(
        (i, node) => $(node).find("header:contains('Lomakeosio')").length > 0
      )
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
        console.log(formSections())
        before(
          clickComponentMenuItem('Lomakeosio'),
          setTextFieldValue(
            () => formSections().eq(0).find('.editor-form__text-field:first'),
            'Lomakeosio'
          ),
          clickSubComponentMenuItem('Painikkeet, yksi valittavissa', () =>
            formComponents().eq(0)
          ),
          setTextFieldValue(
            () => formSections().eq(0).find('.editor-form__text-field:eq(1)'),
            'Rajoitettu valinta'
          ),
          clickElement(() =>
            formSections()
              .eq(0)
              .find('.editor-form__checkbox + label:contains("Pakollinen")')
          ), // required
          clickElement(() =>
            formSections()
              .eq(0)
              .find(
                '.editor-form__checkbox + label:contains("Rajoitettu valinta")'
              )
          ), // selection limit
          clickElement(() =>
            formSections()
              .eq(0)
              .find('.editor-form__add-dropdown-item a:contains("Lisää")')
          ),
          setTextFieldValue(
            () => formSections().eq(0).find('.editor-form__text-field:eq(2)'),
            'Aina täynnä'
          ),
          setTextFieldValue(
            () => formSections().eq(0).find('.editor-form__text-field:eq(3)'),
            '0'
          ),
          clickElement(() =>
            formSections()
              .eq(0)
              .find('.editor-form__add-dropdown-item a:contains("Lisää")')
          ),
          setTextFieldValue(
            () => formComponents().find('.editor-form__text-field:eq(4)'),
            'Aina tilaa'
          ),
          clickElement(() =>
            formSections()
              .eq(0)
              .find('.editor-form__add-dropdown-item a:contains("Lisää")')
          ),
          setTextFieldValue(
            () => formSections().eq(0).find('.editor-form__text-field:eq(6)'),
            'Yksi paikka'
          ),
          setTextFieldValue(
            () => formSections().eq(0).find('.editor-form__text-field:eq(7)'),
            '1'
          )
        )
        it('has expected contents', () => {
          expect(formSections().eq(0)).to.have.length(1)
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
