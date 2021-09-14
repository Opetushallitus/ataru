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
            formComponents().find('.editor-form__checkbox + label')
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
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper'
              ),
            'Pudotusvalikko'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(1)'
              ),
            'Kysymysryhmä: pudotusvalikko'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(2)'
              ),
            'Pudotusvalikko: A'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(3)'
              ),
            'Pudotusvalikko: B'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label'
            )
          )
        )
        it('adds dropdown as element to a question group', () => {
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(1)'
              )
              .text()
          ).to.equal('Pudotusvalikko')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(1)'
              )
              .val()
          ).to.equal('Kysymysryhmä: pudotusvalikko')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(2)'
              )
              .val()
          ).to.equal('Pudotusvalikko: A')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(3)'
              )
              .val()
          ).to.equal('Pudotusvalikko: B')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox'
              )
              .prop('checked')
          ).to.equal(true)
        })
      })

      describe('adding single choice button as element to a question group', () => {
        before(
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper'
              ),
            'Painikkeet, yksi valittavissa'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(4)'
              ),
            'Kysymysryhmä: painikkeet, yksi valittavissa'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__add-dropdown-item a:contains("Lisää"):eq(1)'
            )
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(5)'
              ),
            'Painikkeet, yksi valittavissa: A'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__add-dropdown-item a:contains("Lisää"):eq(1)'
            )
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(6)'
              ),
            'Painikkeet, yksi valittavissa: B'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(1)'
            )
          )
        )
        it('adds single choice button as element to a question group', () => {
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(2)'
              )
              .text()
          ).to.equal('Painikkeet, yksi valittavissa')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(4)'
              )
              .val()
          ).to.equal('Kysymysryhmä: painikkeet, yksi valittavissa')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(5)'
              )
              .val()
          ).to.equal('Painikkeet, yksi valittavissa: A')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(6)'
              )
              .val()
          ).to.equal('Painikkeet, yksi valittavissa: B')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(1)'
              )
              .prop('checked')
          ).to.equal(true)
        })
      })

      describe('adding multiple choice button as element to a question group', () => {
        before(
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper'
              ),
            'Lista, monta valittavissa'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(7)'
              ),
            'Kysymysryhmä: lista, monta valittavissa'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__add-dropdown-item a:contains("Lisää"):eq(2)'
            )
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(8)'
              ),
            'Lista, monta valittavissa: A'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__add-dropdown-item a:contains("Lisää"):eq(2)'
            )
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(9)'
              ),
            'Lista, monta valittavissa: B'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(2)'
            )
          )
        )
        it('adds multiple choice button as element to a question group', () => {
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(3)'
              )
              .text()
          ).to.equal('Lista, monta valittavissa')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(7)'
              )
              .val()
          ).to.equal('Kysymysryhmä: lista, monta valittavissa')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(8)'
              )
              .val()
          ).to.equal('Lista, monta valittavissa: A')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(9)'
              )
              .val()
          ).to.equal('Lista, monta valittavissa: B')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(2)'
              )
              .prop('checked')
          ).to.equal(true)
        })
      })

      describe('adding single-answer text field as element to a question group', () => {
        before(
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper'
              ),
            'Tekstikenttä'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(10)'
              ),
            'Tekstikenttä, yksi vastaus'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(3)'
            )
          )
        )
        it('adds single-answer text field as element to a question group', () => {
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(4)'
              )
              .text()
          ).to.equal('Tekstikenttä')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(10)'
              )
              .val()
          ).to.equal('Tekstikenttä, yksi vastaus')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(3)'
              )
              .prop('checked')
          ).to.equal(true) // required
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(4)'
              )
              .prop('checked')
          ).to.equal(false) // multiple answers
        })
      })

      describe('adding multi-answer text field as element to a question group', () => {
        before(
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper'
              ),
            'Tekstikenttä'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(11)'
              ),
            'Tekstikenttä, monta vastausta'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(6)'
            )
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(7)'
            )
          )
        )
        it('adds multi-answer text field as element to a question group', () => {
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(5)'
              )
              .text()
          ).to.equal('Tekstikenttä')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(11)'
              )
              .val()
          ).to.equal('Tekstikenttä, monta vastausta')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(6)'
              )
              .prop('checked')
          ).to.equal(true) // required
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(7)'
              )
              .prop('checked')
          ).to.equal(true) // multiple answers
        })
      })

      describe('adding text area as element to a question group', () => {
        before(
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper'
              ),
            'Tekstialue'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(12)'
              ),
            'Tekstialue'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox + label:eq(9)'
            )
          )
        )
        it('adds text area as element to a question group', () => {
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(6)'
              )
              .text()
          ).to.equal('Tekstialue')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(12)'
              )
              .val()
          ).to.equal('Tekstialue')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(9)'
              )
              .prop('checked')
          ).to.equal(true) // required
        })
      })

      describe('adding single-answer adjacent text field to a question group', () => {
        before(
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper'
              ),
            'Vierekkäiset tekstikentät'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(14)'
              ),
            'Vierekkäiset tekstikentät, yksi vastaus'
          ),
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container'
              ),
            'Tekstikenttä'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(15)'
              ),
            'Vierekkäiset tekstikentät, yksi vastaus: A'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(11) + label'
            )
          ),
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container'
              ),
            'Tekstikenttä'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(16)'
              ),
            'Vierekkäiset tekstikentät, yksi vastaus: B'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(13) + label'
            )
          )
        )
        it('adds single-answer adjacent text field as element to a question group', () => {
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(7)'
              )
              .text()
          ).to.equal('Vierekkäiset tekstikentät')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(14)'
              )
              .val()
          ).to.equal('Vierekkäiset tekstikentät, yksi vastaus')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(10)'
              )
              .prop('checked')
          ).to.equal(false) // multiple answers
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(15)'
              )
              .val()
          ).to.equal('Vierekkäiset tekstikentät, yksi vastaus: A')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(11)' //pakollinen tieto
              )
              .prop('checked')
          ).to.equal(true)
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(12)' //vain numeroita
              )
              .prop('checked')
          ).to.equal(false)
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(16)'
              )
              .val()
          ).to.equal('Vierekkäiset tekstikentät, yksi vastaus: B')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(13)' //pakollinen tieto
              )
              .prop('checked')
          ).to.equal(true)
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(14)' //vain numeroita
              )
              .prop('checked')
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
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper'
              ),
            'Vierekkäiset tekstikentät'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(17)'
              ),
            'Vierekkäiset tekstikentät, monta vastausta'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(15) + label'
            )
          ),
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container:eq(1)'
              ),
            'Tekstikenttä'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(18)'
              ),
            'Vierekkäiset tekstikentät, monta vastausta: A'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(16) + label'
            )
          ),
          clickSubComponentMenuItem(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__adjacent-fieldset-container:eq(1)'
              ),
            'Tekstikenttä'
          ),
          setTextFieldValue(
            () =>
              formComponents().find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(19)'
              ),
            'Vierekkäiset tekstikentät, monta vastausta: B'
          ),
          clickElement(() =>
            formComponents().find(
              '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(18) + label'
            )
          )
        )
        it('adds multi-answer adjacent text field as element to a question group', () => {
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__component-main-header:eq(10)'
              )
              .text()
          ).to.equal('Vierekkäiset tekstikentät')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(17)'
              )
              .val()
          ).to.equal('Vierekkäiset tekstikentät, monta vastausta')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(15)'
              )
              .prop('checked')
          ).to.equal(true) // multiple answers
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(18)'
              )
              .val()
          ).to.equal('Vierekkäiset tekstikentät, monta vastausta: A')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(16)'
              )
              .prop('checked')
          ).to.equal(true)
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__text-field:eq(19)'
              )
              .val()
          ).to.equal('Vierekkäiset tekstikentät, monta vastausta: B')
          expect(
            formComponents()
              .find(
                '.editor-form__followup-question-overlay .editor-form__component-wrapper .editor-form__checkbox:eq(18)'
              )
              .prop('checked')
          ).to.equal(true)
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
