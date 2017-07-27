(function() {
  before(function () {
    var query = location.search.substring(1).split('&')
    var hakuOid = ''

    for (var i = 0; i < query.length; i++) {
      var param = query[i].split('=')
      if (param[0] === 'hakuOid') {
        hakuOid = param[1]
      }
    }

    if (hakuOid !== '') {
      console.log("haku oid", hakuOid)
      loadInFrame('/hakemus/haku/' + hakuOid)
    }
  })

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('hakemus', function() {

    describe('form loads', function () {
      before(
        wait.until(function() { return formSections().length == 3 }, 10000)
      )
      it('with complete form and the only hakukohde selected', function() {
        expect(formFields().length).to.equal(14)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('testing2')
        expect(selectedHakukohteet().length).to.equal(1)
        expect(selectedHakukohdeName(selectedHakukohteet()[0])).to.equal('Ajoneuvonosturinkuljettajan ammattitutkinto')
      })
    })
  })
})()
