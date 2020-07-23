;(() => {
  const singleHakukohdeHakuOid = '1.2.246.562.29.65950024185'
  const multipleHakukohdeHakuOid = '1.2.246.562.29.65950024186'
  const kkHakuOid = '1.2.246.562.29.65950024190'
  const multipleHakukohdeKkHakuOid = '1.2.246.562.29.65950024191'
  const hakuWithPohjakoulutusMooduliKk = '1.2.246.562.29.65950024192'

  before(() => {
    loadInFrame('/hakemus/haku/' + singleHakukohdeHakuOid)
  })

  afterEach(() => {
    expect(window.uiError || null).to.be.null
  })

  describe('hakemus by haku with single hakukohde', () => {
    describe('form loads', () => {
      before(wait.until(() => formSections().length == 3))
      it('with complete form and the only hakukohde selected', () => {
        expect(formFields().length).to.equal(15)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('testing2')
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 10 tietoa')
        expect(selectedHakukohteet().length).to.equal(1)
        expect(hakukohdeSearchInput().is(':visible')).to.equal(false)
        expect(selectedHakukohdeName(selectedHakukohteet()[0])).to.equal(
          'Ajoneuvonosturinkuljettajan ammattitutkinto – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie'
        )
      })
    })
  })

  describe('hakemus by haku KK', () => {
    describe('with single hakukohde', () => {
      before(
        () => loadInFrame('/hakemus/haku/' + kkHakuOid),
        wait.until(() => formSections().length == 3)
      )
      it('does not show the details', () => {
        expect(selectedHakukohdeTexts()).to.equal(
          'Ajoneuvonosturinkuljettajan ammattitutkinto – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie'
        )
      })
    })
    describe('with multiple hakukohde', () => {
      before(
        () => loadInFrame('/hakemus/haku/' + multipleHakukohdeKkHakuOid),
        wait.until(() => formSections().length == 3)
      )
      describe('adding KK hakukohde', () => {
        before(
          clickElement(addHakukohdeLink),
          setTextFieldValue(hakukohdeSearchInput, 'haku'),
          wait.until(() => hakukohdeSearchHits().length === 3),
          clickElement(() => nthHakukohdeSearchResultButton(2)),
          wait.until(() => selectedHakukohteet().length === 1),
          wait.until(
            () => invalidFieldsStatus().text() === 'Tarkista 10 tietoa'
          )
        )
        it('has correct selected text and search text, with no description', () => {
          expect(submitButton().prop('disabled')).to.equal(true)
          expect(selectedHakukohteet().length).to.equal(1)
          expect(selectedHakukohdeTexts()).to.equal(
            'Testihakukohde 3 – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie'
          )
          expect(searchHakukohdeTexts()).to.equal(
            'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTestihakukohde 3 – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie'
          )
        })
      })
    })
    describe('when yo-tutkinto', () => {
      const firstSegmentLabel = () =>
        formSections()
          .eq(2)
          .find('label:contains("Suomessa suoritettu ylioppilastutkinto")')
      const firstSegmentYesButton = () =>
        formSections()
          .eq(2)
          .find(
            'label:contains("Suomessa suoritettu ylioppilastutkinto") + div'
          )
          .find('label:contains("Kyllä")')
      const firstSegmentInputBox = () =>
        formSections()
          .eq(2)
          .find(
            'label:contains("Suomessa suoritettu ylioppilastutkinto") + div'
          )
          .find('input.application__form-text-input')
      const thirdSegmentLabel = () =>
        formSections()
          .eq(2)
          .find('label:contains("Suomessa suoritettu korkeakoulututkinto")')
      const thirdSegmentUploadButton = () =>
        formSections()
          .eq(2)
          .find(
            'label:contains("Suomessa suoritettu korkeakoulututkinto") + div'
          )
          .find('label.application__form-upload-label')
          .eq(0)
      before(
        () => loadInFrame('/hakemus/haku/' + hakuWithPohjakoulutusMooduliKk),
        wait.until(() => formSections().length == 3),
        clickElement(firstSegmentLabel),
        clickElement(firstSegmentYesButton),
        setTextFieldValue(firstSegmentInputBox, '2015'),
        clickElement(thirdSegmentLabel)
      )
      it('hides the attachemnt requests that are under POHJAKOULUTUKSESI, and not the others', () => {
        expect(
          testFrame()
            .find(
              '#application-form-field-label-attachment-out-of-pohjakoulutus'
            )
            .text()
        ).to.equal('Random liite.')
        expect(thirdSegmentUploadButton().length).to.equal(0)
      })
    })
  })

  describe('hakemus by haku with multiple hakukohde', () => {
    describe('form loads', () => {
      before(
        () => loadInFrame('/hakemus/haku/' + multipleHakukohdeHakuOid),
        wait.until(() => formSections().length == 3)
      )
      it('with complete form and no hakukohde selected', () => {
        expect(formFields().length).to.equal(15)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('testing2')
        expect(selectedHakukohteet().length).to.equal(0)
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 11 tietoa')
        expect(hakukohdeSearchInput().is(':visible')).to.equal(false)
      })
    })

    describe('adding hakukohde', () => {
      before(
        clickElement(addHakukohdeLink),
        setTextFieldValue(hakukohdeSearchInput, 'haku'),
        wait.until(() => hakukohdeSearchHits().length === 3),
        clickElement(() => nthHakukohdeSearchResultButton(2)),
        wait.until(() => selectedHakukohteet().length === 1),
        wait.until(() => invalidFieldsStatus().text() === 'Tarkista 10 tietoa')
      )

      it('has correct data', () => {
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(selectedHakukohteet().length).to.equal(1)
      })
    })

    describe('clicking remove on selected hakukohde', () => {
      before(
        clickElement(() => selectedHakukohteet().eq(0).find('button')),
        wait.until(() => selectedHakukohteet().length === 0),
        wait.until(() => invalidFieldsStatus().text() === 'Tarkista 11 tietoa')
      )
      it('removes as expected', () => {
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 11 tietoa')
      })
    })

    describe('re-adding hakukohde and filling form', () => {
      before(
        clickElement(() => nthHakukohdeSearchResultButton(0)),
        clickElement(() => nthHakukohdeSearchResultButton(1)),
        clickElement(() =>
          testFrame().find('.application__hakukohde-selection-open-search')
        ),
        setNthFieldInputValue(1, 'Etunimi Tokanimi'),
        blurField(() => {
          return formFields().eq(1).find('input')
        }),
        setNthFieldInputValue(3, 'Sukunimi'),
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
        expect(hakukohdeSearchInput().is(':visible')).to.equal(false)
        expect(selectedHakukohteet().length).to.equal(2)
        expect(invalidFieldsStatus().length).to.equal(0)
      })
    })

    describe('changing hakukohde order back and forth', () => {
      before(
        clickElement(() => nthHakukohdePriorityDown(0)),
        wait.until(
          () =>
            selectedHakukohdeTexts() ===
            'Testihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi B | Tutkintonimike B | Tarkenne BTestihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi A | Tutkintonimike A | Tarkenne A'
        ),
        clickElement(() => nthHakukohdePriorityUp(1)),
        wait.until(
          () =>
            selectedHakukohdeTexts() ===
            'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi A | Tutkintonimike A | Tarkenne ATestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi B | Tutkintonimike B | Tarkenne B'
        ),
        //Make sure the disabled buttons do nothing
        clickElement(() => nthHakukohdePriorityUp(0)),
        wait.until(
          () =>
            selectedHakukohdeTexts() ===
            'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi A | Tutkintonimike A | Tarkenne ATestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi B | Tutkintonimike B | Tarkenne B'
        ),
        clickElement(() => nthHakukohdePriorityDown(1)),
        wait.until(
          () =>
            selectedHakukohdeTexts() ===
            'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi A | Tutkintonimike A | Tarkenne ATestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi B | Tutkintonimike B | Tarkenne B'
        )
      )

      it('has hakukohdes in correct order', () => {
        expect(selectedHakukohdeTexts()).to.equal(
          'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi A | Tutkintonimike A | Tarkenne ATestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi B | Tutkintonimike B | Tarkenne B'
        )
      })
    })

    describe('submitting form and viewing results', () => {
      before(
        clickElement(() => submitButton()),
        wait.until(
          () =>
            testFrame().find('.application__sent-placeholder-text').length == 1
        )
      )
      it('shows readonly application with selected data', () => {
        const hakukohdeValues = testFrame()
          .find('.application__hakukohde-selected-list')
          .text()
        expect(hakukohdeValues).to.equal(
          '1Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi A | Tutkintonimike A | Tarkenne A2Testihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi B | Tutkintonimike B | Tarkenne B'
        )

        const otherValues = _.map(
          testFrame().find('.application__text-field-paragraph'),
          (e) => $(e).text()
        )
        const expectedOtherValues = [
          'Etunimi Tokanimi',
          'Etunimi',
          'Sukunimi',
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
