(function () {
  before(function () {
    if (!testFormApplicationSecret) {
      console.log("Test application secret undefined (no application found). Did you run virkailija and hakija-form tests first?");
    } else {
      console.log("secret", testFormApplicationSecret);
      loadInFrame('/hakemus?modify=' + testFormApplicationSecret)
    }
  })

  describe('hakemus edit', function () {
    describe('form loads', function () {
      before(
        wait.until(function () {
          return formSections().length == 2
        })
      )
      it('with complete form', function () {
        expect(formFields().length).to.equal(33)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(submitButton().prop('disabled')).to.equal(true)
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
          "C2",
          "1,323"
        ]

        var dropdownInputValues = _.map(testFrame().find('select.application__form-select option:selected'), function (e) {
          return $(e).text()
        })
        var expectedDropdownInputValues = [
          "Suomi",
          "Suomi",
          "Jyväskylä",
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
        var expectedFollowupCheckboxInputValues = ['Jatkokysymys A', 'Jatkokysymys B']

        expect(textInputValues).to.eql(expectedTestInputValues)
        expect(dropdownInputValues).to.eql(expectedDropdownInputValues)
        expect(checkboxInputValues).to.eql(expectedCheckboxInputValues)
        expect(followupCheckboxInputValues).to.eql(expectedFollowupCheckboxInputValues)
      })
    })

    describe('changing values to be invalid', function () {
      before(
        setNthFieldInputValue(1, '420noscope'), //cannot be edited, no error!
        setNthFieldValue(23, 'textarea', ''),
        clickNthFieldRadio(26, 'Ensimmäinen vaihtoehto')
      )

      it('shows invalidity errors', function () {
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 2 tietoa')
        expect(submitButton().prop('disabled')).to.equal(true)
      })
    })

    describe('change values and save', function () {
      before(
        setNthFieldInputValue(1, 'Tokanimi'), // cannot be edited, should not be edited in virkailija edit spec.
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
          "Pudotusvalikon 1. kysymys",
          "1,323"
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
