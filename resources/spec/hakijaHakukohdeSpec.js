(function() {
  before(function () {
    loadInFrame('/hakemus/hakukohde/1.2.246.562.20.49028196523')
  })

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('hakemus via hakukohde', function() {

    describe('form loads', function () {
      before(
        wait.until(function() { return formSections().length == 3 }, 10000)
      )
      it('with complete form and default hakukohde selected', function() {
        expect(formFields().length).to.equal(14)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 10 tietoa')
        expect(formHeader().text()).to.equal('testing2')
        expect(selectedHakukohteet().length).to.equal(1)
        expect(hakukohdeSearchHits().is(':visible')).to.equal(false)
        expect(hakukohdeSearchInput().is(':visible')).to.equal(false)
        expect(selectedHakukohteet().first().text()).to.equal('Testihakukohde 1 –\xa0Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne APoista')
      })
    })

    describe('inputting hakukohde search terms', function() {
      before(
        clickElement(addHakukohdeLink),
        setTextFieldValue(hakukohdeSearchInput, 'haku'),
        wait.until(function() { return hakukohdeSearchHits().length === 3})
      )
      it('returns correct results', function() {
        var results = _.map(hakukohdeSearchHits(), function(e) {
          return $(e).find('.application__hakukohde-row-text-container').text()
        })
        expect(results).to.eql([
          "Testihakukohde 1 –\xa0Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne A",
          "Testihakukohde 2 –\xa0Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne B",
          "Testihakukohde 3 –\xa0Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne C"
        ])
      })
    })

    describe('narrowing down search results and adding hakukohde', function() {
      before(
        setTextFieldValue(hakukohdeSearchInput, 'hakukohde 2'),
        wait.until(function() { return hakukohdeSearchHits().length === 1}),
        clickElement(function() { return nthHakukohdeSearchResultButton(0) }),
        wait.until(function() { return selectedHakukohteet().length === 2})
      )
      it('adds hakukohde to selected list', function() {
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 10 tietoa')
        expect(selectedHakukohteet().eq(0).text()).to.equal('Testihakukohde 1 –\xa0Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne APoista')
        expect(selectedHakukohteet().eq(1).text()).to.equal('Testihakukohde 2 –\xa0Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne BPoista')
      })
    })
  })
})()
