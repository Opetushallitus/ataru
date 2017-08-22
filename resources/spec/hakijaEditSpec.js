(function () {
  before(function () {
    var secret = getQueryParam('modify')

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
        expect(formFields().length).to.equal(32)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(invalidSections().find('a').length).to.equal(3)
        expect(invalidSections().find('a.application__banner-wrapper-section-link-not-valid').length).to.equal(0)
      })

      it('with correct existing answers', function () {
        var textInputValues = _.map(testFrame().find('.application__form-text-input'), function (e) {
          return $(e).val()
        })
        var expectedTestInputValues = [
          "Etunimi Tokanimi",
          "Etunimi",
          "Sukunimi",
          "***********",
          "test@example.com",
          "0123456789",
          "Katutie 12 B",
          "40100",
          "JYVÄSKYLÄ",
          "Jyväskylä",
          "Tekstikentän vastaus",
          "Toistuva vastaus 1",
          "Toistuva vastaus 2",
          "Toistuva vastaus 3",
          "",
          "Pakollisen tekstialueen vastaus",
          "Jatkokysymyksen vastaus",
          "A1",
          "B1",
          "C1",
          "A2",
          "",
          "C2",
          "Toisen pakollisen tekstialueen vastaus",
          "",
          "A1",
          "B1",
          "C1",
          "A2",
          "",
          "C2",
          "Vasen vierekkäinen",
          "Oikea vierekkäinen",
          "A1",
          "B1",
          "C1",
          "A2",
          "",
          "C2"
        ]

        var dropdownInputValues = _.map(testFrame().find('select.application__form-select option:selected'), function (e) {
          return $(e).text()
        })
        var expectedDropdownInputValues = [
          "Suomi",
          "Suomi",
          "suomi",
          "Kolmas vaihtoehto",
          "Lisensiaatin tutkinto",
          "",
          "Pudotusvalikon 1. kysymys"
        ]

        var checkboxInputValues = _.map(testFrame().find('input.application__form-checkbox:checked').not('.application__form-multi-choice-followups-container .application__form-checkbox'), function (e) {
          return $(e).val()
        })
        var expectedCheckboxInputValues = ["Toinen vaihtoehto", "139"]

        var followupCheckboxInputValues = _.map(testFrame().find('.application__form-multi-choice-followups-container input.application__form-checkbox:checked'), function (e) {
          return $(e).val()
        })
        var expectedFollowupCheckboxInputValues = ['Jatkokysymys A']

        expect(textInputValues).to.eql(expectedTestInputValues)
        expect(dropdownInputValues).to.eql(expectedDropdownInputValues)
        expect(checkboxInputValues).to.eql(expectedCheckboxInputValues)
        expect(followupCheckboxInputValues).to.eql(expectedFollowupCheckboxInputValues)
      })
    })

    describe('changing values to be invalid', function () {
      before(
        setNthFieldInputValue(1, '420noscope'),
        setNthFieldValue(23, 'textarea', ''),
        clickNthFieldRadio(26, 'Ensimmäinen vaihtoehto')
      )

      it('shows invalidity errors', function () {
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 3 tietoa')
        expect(invalidSections().find('a').length).to.equal(3)
        expect(invalidSections().find('a.application__banner-wrapper-section-link-not-valid').length).to.equal(2)
        expect(submitButton().prop('disabled')).to.equal(true)
      })
    })

    describe('change values and save', function () {
      before(
        setNthFieldInputValue(1, 'Tokanimi'),
        setNthFieldValue(23, 'textarea', 'Muokattu vastaus'),
        clickNthFieldRadio(26, 'Toinen vaihtoehto'),
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
          "0123456789",
          "Suomi",
          "Katutie 12 B",
          "40100",
          "JYVÄSKYLÄ",
          "Jyväskylä",
          "suomi",
          "Tekstikentän vastaus",
          "Toistuva vastaus 1Toistuva vastaus 2Toistuva vastaus 3",
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
  })
})()
