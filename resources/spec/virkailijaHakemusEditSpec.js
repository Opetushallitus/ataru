;(() => {
  before(() => {
    console.log('virkailijaSecret', virkailijaSecret || 'UNDEFINED')
    loadInFrame('/hakemus?virkailija-secret=' + virkailijaSecret)
  })

  const newPhoneNumber = Math.floor(Math.random() * 10000000).toString()

  describe('Virkailija hakemus edit', () => {
    describe('shows application with secret', () => {
      before(
        wait.until(
          () => formSections().length == 2 && formFields().length == 31
        )
      )
      it('with complete form', () => {
        expect(formFields().length).to.equal(31)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(submitButton().prop('disabled')).to.equal(true)
      })
    })

    describe('change values and save', () => {
      before(
        setNthFieldInputValue(6, newPhoneNumber),
        wait.until(() => !submitButton().prop('disabled')),
        clickElement(submitButton),
        wait.until(
          () =>
            testFrame().find('.application__sent-placeholder-text').length == 1
        )
      )

      it('shows submitted form', () => {
        const displayedValues = _.map(
          testFrame().find('.application__text-field-paragraph'),
          (e) => $(e).text()
        )
        console.log('values')
        console.log(displayedValues)
        const expectedValues = [
          'Etunimi Tokanimi',
          'Etunimi',
          'Sukunimi',
          'Suomi',
          '020202A0202',
          'test@example.com',
          newPhoneNumber,
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
          'Tekniikan lisensiaatti',
          'Pudotusvalikon 1. kysymys',
          '1,323',
          'Entinen Neuvostoliitto',
        ]

        const tabularValues = _.map(
          testFrame().find('.application__form-field table td'),
          (e) => $(e).text()
        )
        console.log('tabularValues')
        console.log(tabularValues)
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

    describe('edit with invalid key', () => {
      before(
        () => loadInFrame('/hakemus?virkailija-secret=' + virkailijaSecret),
        wait.until(
          () => testFrame().find('.application__message-display').length == 1
        )
      )

      it('shows error', () => {
        expect(
          testFrame().find('.application__message-display').text()
        ).to.include('vanhentunut')
      })
    })
  })
})()
