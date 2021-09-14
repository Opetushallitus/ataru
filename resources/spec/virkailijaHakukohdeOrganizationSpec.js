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

  const formSections = () => {
    return testFrame()
      .find('.editor-form__component-wrapper')
      .filter(
        (i, node) => $(node).find("header:contains('Lomakeosio')").length > 0
      )
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

  before(() => {
    loadInFrame(
      'http://localhost:8350/lomake-editori/auth/cas?ticket=USER-WITH-HAKUKOHDE-ORGANIZATION'
    )
  })

  afterEach(() => {
    expect(window.uiError || null).to.be.null
  })

  describe('Editor when user associated by hakukohteen organization', () => {
    before(
      wait.until(editorPageIsLoaded),
      clickElement(() => formListItems(0)),
      wait.forMilliseconds(1000), // TODO: fix form refresh in frontend so that this isn't required (or check that no AJAX requests are ongoing)
      clickComponentMenuItem('Tekstikenttä'),
      setTextFieldValue(
        () => formComponents().eq(0).find('.editor-form__text-field'),
        'Ensimmäinen kysymys'
      ),
      clickElement(() =>
        formComponents().eq(0).find('.editor-form__info-addon-checkbox label')
      ),
      setTextFieldValue(
        () =>
          formComponents()
            .eq(0)
            .find('.editor-form__info-addon-inputs textarea'),
        'Ensimmäisen kysymyksen ohjeteksti'
      )
    )
    it('has 1 fixture forms', () => {
      expect(formListItems()).to.have.length(1)
      expect(formComponents()).to.have.length(1)
    })
  })
})()
