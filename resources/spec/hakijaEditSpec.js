(function() {
  before(function () {
    var query = location.search.substring(1).split('&')
    var secret = ''

    for (var i = 0; i < query.length; i++) {
      var param = query[i].split('=')
      if (param[0] == 'modify') {
        secret = param[1]
      }
    }

    console.log("secret", secret || 'UNDEFINED')
    loadInFrame('/hakemus?modify=' + secret)
  })

  describe('hakemus edit', function() {
    describe('form loads', function() {
      before(
        wait.until(function() { return formSections().length == 2 })
      )
      it('with complete form', function() {
        expect(formFields().length).to.equal(24)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(invalidFieldsStatus().text()).to.equal('13 pakollista tietoa puuttuu')
        expect(invalidSections().find('a').length).to.equal(3)
        expect(invalidSections().find('a.application__banner-wrapper-section-link-not-valid').length).to.equal(2)
      })
    })
  })
})()