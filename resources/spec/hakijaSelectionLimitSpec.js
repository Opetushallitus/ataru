(function() {
  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('hakemus', function() {
    describe('form loads', function() {
      before(
        newForm('testSelectionLimitForm'),
        wait.until(function() { return formSections().length == 1 })
      )
      it('loads form with selection limit', function() {
        expect(formFields().length).to.equal(14)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('Selection Limit')
      })
    })

    describe('select selection limited value', function() {
      before(
        setNthFieldInputValue(0, 'Etunimi Tokanimi'),
        blurField(function () { return formFields().eq(0).find('input') }),
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
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 1 tietoa')
      })
    })

    describe('select "Aina tilaa"', function() {
        before(
            clickNthFieldRadio(13, 'Aina tilaa'),
            wait.untilFalse(function() {
                return formFields().eq(13).find('.application__form-single-choice-button:disabled + label').text() === 'Aina täynnäAina tilaaYksi paikka';
            })
        )
        it('is "Aina tilaa" selected', function() {
            expect(formFields().eq(13).find('.application__form-single-choice-button:disabled + label').text()).to.equal('Aina täynnä (ei valittavissa)')
            expect(formFields().eq(13).find('.application__form-single-choice-button:checked + label').text()).to.equal('Aina tilaa')
        })
    })

    describe('select "Yksi paikka"', function() {
        before(
            clickNthFieldRadio(13, 'Yksi paikka'),
            wait.until(function() {
                return formFields().eq(13).find('.application__form-single-choice-button:disabled + label').text() !== 'Aina täynnäAina tilaaYksi paikka';
            })
        )
        it('is "Yksi paikka" selected', function() {
            expect(formFields().eq(13).find('.application__form-single-choice-button:disabled + label').text()).to.equal('Aina täynnä (ei valittavissa)')
            expect(formFields().eq(13).find('.application__form-single-choice-button:checked + label').text()).to.equal('Yksi paikka')
        })
    })

    describe('submit the application', function() {
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
  })
})()
