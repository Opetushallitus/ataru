(function() {
  before(function () {
    var query = location.search.substring(1).split('&')
    var formId = ''

    for (var i = 0; i < query.length; i++) {
      var param = query[i].split('=')
      if (param[0] == 'formId') {
        formId = param[1]
      }
    }

    console.log("form id", formId || 'UNDEFINED')
    loadInFrame('/hakemus/' + formId)
  })

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('hakemus', function() {

    describe('form loads', function () {
      before(
        wait.until(function() { return formSections().length == 2 })
      )
      it('with complete form', function() {
        expect(formFields().length).to.equal(25)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(invalidFieldsStatus().text()).to.equal('Tarkista 13 tietoa')
        expect(invalidSections().find('a').length).to.equal(3)
        expect(invalidSections().find('a.application__banner-wrapper-section-link-not-valid').length).to.equal(2)
      })
    })

    describe('person info module', function() {

      describe('structure', function() {
        it('has the correct fields', function() {
          var labels = _.map(personInfoModule().find('label'), function(e) { return $(e).text() })
          var expectedLabels = ["Etunimet *.","Kutsumanimi *.","Sukunimi *.","Kansalaisuus *.","Henkilötunnus *.","Sähköpostiosoite *.","Matkapuhelin *.","Asuinmaa *.","Katuosoite *.","Postinumero *.","Postitoimipaikka *.","Kotikunta *.","Äidinkieli *."]

          console.log("?", labels, expectedLabels)
          expect(personInfoModule().find('.application__wrapper-heading h2').text()).to.equal('Henkilötiedot')
          expect(labels).to.eql(expectedLabels)
        })
      })

      describe('filling out', function() {
        before(
          setNthFieldInputValue(0, 'Etunimi Tokanimi'),
          setNthFieldInputValue(2, 'Sukunimi'),
          setNthFieldInputValue(4, '020202A0202'),
          setNthFieldInputValue(5, 'test@example.com'),
          setNthFieldInputValue(6, '0123456789'),
          setNthFieldInputValue(8, 'Katutie 12 B'),
          setNthFieldInputValue(9, '40100'),
          setNthFieldInputValue(11, 'Jyväskylä'),
          wait.until(function() {
            return formFields().eq(10).find('input').val() !== ''
          })
        )
        it('works and validates correctly', function() {
          expect(formFields().eq(3).find('select').val()).to.equal('246')
          expect(formFields().eq(10).find('input').val()).to.equal('JYVÄSKYLÄ')
          expect(formFields().eq(12).find('select').val()).to.equal('FI')
          expect(invalidFieldsStatus().text()).to.equal('Tarkista 3 tietoa')
          expect(invalidSections().find('a.application__banner-wrapper-section-link-not-valid').length).to.equal(1)
        })
      })

      // TODO: tests for nationality / SSN / birthdate / gender logic
    })

    describe('user-defined fields', function() {
      before(
        setNthFieldInputValue(13, 'Tekstikentän vastaus'),
        setNthFieldInputValue(14, 'Toistuva vastaus 1'),
        setNthFieldSubInputValue(14, 1, 'Toistuva vastaus 2'),
        setNthFieldSubInputValue(14, 2, 'Toistuva vastaus 3'),
        clickElement(function() {
          return formFields().eq(14).find('a.application__form-repeatable-text--addremove').eq(0)
        }),
        setNthFieldValue(15, 'textarea', 'Pakollisen tekstialueen vastaus'),
        setNthFieldOption(16, 'Kolmas vaihtoehto'),
        setNthFieldInputValue(17, 'Jatkokysymyksen vastaus'),
        setNthFieldOption(18, '120'),
        clickNthFieldRadio(19, 'Toinen vaihtoehto', true),
        clickNthFieldRadio(20, 'En'),
        clickNthFieldRadio(21, 'Arkkitehti', true),
        setNthFieldValue(22, 'textarea', 'Toisen pakollisen tekstialueen vastaus'),
        clickNthFieldRadio(25, 'Ensimmäinen vaihtoehto'),
        setNthFieldSubInputValue(26, 0, 'Vasen vierekkäinen'),
        setNthFieldSubInputValue(26, 1, 'Oikea vierekkäinen')

      )
      it('works and validates correctly', function() {
        expect(invalidFieldsStatus().length).to.equal(0)
        expect(submitButton().prop('disabled')).to.equal(false)
      })


    })

    describe('submitting', function() {
      before(
        clickElement(function() { return submitButton() }),
          wait.until(function() {
            return testFrame().find('.application__sent-placeholder-text').length == 1
        })
      )

      it('shows submitted form', function() {
        var displayedValues = _.map(testFrame().find('.application__form-field div'), function(e) { return $(e).text() })
        var expectedValues = ["Etunimi Tokanimi",
                              "Etunimi",
                              "Sukunimi",
                              "Suomi",
                              "020202A0202",
                              "test@example.com",
                              "0123456789",
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
                              "Toisen pakollisen tekstialueen vastaus",
                              "",
                              "",
                              "Ensimmäinen vaihtoehto"]

        var tabularValues = _.map(testFrame().find('.application__form-field table td'), function(e) { return $(e).text() })
        var expectedTabularValues = ["Vasen vierekkäinen", "Oikea vierekkäinen"]

        expect(displayedValues).to.eql(expectedValues)
        expect(tabularValues).to.eql(expectedTabularValues)

      })
    })
  })
})()
