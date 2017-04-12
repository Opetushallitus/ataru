(function () {
  before(function () {
    var query = location.search.substring(1).split('&')
    var secret = ''

    for (var i = 0; i < query.length; i++) {
      var param = query[i].split('=')
      if (param[0] == 'modify') {
        secret = param[1]
      }
    }

    console.log("secret", secret || 'UNDEFINED')
    loadInFrame('/hakemus?modify=' + secret)
  })

  describe('hakemus edit', function () {
    describe('form loads', function () {
      before(
        wait.until(function () {
          return formSections().length == 2
        })
      )
      it('with complete form', function () {
        expect(formFields().length).to.equal(14)
        expect(submitButton().prop('disabled')).to.equal(false)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(submitButton().prop('disabled')).to.equal(false)
        expect(invalidSections().find('a').length).to.equal(3)
        expect(invalidSections().find('a.application__banner-wrapper-section-link-not-valid').length).to.equal(0)
      })

      it('with correct existing answers', function () {
        var textInputValues = _.map(testFrame().find('.application__form-text-input'), function (e) {
          return $(e).val()
        })
        var expectedTestInputValues = [
          "Tekstikentän vastaus",
          "Toistuva vastaus 1",
          "Toistuva vastaus 3",
          "",
          "Pakollisen tekstialueen vastaus",
          "Jatkokysymyksen vastaus",
          "Toisen pakollisen tekstialueen vastaus",
          "",
          "Vasen vierekkäinen",
          "Oikea vierekkäinen"
        ]

        var dropdownInputValues = _.map(testFrame().find('select.application__form-select option:selected'), function (e) {
          return $(e).text()
        })
        var expectedDropdownInputValues = [
          "Kolmas vaihtoehto",
          "Lisensiaatin tutkinto",
          ""
        ]

        var checkboxInputValues = _.map(testFrame().find('input.application__form-checkbox:checked'), function (e) {
          return $(e).val()
        })
        var expectedCheckboxInputValues = ["Kolmas vaihtoehto", "139"]

        expect(textInputValues).to.eql(expectedTestInputValues)
        expect(dropdownInputValues).to.eql(expectedDropdownInputValues)
        expect(checkboxInputValues).to.eql(expectedCheckboxInputValues)
      })
    })

    describe('changing values to be invalid', function () {
      before(
        setNthFieldValue(9, 'textarea', ''),
        clickNthFieldRadio(12, 'Ensimmäinen vaihtoehto')
      )

      it('shows invalidity errors', function () {
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 2 tietoa')
        expect(invalidSections().find('a').length).to.equal(3)
        expect(invalidSections().find('a.application__banner-wrapper-section-link-not-valid').length).to.equal(1)
        expect(submitButton().prop('disabled')).to.equal(true)
      })
    })

    describe('change values and save', function () {
      before(
        setNthFieldValue(9, 'textarea', 'Muokattu vastaus'),
        clickNthFieldRadio(12, 'Toinen vaihtoehto'),
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
          "Jos haluat muuttaa henkilötietojasi, ota yhteyttä hakemaasi oppilaitokseen.",
          "Tekstikentän vastaus",
          "Toistuva vastaus 1Toistuva vastaus 3",
          "Pakollisen tekstialueen vastaus",
          "Kolmas vaihtoehto",
          "Jatkokysymyksen vastaus",
          "Lisensiaatin tutkinto",
          "Kolmas vaihtoehto",
          "Arkkitehti",
          "Muokattu vastaus",
          "",
          "",
          "Toinen vaihtoehto"
        ]

        var tabularValues = _.map(testFrame().find('.application__form-field table td'), function (e) {
          return $(e).text()
        })
        var expectedTabularValues = ["Vasen vierekkäinen", "Oikea vierekkäinen"]

        expect(displayedValues).to.eql(expectedValues)
        expect(tabularValues).to.eql(expectedTabularValues)
      })
    })
  })
})()
