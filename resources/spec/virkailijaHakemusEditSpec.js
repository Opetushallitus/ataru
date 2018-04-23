(function () {
  before(function () {
    console.log("virkailijaSecret", virkailijaSecret || 'UNDEFINED')
    loadInFrame('/hakemus?virkailija-secret=' + virkailijaSecret)
  })

  var newPhoneNumber = Math.floor(Math.random() * 10000000).toString();

  describe('Virkailija hakemus edit', function () {
    describe('shows application with secret', function () {
      before(
        wait.until(function () {
          return formSections().length == 2
        })
      )
      it('with complete form', function () {
        expect(formFields().length).to.equal(31)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(submitButton().prop('disabled')).to.equal(true)
      })
    });

    describe('change values and save', function () {
      before(
        setNthFieldInputValue(6, newPhoneNumber),
        clickElement(function () {
          return submitButton()
        }),
        wait.until(function () {
          return testFrame().find('.application__sent-placeholder-text').length == 1
        })
      )

      it('shows submitted form', function () {
        var displayedValues = _.map(testFrame().find('.application__text-field-paragraph'), function(e) { return $(e).text() });
        console.log("values");
        console.log(displayedValues);
        var expectedValues = [
          "Etunimi Tokanimi",
          "Etunimi",
          "Sukunimi",
          "Suomi",
          "***********",
          "test@example.com",
          newPhoneNumber,
          "Suomi",
          "Katutie 12 B",
          "40100",
          "JYVÄSKYLÄ",
          "Jyväskylä",
          "suomi",
          "Tekstikentän vastaus",
          "Toistuva vastaus 1",
          "Toistuva vastaus 2",
          "Toistuva vastaus 3",
          "Pakollisen tekstialueen vastaus",
          "Kolmas vaihtoehto",
          "Jatkokysymyksen vastaus",
          "Lisensiaatin tutkinto",
          "Toinen vaihtoehto",
          "En",
          "Arkkitehti",
          "Muokattu vastaus",
          "Toinen vaihtoehto",
          "Pudotusvalikon 1. kysymys",
          "1,323"
        ];

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
        function() {return loadInFrame('/hakemus?virkailija-secret=' + virkailijaSecret)},
        wait.until(function () {
          return testFrame().find('.application__error-display').length == 1;
        })
      )

      it('shows error', function() {
        expect(testFrame().find('.application__error-display').text()).to.include('{:status 400, :status-text "Bad Request", :failure :error, :response {:error "Invalid virkailija secret"}}')
      })
    })
  });
})();
