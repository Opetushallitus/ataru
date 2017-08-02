(function() {
  before(function () {
    var hakukohdeOid = getQueryParam('hakukohdeOid')

    if (hakukohdeOid !== '') {
      console.log("hakukohde oid", hakukohdeOid)
      loadInFrame('/hakemus/hakukohde/' + hakukohdeOid)
    }
  })

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('hakemus via hakukohde', function() {

    describe('form loads', function () {
      before(
        wait.until(function() { return formSections().length == 3 }, 10000)
      )
      it('with complete form and no hakukohde selected', function() {
        expect(formFields().length).to.equal(14)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 11 tietoa')
        expect(formHeader().text()).to.equal('testing2')
        expect(selectedHakukohteet().length).to.equal(0)
        expect(hakukohdeSearchHits().length).to.equal(0)
        expect(hakukohdeSearchInput().is(':visible')).to.equal(true)
      })
    })

    describe('inputting hakukohde search terms', function() {
      before(
        setTextFieldValue(hakukohdeSearchInput, 'haku'),
        wait.until(function() { return hakukohdeSearchHits().length === 3})
      )
      it('returns correct results', function() {
        var results = _.map(hakukohdeSearchHits(), function(e) {
          return $(e).find('.application__hakukohde-row-text-container').text()
        })
        expect(results).to.eql(["Testihakukohde 1Tarkenne A", "Testihakukohde 2Tarkenne B", "Testihakukohde 3Tarkenne C"])
      })
    })

    describe('narrowing down search results and adding hakukohde', function() {
      before(
        setTextFieldValue(hakukohdeSearchInput, 'hakukohde 1'),
        wait.until(function() { return hakukohdeSearchHits().length === 1}),
        clickElement(function() { return nthHakukohdeSearchResultButton(0) }),
        wait.until(function() { return selectedHakukohteet().length === 1})
      )
      it('adds hakukohde to selected list', function() {
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 10 tietoa')
        expect(selectedHakukohteet().first().text()).to.equal('Testihakukohde 1Tarkenne APoista')
      })
    })
  })
})()
