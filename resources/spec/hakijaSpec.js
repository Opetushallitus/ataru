;(() => {
  afterEach(() => {
    expect(window.uiError || null).to.be.null
  })

  describe('hakemus', () => {
    describe('form loads', () => {
      before(
        newForm('testForm'),
        wait.until(() => {
          return formSections().length == 3
        })
      )
      it('with complete form', () => {
        expect(formFields().length).to.equal(28)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 13 tietoa')
        expect(
          formFields()
            .eq(15)
            .find('.application__form-textarea-max-length')
            .text()
        ).to.equal('0 / 2000')
      })
    })

    describe('person info module', () => {
      describe('structure', () => {
        it('has the correct fields', () => {
          const labels = _.map(personInfoModule().find('label'), (e) => {
            return $(e).text()
          })
          const expectedLabels = [
            'Etunimet *',
            'Kutsumanimi *',
            'Sukunimi *',
            'Kansalaisuus *',
            'Henkilötunnus *',
            'Sähköpostiosoite *',
            'Varmista sähköpostiosoite *',
            'Matkapuhelin *',
            'Asuinmaa *',
            'Katuosoite *',
            'Postinumero *',
            'Postitoimipaikka *',
            'Kotikunta *',
            'Äidinkieli *',
          ]

          expect(
            personInfoModule().find('.application__wrapper-heading h2').text()
          ).to.equal('Henkilötiedot')

          expectedLabels.every((e, i) => {
            expect(e).to.equal(labels[i])
          })
        })
      })

      describe('filling out', () => {
        before(
          setNthFieldInputValue(0, 'Etunimi Tokanimi'),
          blurField(() => {
            return formFields().eq(0).find('input')
          }),
          setNthFieldInputValue(2, 'Sukunimi'),
          setNthFieldInputValue(4, '020202A0202'),
          setNthFieldInputValue(5, 'test@example.com'),
          setFieldInputValue('#verify-email', 'test@example.com'),
          setNthFieldInputValue(6, '0123456789'),
          setNthFieldInputValue(8, 'Katutie 12 B'),
          setNthFieldInputValue(9, '40100'),
          setNthFieldOption(11, '179'),
          wait.until(() => {
            return formFields().eq(10).find('input').val() !== ''
          }),
          wait.forMilliseconds(600),
          clickElement(invalidFieldsStatus)
        )
        it('works and validates correctly', () => {
          expect(formFields().eq(1).find('input').val()).to.equal('Etunimi')
          expect(formFields().eq(3).find('select').val()).to.equal('246')
          expect(formFields().eq(10).find('input').val()).to.equal('JYVÄSKYLÄ')
          expect(formFields().eq(12).find('select').val()).to.equal('FI')
          expect(invalidFieldNames().join(';')).to.equal(
            'Toinen kysymys;Osiokysymys;Lyhyen listan kysymys'
          )
          expect(invalidFieldsStatus().text()).to.equal('Tarkista 3 tietoa')
        })
      })
    })

    describe('user-defined fields', () => {
      before(
        setNthFieldInputValue(13, 'Tekstikentän vastaus'),
        setNthFieldInputValue(14, 'Toistuva vastaus 1'),
        setNthFieldSubInputValue(14, 1, 'Toistuva vastaus 2'),
        setNthFieldSubInputValue(14, 2, 'Toistuva vastaus 3'),
        clickElement(() => {
          return formFields()
            .eq(14)
            .find('a.application__form-repeatable-text--addremove')
            .eq(0)
        }),
        setNthFieldValue(15, 'textarea', 'Pakollisen tekstialueen vastaus'),
        setNthFieldOption(16, '2'),
        setNthFieldInputValue(17, 'Jatkokysymyksen vastaus'),
        setNthFieldOption(18, '120'),
        clickNthFieldRadio(19, 'Toinen vaihtoehto', true),
        clickNthFieldRadio(20, 'En'),
        setNthFieldSubInputValue(21, 0, 'A1'),
        setNthFieldSubInputValue(21, 1, 'B1'),
        clickElement(() => {
          return formFields().eq(21).find('.application__form-add-new-row')
        }),
        setNthFieldSubInputValue(21, 2, 'C1'),
        setNthFieldSubInputValue(21, 3, 'A2'),
        setNthFieldSubInputValue(21, 5, 'C2'),
        clickNthFieldRadio(22, 'Arkkitehti', true),
        setNthFieldValue(
          23,
          'textarea',
          'Toisen pakollisen tekstialueen vastaus'
        ),
        clickNthFieldRadio(26, 'Ensimmäinen vaihtoehto'),
        clickNthFieldRadio(27, 'Jatkokysymys A'),
        clickNthFieldRadio(27, 'Jatkokysymys B'),
        setNthFieldSubInputValue(28, 0, 'A1'),
        setNthFieldSubInputValue(28, 1, 'B1'),
        setNthFieldSubInputValue(28, 2, 'C1'),
        clickElement(() => {
          return formFields().eq(28).find('.application__form-add-new-row')
        }),
        setNthFieldSubInputValue(28, 3, 'A2'),
        setNthFieldSubInputValue(28, 5, 'C2'),
        setNthFieldSubInputValue(29, 0, 'Vasen vierekkäinen'),
        setNthFieldSubInputValue(29, 1, 'Oikea vierekkäinen'),
        setNthFieldOption(30, '0'),
        setNthFieldSubInputValue(31, 0, 'A1'),
        setNthFieldSubInputValue(31, 1, 'B1'),
        setNthFieldSubInputValue(31, 2, 'C1'),
        clickElement(() => {
          return formFields().eq(31).find('.application__form-add-new-row')
        }),
        setNthFieldSubInputValue(31, 3, 'A2'),
        setNthFieldSubInputValue(31, 5, 'C2'),
        setNthFieldInputValue(32, '1,323'),
        setNthFieldOption(33, '810'),
        wait.until(() => {
          return !submitButton().prop('disabled')
        })
      )
      it('works and validates correctly', () => {
        expect(invalidFieldNames().join(';')).to.equal('')
        expect(invalidFieldsStatus().length).to.equal(0)
        expect(submitButton().prop('disabled')).to.equal(false)
      })
    })

    describe('submitting', () => {
      before(
        clickElement(() => {
          return submitButton()
        }),
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
          '020202A0202',
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
          'Toisen pakollisen tekstialueen vastaus',
          '',
          'Ensimmäinen vaihtoehto',
          'Jatkokysymys A',
          'Jatkokysymys B',
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
