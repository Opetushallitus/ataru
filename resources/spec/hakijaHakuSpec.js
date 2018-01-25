(function() {
  var singleHakukohdeHakuOid = "1.2.246.562.29.65950024185"
  var multipleHakukohdeHakuOid = "1.2.246.562.29.65950024186"

  before(function () {
    loadInFrame('/hakemus/haku/' + singleHakukohdeHakuOid)
  })

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('hakemus by haku with single hakukohde', function() {
    describe('form loads', function () {
      before(
        wait.until(function() { return formSections().length == 3 }, 10000)
      )
      it('with complete form and the only hakukohde selected', function() {
        expect(formFields().length).to.equal(14)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('testing2')
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 10 tietoa')
        expect(selectedHakukohteet().length).to.equal(1)
        expect(hakukohdeSearchInput().is(':visible')).to.equal(false)
        // \xa0 == &nbsp;
        expect(selectedHakukohdeName(selectedHakukohteet()[0])).to.equal('Ajoneuvonosturinkuljettajan ammattitutkinto –\xa0Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie')
      })
    })
  })

  describe('hakemus by haku with multiple hakukohde', function() {
    describe('form loads', function () {
      before(
        function() { return loadInFrame('/hakemus/haku/' + multipleHakukohdeHakuOid)},
        wait.until(function () {
          return formSections().length == 3
        }, 10000)
      )
      it('with complete form and no hakukohde selected', function () {
        expect(formFields().length).to.equal(14)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('testing2')
        expect(selectedHakukohteet().length).to.equal(0)
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 11 tietoa')
        expect(hakukohdeSearchInput().is(':visible')).to.equal(false)
      })
    })

    describe('adding hakukohde', function () {
      before(
        clickElement(addHakukohdeLink),
        setTextFieldValue(hakukohdeSearchInput, 'haku'),
        wait.until(function () {
          return hakukohdeSearchHits().length === 3
        }),
        clickElement(function () {
          return nthHakukohdeSearchResultButton(2)
        }),
        wait.until(function () {
          return selectedHakukohteet().length === 1
        })
      )

      it('has correct data', function () {
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(selectedHakukohteet().length).to.equal(1)
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 10 tietoa')
      })
    })

    describe('clicking remove on selected hakukohde', function () {
      before(
        clickElement(function() { return selectedHakukohteet().eq(0).find('a') }),
        wait.until(function () {
          return selectedHakukohteet().length === 0
        })
      )
      it('removes as expected', function () {
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 11 tietoa')
      })
    })

    describe('re-adding hakukohde and filling form', function () {
      before(
        clickElement(function () {
          return nthHakukohdeSearchResultButton(0)
        }),
        clickElement(function () {
          return nthHakukohdeSearchResultButton(1)
        }),
        clickElement(function () {
          return testFrame().find('.application__hakukohde-selection-open-search')
        }),
        setNthFieldInputValue(0, 'Etunimi Tokanimi'),
        setNthFieldInputValue(2, 'Sukunimi'),
        setNthFieldInputValue(4, '020202A0202'),
        setNthFieldInputValue(5, 'test@example.com'),
        setNthFieldInputValue(6, '0123456789'),
        setNthFieldInputValue(8, 'Katutie 12 B'),
        setNthFieldInputValue(9, '00100'),
        setNthFieldInputValue(11, 'Helsinki'),
        setNthFieldInputValue(13, '55cm'),
        wait.until(function () {
          return formFields().eq(10).find('input').val() !== ''
        })
      )
      it('validates and shows form correctly', function () {
        expect(hakukohdeSearchInput().is(':visible')).to.equal(false)
        expect(submitButton().prop('disabled')).to.equal(false)
        expect(selectedHakukohteet().length).to.equal(2)
        expect(invalidFieldsStatus().length).to.equal(0)
      })
    })

    describe('changing hakukohde order back and forth', function() {
      before(
        clickElement(function() {
          return nthHakukohdePriorityDown(0)
        }),
        wait.until(function() {
          return selectedHakukohdeTexts() === 'Testihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne BTestihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne A';
        }),
        clickElement(function() {
          return nthHakukohdePriorityUp(1)
        }),
        wait.until(function() {
          return selectedHakukohdeTexts() === 'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne ATestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne B'
        }),
        //Make sure the disabled buttons do nothing
        clickElement(function() {
          return nthHakukohdePriorityUp(0)
        }),
        wait.until(function() {
          return selectedHakukohdeTexts() === 'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne ATestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne B'
        }),
        clickElement(function() {
          return nthHakukohdePriorityDown(1)
        }),
        wait.until(function() {
          return selectedHakukohdeTexts() === 'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne ATestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne B'
        })
      );

      it('has hakukohdes in correct order', function() {
          expect(selectedHakukohdeTexts()).to.equal('Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne ATestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne B')
      });
    });

    describe('submitting form and viewing results', function () {
      before(
        clickElement(function () {
          return submitButton()
        }),
        wait.until(function () {
          return testFrame().find('.application__sent-placeholder-text').length == 1
        })
      )
      it('shows readonly application with selected data', function() {
        var hakukohdeValues = testFrame().find('.application__hakukohde-selected-list').text()
        expect(hakukohdeValues).to.equal('1Testihakukohde 1 –\xa0Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne A2Testihakukohde 2 –\xa0Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne B')

        var otherValues = _.map(testFrame().find('.application__form-field div'), function(e) { return $(e).text() })
        var expectedOtherValues = ["Etunimi Tokanimi",
          "Etunimi",
          "Sukunimi",
          "Suomi",
          "020202A0202",
          "test@example.com",
          "0123456789",
          "Suomi",
          "Katutie 12 B",
          "00100",
          "HELSINKI",
          "Helsinki",
          "suomi",
          "55cm"]
        expect(otherValues).to.eql(expectedOtherValues)
      })
    })
  });
})()
