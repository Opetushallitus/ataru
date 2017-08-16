(function () {
  var secret;
  before(function () {
    secret = getQueryParam('virkailija-secret')

    console.log("secret", secret || 'UNDEFINED')
    loadInFrame('/hakemus?virkailija-secret=' + secret)
  })

  describe('Virkailija hakemus edit', function () {
    describe('shows application with secret', function () {
      before(
        wait.until(function () {
          return formSections().length == 2
        })
      )
      it('with complete form', function () {
        expect(formFields().length).to.equal(30)
        expect(submitButton().prop('disabled')).to.equal(false)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(submitButton().prop('disabled')).to.equal(false)
        expect(invalidSections().find('a').length).to.equal(3)
        expect(invalidSections().find('a.application__banner-wrapper-section-link-not-valid').length).to.equal(0)
      })
    });

    describe('change values and save', function () {
      before(
        setNthFieldInputValue(6, '12345'),
        clickElement(function () {
          return submitButton()
        }),
        wait.until(function () {
          return testFrame().find('.application__sent-placeholder-text').length == 1
        })
      )

      it('shows submitted form', function () {
        var displayedValues = _.map(testFrame().find('.application__form-field div'), function (e) {
          return $(e).text()
        })
        var expectedValues = [
          "Etunimi Tokanimi",
          "Tokanimi",
          "Sukunimi",
          "Suomi",
          "***********",
          "test@example.com",
          "12345",
          "Suomi",
          "Katutie 12 B",
          "40100",
          "JYVÄSKYLÄ",
          "Jyväskylä",
          "suomi",
          "Tekstikentän vastaus",
          "Toistuva vastaus 1Toistuva vastaus 3",
          "Pakollisen tekstialueen vastaus",
          "Kolmas vaihtoehto",
          "Jatkokysymyksen vastaus",
          "Lisensiaatin tutkinto",
          "Toinen vaihtoehto",
          "En",
          "Arkkitehti",
          "Muokattu vastaus",
          "",
          "",
          "Toinen vaihtoehto",
          "Pudotusvalikon 1. kysymys"
        ]

        var tabularValues = _.map(testFrame().find('.application__form-field table td'), function (e) {
          return $(e).text()
        })
        var expectedTabularValues = ["A1", "B1", "C1", "A2", "", "C2", "Vasen vierekkäinen", "Oikea vierekkäinen", "A1", "B1", "C1", "A2", "", "C2"]

        expect(displayedValues).to.eql(expectedValues)
        expect(tabularValues).to.eql(expectedTabularValues)
      })
    })

    describe('edit with invalid key', function() {
      before(
        function() {return loadInFrame('/hakemus?virkailija-secret=' + secret)},
        wait.until(function () {
          return testFrame().find('.application__error-display').length == 1;
        })
      )

      it('shows error', function() {
        expect(testFrame().find('.application__error-display').text()).to.include('{:status 400, :status-text "Bad Request", :failure :error, :response {:error "Attempted to edit hakemus with invalid virkailija secret."}}')
      })
    })
  });
})();
