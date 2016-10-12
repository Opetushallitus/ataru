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

  function formHeader() {
    return testFrame().find('.application__header')
  }

  function submitButton() {
    return testFrame().find('.application__send-application-button')
  }

  function formSections() {
    return testFrame().find('.application__form-content-area .application__wrapper-element')
  }

  function formFields() {
    return testFrame().find('.application__form-content-area .application__form-field')
  }

  function invalidFieldsStatus() {
    return testFrame().find('.application__invalid-field-status-title')
  }

  function invalidSections() {
    return testFrame().find('.application__banner-wrapper-sections-content')
  }

  function personInfoModule() {
    return formSections().eq(0)
  }

  function setNthFieldInputValue(n, value) {
    return setTextFieldValue(function() { return formFields().eq(n).find('input') }, value)
  }

  function setNthFieldValue(n, selector, value) {
    return function() {
      var $e = formFields().eq(n).find(selector)
      $e.val(value)
      triggerEvent($e, 'input') // needs to be input event because who knows why
    }
  }

  function setNthFieldOption(n, value) {
    return function() {
      formFields().eq(n).find('option[value="'+value+'"]').prop('selected', true)
      triggerEvent(formFields().eq(n).find('select'), 'change')
    }
  }

  function clickNthFieldRadio(n, value) {
    return function() {
      formFields().eq(n).find('label:contains('+value+')').click()
    }
  }

  describe('hakemus', function() {

    describe('form loads', function () {
      before(
        wait.until(function() { return formSections().length == 2 })
      )
      it('with complete form', function() {
        expect(formFields().length).to.equal(20)
        expect(submitButton().prop('disabled')).to.equal(true)
        expect(formHeader().text()).to.equal('Testilomake')
        expect(invalidFieldsStatus().text()).to.equal('12 pakollista tietoa puuttuu')
        expect(invalidSections().find('a').length).to.equal(2)
        expect(invalidSections().find('a.application__banner-wrapper-section-link-not-valid').length).to.equal(2)
      })
    })

    describe('person info module', function() {

      describe('structure', function() {
        it('has the correct fields', function() {
          var labels = _.map(personInfoModule().find('label'), function(e) { return $(e).text() })
          var expectedLabels = ["Etunimet *.","Kutsumanimi *.","Sukunimi *.","Kansalaisuus *.","Henkilötunnus *.","Sähköpostiosoite *.","Matkapuhelin *.","Katuosoite *.","Postinumero *.","Postitoimipaikka *.","Kotikunta *.","Äidinkieli *."]

          expect(personInfoModule().find('.application__wrapper-heading h2').text()).to.equal('Henkilötiedot')
          expect(labels).to.eql(expectedLabels)
        })
      })

      describe('filling out', function() {
        before(
          setNthFieldInputValue(0, 'Etunimi'),
          setNthFieldInputValue(1, 'Etunimi'),
          setNthFieldInputValue(2, 'Sukunimi'),
          setNthFieldInputValue(4, '020202A0202'),
          setNthFieldInputValue(5, 'test@example.com'),
          setNthFieldInputValue(6, '0123456789'),
          setNthFieldInputValue(7, 'Katutie 12 B'),
          setNthFieldInputValue(8, '40100'),
          setNthFieldInputValue(10, 'Jyväskylä'),
          wait.until(function() {
            return formFields().eq(9).find('input').val() !== ''
          })
        )
        it('works and validates correctly', function() {
          expect(formFields().eq(3).find('select').val()).to.equal('Suomi')
          expect(formFields().eq(9).find('input').val()).to.equal('JYVÄSKYLÄ')
          expect(formFields().eq(11).find('select').val()).to.equal('suomi')
          expect(invalidFieldsStatus().text()).to.equal('2 pakollista tietoa puuttuu')
          expect(invalidSections().find('a.application__banner-wrapper-section-link-not-valid').length).to.equal(1)
        })
      })

      // TODO: tests for nationality / SSN / birthdate / gender logic
    })

    describe('user-defined fields', function() {
      before(
        setNthFieldInputValue(12, 'Tekstikentän vastaus'),
        // TODO: repeating field 13
        setNthFieldValue(14, 'textarea', 'Pakollisen tekstialueen vastaus'),
        setNthFieldOption(15, 'Toinen vaihtoehto'),
        setNthFieldOption(16, 'Lisensiaatin tutkinto'),
        clickNthFieldRadio(17, 'Kolmas vaihtoehto', true),
        clickNthFieldRadio(18, 'Arkkitehti', true),
        setNthFieldValue(19, 'textarea', 'Toisen pakollisen tekstialueen vastaus')
      )
      it('works and validates correctly', function() {
        expect(invalidFieldsStatus().length).to.equal(0)
        expect(submitButton().prop('disabled')).to.equal(false)
      })
    })

    describe('submitting', function() {
      before(clickElement(function() { return submitButton() }))
      wait.until(function() {
        return $('.application-status-controls .application__sent-placeholder-text:contains("Hakemus lähtetty")').length == 1
      }, 2000)

      it('shows submitted form', function() {
        var displayedValues = _.map(testFrame().find('.application__form-field div'), function(e) { return $(e).text() })
        var expectedValues = ["Etunimi", "Etunimi", "Sukunimi", "Suomi", "020202A0202", "test@example.com", "0123456789", "Katutie 12 B", "40100", "JYVÄSKYLÄ", "Jyväskylä", "suomi", "Tekstikentän vastaus", "", "Pakollisen tekstialueen vastaus", "Toinen vaihtoehto", "Lisensiaatin tutkinto", "Kolmas vaihtoehto", "Arkkitehti", "Toisen pakollisen tekstialueen vastaus"]
        expect(displayedValues).to.eql(expectedValues)
      })
    })
  })
})()
