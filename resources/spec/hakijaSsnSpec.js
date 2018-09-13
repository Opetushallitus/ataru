(function() {
  afterEach(function() {
    expect(window.uiError || null).to.be.null
  });

  describe('SSN tests', function() {
    before(
      newForm('ssnTestForm'),
      wait.until(function () {
        return formSections().length === 1
      })
    );

    it('should open a new empty form', function() {
      expect(formFields().length).to.equal(13);
      expect(submitButton().prop('disabled')).to.equal(true);
      expect(formHeader().text()).to.equal('SSN_testilomake');
    });

    describe('SSN fields visibility', function() {
      it('should not have non-Finnish ssn fields visible initially', assertOnlyFinnishSsn);
      it('should have correct amount of required fields', assertInvalidFieldCount(10));

      describe('Select non-Finnish ssn nationality', function () {
        before(
          setNthFieldOption(3, '740'),
          wait.until(function() { return hasFormField('have-finnish-ssn') })
        )

        it('should show "have Finnish ssn?" field', assertHaveFinnishSsn)
        it('should have correct amount of required fields', assertInvalidFieldCount(10));
      });

      describe('Select "I don\'t have a Finnish ssn"', function () {
        before(
          setNthFieldOption(4, 'false'),
          wait.until(function() { return !hasFormField('ssn') })
        )

        it('should display all non-Finnish ssn fields', assertNonFinnishSsnFields)
        it('should have correct amount of required fields', assertInvalidFieldCount(12));
      });

      describe('Back to "I have a Finnish ssn"', function () {
        before(
          setNthFieldOption(4, 'true'),
          wait.until(function() { return hasFormField('ssn') })
        );

        it('should show "have Finnish ssn?" field', assertHaveFinnishSsn)
        it('should have correct amount of required fields', assertInvalidFieldCount(10));
      });

      describe('Back to Finnish nationality', function () {
        before(
          setNthFieldOption(3, '246'),
          wait.until(function() { return !hasFormField('have-finnish-ssn') })
        );

        it('should not have non-Finnish ssn fields visible', assertOnlyFinnishSsn);
        it('should have correct amount of required fields', assertInvalidFieldCount(10));
      });
    });

    describe('Filling out non-Finnish ssn info', function() {
      before(
        setNthFieldInputValue(0, 'Etunimi Tokanimi'),
        focusInput(2),
        wait.until(function() {
          return formFields().eq(1).find('input').val() !== ''
        }),
        setNthFieldInputValue(2, 'Sukunimi'),
        setNthFieldOption(3, '740'),
        wait.until(function () {
          return formFields().eq(4).find('label').text() === 'Onko sinulla suomalainen henkilötunnus?.'
        }),
        setNthFieldOption(4, 'false'),
        wait.until(function () {
          return formFields().eq(5).find('label').text() === 'Syntymäaika *.'
        }),
        setNthFieldInputValue(5, '1.1.1990'),
        setNthFieldOption(6, '1'),
        setNthFieldInputValue(7, 'Paramaribo, Suriname'),
        setNthFieldInputValue(8, '12345'),
        setNthFieldInputValue(9, 'id-12345'),
        setNthFieldInputValue(10, 'test@example.com'),
        setNthFieldInputValue(11, '0123456789'),
        setNthFieldInputValue(13, 'Katutie 12 B'),
        setNthFieldInputValue(14, '40100'),
        wait.until(function() {
          return formFields().eq(15).find('input').val() !== ''
        }),
        setNthFieldOption(16, "179"),
        wait.until(submitButtonEnabled)
      )

      it('works and validates correctly', function () {
        assertInvalidFieldCount(0)()
      })
    });

    describe('Submitting', function () {
      before(
        clickElement(function () {
          return submitButton()
        }),
        wait.until(function () {
          return testFrame().find('.application__sent-placeholder-text').length == 1
        })
      )

      it('has submitted the form', function() {
        var displayedValues = _.map(testFrame().find('.application__text-field-paragraph'), function(e) { return $(e).text() });
        var expectedValues = ["Etunimi Tokanimi",
          "Etunimi",
          "Sukunimi",
          "Suriname",
          "01.01.1990",
          "mies",
          "Paramaribo, Suriname",
          "12345",
          "id-12345",
          "test@example.com",
          "0123456789",
          "Suomi",
          "Katutie 12 B",
          "40100",
          "JYVÄSKYLÄ",
          "Jyväskylä",
          "suomi"]

        expect(displayedValues).to.eql(expectedValues);
      })
    })
  })
})()
