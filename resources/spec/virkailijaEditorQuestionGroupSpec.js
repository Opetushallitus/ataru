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

  const questionGroupChildrenContainer =
    '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__wrapper-element-well'

  const addQuestionGroupElement = (elementName) =>
    clickSubComponentMenuItem(
      () =>
        formComponents().find(
          '.editor-form__followup-question-overlay .editor-form__component-wrapper'
        ),
      elementName
    )

  const findQuestionInQuestionGroup = (questionIndex) =>
    formComponents()
      .find(
        `${questionGroupChildrenContainer} > div > .editor-form__component-wrapper`
      )
      .eq(questionIndex)

  const textFieldInNestedQuestion = (questionIndex, textFieldIndex) =>
    findQuestionInQuestionGroup(questionIndex)
      .find('.editor-form__text-field')
      .eq(textFieldIndex)

  const checkboxInNestedQuestion = (
    questionIndex,
    checkboxLabel,
    checkboxIndex = 0
  ) => {
    const label = findQuestionInQuestionGroup(questionIndex)
      .find(`.editor-form__checkbox + label:contains("${checkboxLabel}")`)
      .eq(checkboxIndex)
    return formComponents().find('#' + label.attr('for'))
  }

  const setTextFieldValueInNestedQuestion = (
    questionIndex,
    textFieldIndex,
    value
  ) =>
    setTextFieldValue(
      () => textFieldInNestedQuestion(questionIndex, textFieldIndex),
      value
    )

  const clickAddDropdownItemInNestedQuestion = (questionIndex) =>
    clickElement(() =>
      findQuestionInQuestionGroup(questionIndex).find(
        '.editor-form__add-dropdown-item a:contains("Lisää")'
      )
    )

  const clickCheckboxWithLabelInNestedQuestion = (
    questionIndex,
    checkboxLabel,
    checkboxIndex = 0
  ) =>
    clickElement(() =>
      findQuestionInQuestionGroup(questionIndex)
        .find(`.editor-form__checkbox + label:contains("${checkboxLabel}")`)
        .eq(checkboxIndex)
    )

  const addAdjacentTextFieldInNestedQuestion = (questionIndex) =>
    clickSubComponentMenuItem(
      () =>
        findQuestionInQuestionGroup(questionIndex).find(
          '.editor-form__adjacent-fieldset-container'
        ),
      'Tekstikenttä'
    )

  const headerInNestedQuestion = (questionIndex) =>
    findQuestionInQuestionGroup(questionIndex)
      .find('.editor-form__component-main-header')
      .eq(0)

  before(() => {
    loadInFrame('http://localhost:8350/lomake-editori/')
  })

  afterEach(() => {
    expect(window.uiError || null).to.be.null
  })

  describe('Editor', () => {
    describe('form creation', () => {
      before(
        wait.until(editorPageIsLoaded),
        clickElement(addNewFormLink),
        wait.forMilliseconds(10 * 1000), // Odota, että autosave on valmistunut
        setTextFieldValue(formTitleField, 'Kysymysryhmä: testilomake'),
        wait.until(
          () =>
            formListItems(0).find('span:eq(0)').text() ===
            'Kysymysryhmä: testilomake'
        ),
        wait.forElement(personInfoModule)
      )
      it('creates blank form', () => {
        expect(formTitleField().val()).to.equal('Kysymysryhmä: testilomake')
        expect(formComponents()).to.have.length(0)
      })
      it('has person info module', () => {
        expect(personInfoModule()).to.have.length(1)
      })
    })

    describe('adding elements:', () => {
      describe('adding main level dropdown', () => {
        before(
          clickComponentMenuItem('Pudotusvalikko'),
          setTextFieldValue(
            () => formComponents().find('.editor-form__text-field:eq(0)'),
            'Päätaso: pudotusvalikko'
          ),
          setTextFieldValue(
            () => formComponents().find('.editor-form__text-field:eq(1)'),
            'Päätaso: A'
          ),
          setTextFieldValue(
            () => formComponents().find('.editor-form__text-field:eq(2)'),
            'Päätaso: B'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__checkbox + label:contains("Pakollinen")'
            )
          )
        )
        it('has expected contents', () => {
          expect(formComponents()).to.have.length(1)
          expect(
            formComponents().find('.editor-form__text-field:eq(0)').val()
          ).to.equal('Päätaso: pudotusvalikko')
          expect(
            formComponents()
              .find('.editor-form__checkbox-container input')
              .prop('checked')
          ).to.equal(true)
          expect(
            formComponents().find('.editor-form__text-field:eq(1)').val()
          ).to.equal('Päätaso: A')
          expect(
            formComponents().find('.editor-form__text-field:eq(2)').val()
          ).to.equal('Päätaso: B')
        })
      })

      describe('available followup components for dropdown', () => {
        before(
          clickElement(() =>
            formComponents()
              .eq(0)
              .find(
                '.editor-form__followup-question:eq(1) a:contains("Lisäkysymykset")'
              )
          ),
          wait.until(() => {
            triggerEvent(
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__add-component-toolbar'
              ),
              'mouseover'
            )
            return true
          })
        )
        it('opens up toolbar for adding followup components', () => {
          expect(
            _.map(
              formComponents().find(
                '.editor-form__followup-question-overlay .form__add-component-toolbar--list-item a'
              ),
              (e) => $(e).text()
            )
          ).to.eql([
            'Painikkeet, yksi valittavissa',
            'Painikkeet, yksi valittavissa, koodisto',
            'Pudotusvalikko',
            'Pudotusvalikko, koodisto',
            'Lista, monta valittavissa',
            'Lista, monta valittavissa, koodisto',
            'Tekstikenttä',
            'Tekstialue',
            'Vierekkäiset tekstikentät',
            'Liitepyyntö',
            'Kysymysryhmä',
            'Infoteksti',
          ])
        })
      })

      describe('adding question group as a followup element', () => {
        before(
          clickSubComponentMenuItem(
            () => formComponents().eq(0),
            'Kysymysryhmä'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__text-field'
              ),
            'Kysymysryhmä: ryhmän otsikko'
          ),
          wait.until(() => {
            triggerEvent(
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__add-component-toolbar'
              ),
              'mouseover'
            )
            return true
          })
        )
        it('adds question group as a followup element', () => {
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-main-header'
              )
              .text()
          ).to.equal('Kysymysryhmä')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__text-field'
              )
              .val()
          ).to.equal('Kysymysryhmä: ryhmän otsikko')
          expect(
            _.map(
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .form__add-component-toolbar--list-item a'
              ),
              (e) => $(e).text()
            )
          ).to.eql([
            'Painikkeet, yksi valittavissa',
            'Painikkeet, yksi valittavissa, koodisto',
            'Pudotusvalikko',
            'Pudotusvalikko, koodisto',
            'Lista, monta valittavissa',
            'Lista, monta valittavissa, koodisto',
            'Tekstikenttä',
            'Tekstialue',
            'Vierekkäiset tekstikentät',
            'Liitepyyntö',
            'Infoteksti',
          ])
        })
      })

      describe('adding dropdown as element to a question group', () => {
        before(
          addQuestionGroupElement('Pudotusvalikko'),
          setTextFieldValueInNestedQuestion(
            0,
            0,
            'Kysymysryhmä: pudotusvalikko'
          ),
          setTextFieldValueInNestedQuestion(0, 1, 'Pudotusvalikko: A'),
          setTextFieldValueInNestedQuestion(0, 2, 'Pudotusvalikko: B'),
          clickCheckboxWithLabelInNestedQuestion(0, 'Pakollinen')
        )
        it('adds dropdown as element to a question group', () => {
          expect(headerInNestedQuestion(0).text()).to.equal('Pudotusvalikko')
          expect(textFieldInNestedQuestion(0, 0).val()).to.equal(
            'Kysymysryhmä: pudotusvalikko'
          )
          expect(textFieldInNestedQuestion(0, 1).val()).to.equal(
            'Pudotusvalikko: A'
          )
          expect(textFieldInNestedQuestion(0, 2).val()).to.equal(
            'Pudotusvalikko: B'
          )
          expect(
            checkboxInNestedQuestion(0, 'Pakollinen').prop('checked')
          ).to.equal(true)
        })
      })

      describe('adding single choice button as element to a question group', () => {
        before(
          addQuestionGroupElement('Painikkeet, yksi valittavissa'),
          setTextFieldValueInNestedQuestion(
            1,
            0,
            'Kysymysryhmä: painikkeet, yksi valittavissa'
          ),
          clickAddDropdownItemInNestedQuestion(1),
          setTextFieldValueInNestedQuestion(
            1,
            1,
            'Painikkeet, yksi valittavissa: A'
          ),
          clickAddDropdownItemInNestedQuestion(1),
          setTextFieldValueInNestedQuestion(
            1,
            2,
            'Painikkeet, yksi valittavissa: B'
          ),
          clickCheckboxWithLabelInNestedQuestion(1, 'Pakollinen')
        )
        it('adds single choice button as element to a question group', () => {
          expect(headerInNestedQuestion(1).text()).to.equal(
            'Painikkeet, yksi valittavissa'
          )
          expect(textFieldInNestedQuestion(1, 0).val()).to.equal(
            'Kysymysryhmä: painikkeet, yksi valittavissa'
          )
          expect(textFieldInNestedQuestion(1, 1).val()).to.equal(
            'Painikkeet, yksi valittavissa: A'
          )
          expect(textFieldInNestedQuestion(1, 2).val()).to.equal(
            'Painikkeet, yksi valittavissa: B'
          )
          expect(
            checkboxInNestedQuestion(1, 'Pakollinen').prop('checked')
          ).to.equal(true)
        })
      })

      describe('adding multiple choice button as element to a question group', () => {
        before(
          addQuestionGroupElement('Lista, monta valittavissa'),
          setTextFieldValueInNestedQuestion(
            2,
            0,
            'Kysymysryhmä: lista, monta valittavissa'
          ),
          clickAddDropdownItemInNestedQuestion(2),
          setTextFieldValueInNestedQuestion(
            2,
            1,
            'Lista, monta valittavissa: A'
          ),
          clickAddDropdownItemInNestedQuestion(2),
          setTextFieldValueInNestedQuestion(
            2,
            2,
            'Lista, monta valittavissa: B'
          ),
          clickCheckboxWithLabelInNestedQuestion(2, 'Pakollinen')
        )
        it('adds multiple choice button as element to a question group', () => {
          expect(headerInNestedQuestion(2).text()).to.equal(
            'Lista, monta valittavissa'
          )
          expect(textFieldInNestedQuestion(2, 0).val()).to.equal(
            'Kysymysryhmä: lista, monta valittavissa'
          )
          expect(textFieldInNestedQuestion(2, 1).val()).to.equal(
            'Lista, monta valittavissa: A'
          )
          expect(textFieldInNestedQuestion(2, 2).val()).to.equal(
            'Lista, monta valittavissa: B'
          )
          expect(
            checkboxInNestedQuestion(2, 'Pakollinen').prop('checked')
          ).to.equal(true)
        })
      })

      describe('adding single-answer text field as element to a question group', () => {
        before(
          addQuestionGroupElement('Tekstikenttä'),
          setTextFieldValueInNestedQuestion(3, 0, 'Tekstikenttä, yksi vastaus'),
          clickCheckboxWithLabelInNestedQuestion(3, 'Pakollinen')
        )
        it('adds single-answer text field as element to a question group', () => {
          expect(headerInNestedQuestion(3).text()).to.equal('Tekstikenttä')
          expect(textFieldInNestedQuestion(3, 0).val()).to.equal(
            'Tekstikenttä, yksi vastaus'
          )
          expect(
            checkboxInNestedQuestion(3, 'Pakollinen').prop('checked')
          ).to.equal(true)
          expect(
            checkboxInNestedQuestion(3, 'useita vastauksia').prop('checked')
          ).to.equal(false)
        })
      })

      describe('adding multi-answer text field as element to a question group', () => {
        before(
          addQuestionGroupElement('Tekstikenttä'),
          setTextFieldValueInNestedQuestion(
            4,
            0,
            'Tekstikenttä, monta vastausta'
          ),
          clickCheckboxWithLabelInNestedQuestion(4, 'Pakollinen'),
          clickCheckboxWithLabelInNestedQuestion(4, 'useita vastauksia')
        )
        it('adds multi-answer text field as element to a question group', () => {
          expect(headerInNestedQuestion(4).text()).to.equal('Tekstikenttä')
          expect(textFieldInNestedQuestion(4, 0).val()).to.equal(
            'Tekstikenttä, monta vastausta'
          )
          expect(
            checkboxInNestedQuestion(4, 'Pakollinen').prop('checked')
          ).to.equal(true)
          expect(
            checkboxInNestedQuestion(4, 'useita vastauksia').prop('checked')
          ).to.equal(true)
        })
      })

      describe('adding text area as element to a question group', () => {
        before(
          addQuestionGroupElement('Tekstialue'),
          setTextFieldValueInNestedQuestion(5, 0, 'Tekstialue'),
          clickCheckboxWithLabelInNestedQuestion(5, 'Pakollinen')
        )
        it('adds text area as element to a question group', () => {
          expect(headerInNestedQuestion(5).text()).to.equal('Tekstialue')
          expect(textFieldInNestedQuestion(5, 0).val()).to.equal('Tekstialue')
          expect(
            checkboxInNestedQuestion(5, 'Pakollinen').prop('checked')
          ).to.equal(true)
        })
      })

      describe('adding single-answer adjacent text field to a question group', () => {
        before(
          addQuestionGroupElement('Vierekkäiset tekstikentät'),
          setTextFieldValueInNestedQuestion(
            6,
            0,
            'Vierekkäiset tekstikentät, yksi vastaus'
          ),
          addAdjacentTextFieldInNestedQuestion(6),
          setTextFieldValueInNestedQuestion(
            6,
            1,
            'Vierekkäiset tekstikentät, yksi vastaus: A'
          ),
          clickCheckboxWithLabelInNestedQuestion(6, 'Pakollinen', 0),
          addAdjacentTextFieldInNestedQuestion(6),
          setTextFieldValueInNestedQuestion(
            6,
            2,
            'Vierekkäiset tekstikentät, yksi vastaus: B'
          ),
          clickCheckboxWithLabelInNestedQuestion(6, 'Pakollinen', 1)
        )
        it('adds single-answer adjacent text field as element to a question group', () => {
          expect(headerInNestedQuestion(6).text()).to.equal(
            'Vierekkäiset tekstikentät'
          )
          expect(textFieldInNestedQuestion(6, 0).val()).to.equal(
            'Vierekkäiset tekstikentät, yksi vastaus'
          )
          expect(
            checkboxInNestedQuestion(6, 'useita vastauksia').prop('checked')
          ).to.equal(false)
          expect(textFieldInNestedQuestion(6, 1).val()).to.equal(
            'Vierekkäiset tekstikentät, yksi vastaus: A'
          )
          expect(
            checkboxInNestedQuestion(6, 'Pakollinen', 0).prop('checked')
          ).to.equal(true)
          expect(
            checkboxInNestedQuestion(6, 'vain numeroita', 0).prop('checked')
          ).to.equal(false)
          expect(textFieldInNestedQuestion(6, 2).val()).to.equal(
            'Vierekkäiset tekstikentät, yksi vastaus: B'
          )
          expect(
            checkboxInNestedQuestion(6, 'Pakollinen', 1).prop('checked')
          ).to.equal(true)
          expect(
            checkboxInNestedQuestion(6, 'vain numeroita', 1).prop('checked')
          ).to.equal(false)
          expect(
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container .form__add-component-toolbar--list-item a:contains("Pohjakoulutusmoduuli")'
            ).length
          ).to.equal(0)
        })
      })

      describe('adding multi-answer adjacent text field to a question group', () => {
        before(
          addQuestionGroupElement('Vierekkäiset tekstikentät'),
          setTextFieldValueInNestedQuestion(
            7,
            0,
            'Vierekkäiset tekstikentät, monta vastausta'
          ),
          clickCheckboxWithLabelInNestedQuestion(7, 'useita vastauksia'),
          addAdjacentTextFieldInNestedQuestion(7),
          setTextFieldValueInNestedQuestion(
            7,
            1,
            'Vierekkäiset tekstikentät, monta vastausta: A'
          ),
          clickCheckboxWithLabelInNestedQuestion(7, 'Pakollinen', 0),
          addAdjacentTextFieldInNestedQuestion(7),
          setTextFieldValueInNestedQuestion(
            7,
            2,
            'Vierekkäiset tekstikentät, monta vastausta: B'
          ),
          clickCheckboxWithLabelInNestedQuestion(7, 'Pakollinen', 1)
        )
        it('adds multi-answer adjacent text field as element to a question group', () => {
          expect(headerInNestedQuestion(7).text()).to.equal(
            'Vierekkäiset tekstikentät'
          )
          expect(textFieldInNestedQuestion(7, 0).val()).to.equal(
            'Vierekkäiset tekstikentät, monta vastausta'
          )
          expect(
            checkboxInNestedQuestion(7, 'useita vastauksia').prop('checked')
          ).to.equal(true)
          expect(textFieldInNestedQuestion(7, 1).val()).to.equal(
            'Vierekkäiset tekstikentät, monta vastausta: A'
          )
          expect(
            checkboxInNestedQuestion(7, 'Pakollinen', 0).prop('checked')
          ).to.equal(true)
          expect(
            checkboxInNestedQuestion(7, 'vain numeroita', 0).prop('checked')
          ).to.equal(false)
          expect(textFieldInNestedQuestion(7, 2).val()).to.equal(
            'Vierekkäiset tekstikentät, monta vastausta: B'
          )
          expect(
            checkboxInNestedQuestion(7, 'Pakollinen', 1).prop('checked')
          ).to.equal(true)
          expect(
            checkboxInNestedQuestion(7, 'vain numeroita', 1).prop('checked')
          ).to.equal(false)
          expect(
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container .form__add-component-toolbar--list-item a:contains("Pohjakoulutusmoduuli")'
            ).length
          ).to.equal(0)
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
          }, 5000)
        )
        it('notification shows success', () => {
          expect(testFrame().find('.top-banner .flasher span').text()).to.equal(
            'Kaikki muutokset tallennettu'
          )
        })
      })
    })
  })
})()
