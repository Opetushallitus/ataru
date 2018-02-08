(function() {
  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('hakemus', function() {
    describe('form loads', function() {
      before(
        newForm('testQuestionGroupForm'),
        wait.until(function() { return formSections().length == 1 })
      )
      it('loads form with question groups', function() {
        expect(formFields().length).to.equal(14)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('Kysymysryhmä: testilomake')
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 11 tietoa')
      })
    })

    describe('person info module', function() {
      before(
        setNthFieldInputValue(0, 'Etunimi Tokanimi'),
        setNthFieldInputValue(2, 'Sukunimi'),
        setNthFieldInputValue(4, '020202A0202'),
        setNthFieldInputValue(5, 'test@example.com'),
        setNthFieldInputValue(6, '050123'),
        setNthFieldInputValue(8, 'Katutie 12 B'),
        setNthFieldInputValue(9, '40100'),
        setNthFieldOption(11, '179'),
        wait.until(function() {
          return formFields().eq(10).find('input').val() !== ''
        })
      )
      it('is filled with valid answers', function() {
        // person info module intentionally not verified in detail in this test
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 1 tietoa')
      })
    })

    describe('question group', function() {
      describe('selecting dropdown element having question group as a followup question', function() {
        before(
          setNthFieldOption(13, 'Päätaso: B')
        )
        it('shows the question group as a followup element', function() {
          expect(formFields().find('.application__form-dropdown-followups .application__question-group-row').length).to.equal(1)
          expect(invalidFieldsStatus().text()).to.equal('Tarkista 10 tietoa')
        })
      })

      describe('adding new question group row', function() {
        before(
          clickElement(function() { return testFrame().find('.application__add-question-group-row a:contains("Lisää")') })
        )
        it('adds new set of answers to the question group', function() {
          expect(invalidFieldsStatus().text()).to.equal('Tarkista 10 tietoa')
        })
      })

      describe('answering to a dropdown question inside a question group', function() {
        before(
          setNthFieldOption(14, 'Pudotusvalikko: A'),
          setNthFieldOption(22, 'Pudotusvalikko: B')
        )
        it('shows the followup question as answered', function() {
          expect(formFields().eq(14).find('.application__form-select option:selected').text()).to.equal('Pudotusvalikko: A')
          expect(formFields().eq(22).find('.application__form-select option:selected').text()).to.equal('Pudotusvalikko: B')
          expect(invalidFieldsStatus().text()).to.equal('Tarkista 9 tietoa')
        })
      })

      describe('answering to a single-choice button inside a question group', function() {
        before(
          clickNthFieldRadio(15, 'Painikkeet, yksi valittavissa: A'),
          clickNthFieldRadio(23, 'Painikkeet, yksi valittavissa: B')
        )
        it('shows the single-choice question as answered', function() {
          expect(formFields().eq(15).find('.application__form-single-choice-button:checked').val()).to.equal('Painikkeet, yksi valittavissa: A')
          expect(formFields().eq(23).find('.application__form-single-choice-button:checked').val()).to.equal('Painikkeet, yksi valittavissa: B')
          expect(invalidFieldsStatus().text()).to.equal('Tarkista 8 tietoa')
        })
      })

      describe('answering to a multi-choice button inside a question group', function() {
        before(
          clickNthFieldRadio(16, 'Lista, monta valittavissa: A'),
          clickNthFieldRadio(16, 'Lista, monta valittavissa: B'),
          clickNthFieldRadio(24, 'Lista, monta valittavissa: B')
        )
        it('shows the multi-choice question as answered', function() {
          expect(formFields().eq(16).find('.application__form-checkbox:checked:eq(0)').val()).to.equal('Lista, monta valittavissa: A')
          expect(formFields().eq(16).find('.application__form-checkbox:checked:eq(1)').val()).to.equal('Lista, monta valittavissa: B'),
          expect(formFields().eq(24).find('.application__form-checkbox:checked').val()).to.equal('Lista, monta valittavissa: B')
          expect(invalidFieldsStatus().text()).to.equal('Tarkista 7 tietoa')
        })
      })

      describe('answering to a single-answer text field inside a question group', function() {
        before(
          setNthFieldInputValue(17, 'Tekstikenttä, yksi vastaus: A'),
          setNthFieldInputValue(25, 'Tekstikenttä, yksi vastaus: B')
        )
        it('shows the single-answer text field as answered', function() {
          expect(formFields().eq(17).find('.application__form-text-input').val()).to.equal('Tekstikenttä, yksi vastaus: A')
          expect(formFields().eq(25).find('.application__form-text-input').val()).to.equal('Tekstikenttä, yksi vastaus: B')
          expect(invalidFieldsStatus().text()).to.equal('Tarkista 6 tietoa')
        })
      })

      describe('answering to a multi-value text field inside a question group', function() {
        before(
          setNthFieldSubInputValue(18, 0, 'Tekstikenttä, monta vastausta: A'),
          setNthFieldSubInputValue(18, 1, 'Tekstikenttä, monta vastausta: B'),
          setNthFieldSubInputValue(26, 0, 'Tekstikenttä, monta vastausta: C'),
          setNthFieldSubInputValue(26, 1, 'Tekstikenttä, monta vastausta: D')
        )
        it('shows the multi-value text field inside a question group as answered', function() {
          expect(formFields().eq(18).find('.application__form-text-input:eq(0)').val()).to.equal('Tekstikenttä, monta vastausta: A')
          expect(formFields().eq(18).find('.application__form-text-input:eq(1)').val()).to.equal('Tekstikenttä, monta vastausta: B')
          expect(formFields().eq(26).find('.application__form-text-input:eq(0)').val()).to.equal('Tekstikenttä, monta vastausta: C')
          expect(formFields().eq(26).find('.application__form-text-input:eq(1)').val()).to.equal('Tekstikenttä, monta vastausta: D')
          expect(invalidFieldsStatus().text()).to.equal('Tarkista 5 tietoa')
        })
      })

      describe('answering to a text area inside a question group', function() {
        before(
          setNthFieldValue(19, 'textarea', 'Tekstialue: AAAAA'),
          setNthFieldValue(27, 'textarea', 'Tekstialue: BBBBB')
        )
        it('shows the text area inside a question group as answered', function() {
          expect(formFields().eq(19).find('.application__form-text-input').val()).to.equal('Tekstialue: AAAAA')
          expect(formFields().eq(27).find('.application__form-text-input').val()).to.equal('Tekstialue: BBBBB')
          expect(invalidFieldsStatus().text()).to.equal('Tarkista 4 tietoa')
        })
      })

      describe('answering to a single-answer adjacent text field inside a question group', function() {
        before(
          setNthFieldSubInputValue(20, 0, 'Vierekkäiset tekstikentät, yksi vastaus: vastaus A'),
          setNthFieldSubInputValue(20, 1, 'Vierekkäiset tekstikentät, yksi vastaus: vastaus B'),
          setNthFieldSubInputValue(28, 0, 'Vierekkäiset tekstikentät, yksi vastaus: vastaus C'),
          setNthFieldSubInputValue(28, 1, 'Vierekkäiset tekstikentät, yksi vastaus: vastaus D')
        )
        it('shows the single-answer adjacent text field inside a question group as answered', function() {
          expect(formFields().eq(20).find('.application__form-text-input:eq(0)').val()).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus A')
          expect(formFields().eq(20).find('.application__form-text-input:eq(1)').val()).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus B')
          expect(formFields().eq(28).find('.application__form-text-input:eq(0)').val()).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus C')
          expect(formFields().eq(28).find('.application__form-text-input:eq(1)').val()).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus D')
          expect(invalidFieldsStatus().text()).to.equal('Tarkista 2 tietoa')
        })
      })

      describe('answering to a multi-answer adjacent text field inside a question group', function() {
        before(
          setNthFieldSubInputValue(21, 0, 'Vierekkäiset tekstikentät, monta vastausta: vastaus A1'),
          setNthFieldSubInputValue(21, 1, 'Vierekkäiset tekstikentät, monta vastausta: vastaus B1'),
          clickElement(function() { return formFields().eq(21).find('a.application__form-add-new-row:contains("Lisää rivi")') }),
          setNthFieldSubInputValue(21, 2, 'Vierekkäiset tekstikentät, monta vastausta: vastaus A2'),
          setNthFieldSubInputValue(21, 3, 'Vierekkäiset tekstikentät, monta vastausta: vastaus B2'),
          setNthFieldSubInputValue(29, 0, 'Vierekkäiset tekstikentät, monta vastausta: vastaus C1'),
          setNthFieldSubInputValue(29, 1, 'Vierekkäiset tekstikentät, monta vastausta: vastaus D1'),
          clickElement(function() { return formFields().eq(29).find('a.application__form-add-new-row:contains("Lisää rivi")') }),
          setNthFieldSubInputValue(29, 2, 'Vierekkäiset tekstikentät, monta vastausta: vastaus C2'),
          setNthFieldSubInputValue(29, 3, 'Vierekkäiset tekstikentät, monta vastausta: vastaus D2')
        )
        it('shows the multi-answer adjacent text field inside a question group as answered', function() {
          expect(formFields().eq(21).find('.application__form-text-input:eq(0)').val()).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus A1')
          expect(formFields().eq(21).find('.application__form-text-input:eq(1)').val()).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus B1')
          expect(formFields().eq(21).find('.application__form-text-input:eq(2)').val()).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus A2')
          expect(formFields().eq(21).find('.application__form-text-input:eq(3)').val()).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus B2')
          expect(formFields().eq(29).find('.application__form-text-input:eq(0)').val()).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus C1')
          expect(formFields().eq(29).find('.application__form-text-input:eq(1)').val()).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus D1')
          expect(formFields().eq(29).find('.application__form-text-input:eq(2)').val()).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus C2')
          expect(formFields().eq(29).find('.application__form-text-input:eq(3)').val()).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus D2')
          expect(invalidFieldsStatus().text()).to.equal('')
          expect(submitButton().prop('disabled')).to.equal(false)
        })
      })

      describe('submitting the application', function() {
        before(
          clickElement(submitButton),
          wait.until(function() {
            return testFrame().find('.application__status-controls .application__sent-placeholder-text').text() === 'Hakemus lähetetty';
          })
        )
        it('submits the application and shows the feedback form', function() {
          expect(testFrame().find('.application-feedback-form').length).to.equal(1)
        })
      })

      describe('closing the feedback form', function() {
        before(
          clickElement(function() { return testFrame().find('.application-feedback-form__close-button') })
        )
        it('closes the feedback form and shows the read-only view', function() {
          expect(testFrame().find('.application-feedback-form').length).to.equal(0)
          expect(readonlyAnswer(0)).to.equal('Etunimi Tokanimi')
          expect(readonlyAnswer(1)).to.equal('Etunimi')
          expect(readonlyAnswer(2)).to.equal('Sukunimi')
          expect(readonlyAnswer(3)).to.equal('Suomi')
          expect(readonlyAnswer(4)).to.equal('020202A0202')
          expect(readonlyAnswer(5)).to.equal('test@example.com')
          expect(readonlyAnswer(6)).to.equal('050123')
          expect(readonlyAnswer(7)).to.equal('Suomi')
          expect(readonlyAnswer(8)).to.equal('Katutie 12 B')
          expect(readonlyAnswer(9)).to.equal('40100')
          expect(readonlyAnswer(10)).to.equal('JYVÄSKYLÄ')
          expect(readonlyAnswer(11)).to.equal('Jyväskylä')
          expect(readonlyAnswer(12)).to.equal('suomi')
          expect(readonlyAnswer(13)).to.equal('Päätaso: B')
          expect(readonlyAnswer(14)).to.equal('Pudotusvalikko: A')
          expect(readonlyAnswer(15)).to.equal('Painikkeet, yksi valittavissa: A')
          expect(readonlyAnswer(16)).to.equal('Lista, monta valittavissa: ALista, monta valittavissa: B')
          expect(readonlyAnswer(17)).to.equal('Tekstikenttä, yksi vastaus: A')
          expect(readonlyAnswer(18)).to.equal('Tekstikenttä, monta vastausta: ATekstikenttä, monta vastausta: B')
          expect(readonlyAnswer(19)).to.equal('Tekstialue: AAAAA')
          expect(adjacentReadonlyAnswer(0)).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus A')
          expect(adjacentReadonlyAnswer(1)).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus B')
          expect(adjacentReadonlyAnswer(2)).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus A1')
          expect(adjacentReadonlyAnswer(3)).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus B1')
          expect(adjacentReadonlyAnswer(4)).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus A2')
          expect(adjacentReadonlyAnswer(5)).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus B2')

          expect(readonlyAnswer(20)).to.equal('Pudotusvalikko: B')
          expect(readonlyAnswer(21)).to.equal('Painikkeet, yksi valittavissa: B')
          expect(readonlyAnswer(22)).to.equal('Lista, monta valittavissa: B')
          expect(readonlyAnswer(23)).to.equal('Tekstikenttä, yksi vastaus: B')
          expect(readonlyAnswer(24)).to.equal('Tekstikenttä, monta vastausta: CTekstikenttä, monta vastausta: D')
          expect(readonlyAnswer(25)).to.equal('Tekstialue: BBBBB')
          expect(adjacentReadonlyAnswer(6)).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus C')
          expect(adjacentReadonlyAnswer(7)).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus D')
          expect(adjacentReadonlyAnswer(8)).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus C1')
          expect(adjacentReadonlyAnswer(9)).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus D1')
          expect(adjacentReadonlyAnswer(10)).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus C2')
          expect(adjacentReadonlyAnswer(11)).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus D2')
        })
      })
    })
  })
})()
