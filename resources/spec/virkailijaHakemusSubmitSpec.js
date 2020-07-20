;(() => {
  const hakuOid = '1.2.246.562.29.65950024189'

  before(() => {
    loadInFrame(
      '/hakemus/haku/' +
        hakuOid +
        '?virkailija-secret=' +
        virkailijaCreateSecret
    )
  })

  afterEach(() => {
    expect(window.uiError || null).to.be.null
  })

  describe('hakemus as virkailija', () => {
    describe('form loads', () => {
      before(wait.until(() => formSections().length == 3))
      it('with complete form and virkailijatäyttö ribbon', () => {
        expect(
          testFrame()
            .find('.application__virkailija-fill-ribbon')
            .is(':visible')
        ).to.equal(true)
        expect(formFields().length).to.equal(15)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('testing2')
        expect(selectedHakukohteet().length).to.equal(1)
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 10 tietoa')
        expect(hakukohdeSearchInput().is(':visible')).to.equal(false)
      })
    })

    describe('filling form', () => {
      before(
        setNthFieldInputValue(1, 'Virkailijan'),
        blurField(() => {
          return formFields().eq(1).find('input')
        }),
        setNthFieldInputValue(3, 'Täyttämä'),
        setNthFieldInputValue(5, '020202A0202'),
        setNthFieldInputValue(6, 'test@example.com'),
        setFieldInputValue('#verify-email', 'test@example.com'),
        setNthFieldInputValue(7, '0123456789'),
        setNthFieldInputValue(9, 'Katutie 12 B'),
        setNthFieldInputValue(10, '00100'),
        setNthFieldOption(12, '091'),
        setNthFieldInputValue(14, '55cm'),
        wait.until(() => formFields().eq(11).find('input').val() !== ''),
        wait.until(() => !submitButton().prop('disabled'))
      )
      it('validates and shows form correctly', () => {
        expect(selectedHakukohteet().length).to.equal(1)
        expect(invalidFieldsStatus().length).to.equal(0)
      })
    })

    describe('submitting form and viewing results', () => {
      before(
        wait.until(() => !submitButton().prop('disabled')),
        clickElement(() => submitButton()),
        wait.until(
          () =>
            testFrame().find('.application__sent-placeholder-text').length == 1
        )
      )
      it('shows readonly application with selected data', () => {
        expect(
          testFrame()
            .find('.application__hakukohde-selected-list')
            .find('.application__hakukohde-header').length
        ).to.equal(1)
        expect(
          testFrame()
            .find('.application__hakukohde-selected-list')
            .find('.application__hakukohde-header')
            .eq(0)
            .text()
        ).to.equal(
          'Testihakukohde – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie'
        )

        const otherValues = _.map(
          testFrame().find('.application__text-field-paragraph'),
          (e) => $(e).text()
        )
        const expectedOtherValues = [
          'Virkailijan',
          'Virkailijan',
          'Täyttämä',
          'Suomi',
          '020202A0202',
          'test@example.com',
          '0123456789',
          'Suomi',
          'Katutie 12 B',
          '00100',
          'HELSINKI',
          'Helsinki',
          'suomi',
          '55cm',
        ]
        expect(otherValues).to.eql(expectedOtherValues)
      })
    })
  })
})()
