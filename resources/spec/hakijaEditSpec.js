;(() => {
  before(() => {
    if (!testFormApplicationSecret) {
      console.log(
        'Test application secret undefined (no application found). Did you run virkailija and hakija-form tests first?'
      )
    } else {
      console.log('secret', testFormApplicationSecret)
      loadInFrame('/hakemus?modify=' + testFormApplicationSecret)
    }
  })

  describe('hakemus edit', () => {
    describe('form loads', () => {
      before(
        wait.until(() => {
          return formSections().length == 2
        }),
        wait.until(() => {
          return testFrame().find('#postal-office').val() === 'JYVÄSKYLÄ'
        })
      )
      it('with complete form', () => {
        expect(formFields().length).to.equal(34)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(submitButton().prop('disabled')).to.equal(true)
      })

      it('with correct existing answers', () => {
        const textInputValues = _.map(
          testFrame().find('.application__form-text-input'),
          (e) => {
            return $(e).val()
          }
        )
        const expectedTestInputValues = [
          'Etunimi Tokanimi',
          'Etunimi',
          'Sukunimi',
          '***********',
          'test@example.com',
          'test@example.com',
          '0123456789',
          'Katutie 12 B',
          '40100',
          'JYVÄSKYLÄ',
          'Tekstikentän vastaus',
          'Toistuva vastaus 1',
          'Toistuva vastaus 2',
          'Toistuva vastaus 3',
          '',
          'Pakollisen tekstialueen vastaus',
          'Jatkokysymyksen vastaus',
          'A1',
          'B1',
          'C1',
          'A2',
          '',
          'C2',
          'Toisen pakollisen tekstialueen vastaus',
          '',
          'A1',
          'B1',
          'C1',
          'A2',
          '',
          'C2',
          'Vasen vierekkäinen',
          'Oikea vierekkäinen',
          'A1',
          'B1',
          'C1',
          'A2',
          '',
          'C2',
          '1,323',
        ]

        const dropdownInputValues = _.map(
          testFrame().find('select.application__form-select option:selected'),
          (e) => {
            return $(e).text()
          }
        )
        const expectedDropdownInputValues = [
          'Suomi',
          'Suomi',
          'Jyväskylä',
          'suomi',
          'Kolmas vaihtoehto',
          'Lisensiaatin tutkinto',
          '',
          'Pudotusvalikon 1. kysymys',
          'Entinen Neuvostoliitto',
        ]

        expect(textInputValues).to.eql(expectedTestInputValues)
        expect(dropdownInputValues).to.eql(expectedDropdownInputValues)
        expect(
          _.map(
            testFrame().find(
              'input.application__form-checkbox:checked + label'
            ),
            (e) => {
              return $(e).text()
            }
          )
        ).to.eql([
          'Toinen vaihtoehto',
          'Arkkitehti',
          'Jatkokysymys A',
          'Jatkokysymys B',
        ])
      })
    })

    describe('changing values to be invalid', () => {
      before(
        setNthFieldInputValue(1, '420noscope'),
        setNthFieldValue(23, 'textarea', ''),
        clickNthFieldRadio(26, 'Ensimmäinen vaihtoehto'),
        clickElement(invalidFieldsStatus),
        wait.until(submitButtonDisabled),
        wait.until(() => {
          return invalidFieldsStatus().text() === 'Tarkista 2 tietoa'
        })
      )

      it('shows invalidity errors', () => {
        expect(invalidFieldNames().join(';')).to.equal(
          'Osiokysymys;Lyhyen listan kysymys'
        )
      })
    })

    describe('change values and save', () => {
      before(
        setNthFieldInputValue(1, 'Etunimi'),
        blurField(() => {
          return formFields().eq(1).find('input')
        }),
        setNthFieldValue(23, 'textarea', 'Muokattu vastaus'),
        clickNthFieldRadio(26, 'Toinen vaihtoehto'),
        wait.until(() => {
          return !submitButton().prop('disabled')
        }),
        clickElement(submitButton),
        wait.until(() => {
          return (
            testFrame().find('.application__sent-placeholder-text').length == 1
          )
        })
      )

      it('shows submitted form', () => {
        const displayedValues = _.map(
          testFrame().find('.application__text-field-paragraph'),
          (e) => {
            return $(e).text()
          }
        )
        console.log('values')
        console.log(displayedValues)
        const expectedValues = [
          'Etunimi Tokanimi',
          'Etunimi',
          'Sukunimi',
          'Suomi',
          '***********',
          'test@example.com',
          '0123456789',
          'Suomi',
          'Katutie 12 B',
          '40100',
          'JYVÄSKYLÄ',
          'Jyväskylä',
          'suomi',
          'Tekstikentän vastaus',
          'Toistuva vastaus 1',
          'Toistuva vastaus 2',
          'Toistuva vastaus 3',
          'Pakollisen tekstialueen vastaus',
          'Kolmas vaihtoehto',
          'Jatkokysymyksen vastaus',
          'Lisensiaatin tutkinto',
          'Toinen vaihtoehto',
          'En',
          'Arkkitehti',
          'Muokattu vastaus',
          '',
          'Toinen vaihtoehto',
          'Pudotusvalikon 1. kysymys',
          '1,323',
          'Entinen Neuvostoliitto',
        ]

        const tabularValues = _.map(
          testFrame().find('.application__form-field table td'),
          (e) => {
            return $(e).text()
          }
        )
        const expectedTabularValues = [
          'A1',
          'B1',
          'C1',
          'A2',
          '',
          'C2',
          'Vasen vierekkäinen',
          'Oikea vierekkäinen',
          'A1',
          'B1',
          'C1',
          'A2',
          '',
          'C2',
        ]

        expect(displayedValues).to.eql(expectedValues)
        expect(tabularValues).to.eql(expectedTabularValues)
      })
    })
  })
})()
