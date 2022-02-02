;(() => {
  const answer = (index) => {
    return testFrame()
      .find('.application__text-field-paragraph:eq(' + index + ')')
      .text()
  }

  const showResults = () => {
    return testFrame().find('[data-test-id=show-results]')
  }

  const adjacentAnswer = (formFieldIndex, answerIndex) => {
    return testFrame()
      .find(
        '.application__readonly-adjacent:eq(' +
          formFieldIndex +
          ') td:eq(' +
          answerIndex +
          ')'
      )
      .text()
  }

  const navigateToApplicationHandling = () => {
    const src =
      'http://localhost:8350/lomake-editori/applications/' + config['form-key']
    console.log(src)
    loadInFrame(src)
  }

  const personInfoHeader = () => {
    return testFrame().find(
      '.application__wrapper-heading h2:contains("Henkilötiedot")'
    )
  }
  const showResultsExists = () => {
    return elementExists(showResults())
  }
  const personInfoHeaderExists = () => {
    return elementExists(personInfoHeader())
  }

  afterEach(() => {
    expect(window.uiError || null).to.be.null
  })

  describe('Virkailija application handling for form with a question group', () => {
    before(
      navigateToApplicationHandling,
      clickElement(showResults),
      wait.until(personInfoHeaderExists)
    )
    it('automatically shows the only application belonging to the form', () => {
      expect(answer(0)).to.equal('Etunimi Tokanimi')
      expect(answer(1)).to.equal('Etunimi')
      expect(answer(2)).to.equal('Sukunimi')
      expect(answer(3)).to.equal('Suomi')
      expect(answer(4)).to.equal('020202A0202')
      expect(answer(5)).to.equal('02.02.2002')
      expect(answer(6)).to.equal('nainen')
      expect(answer(7)).to.equal('test@example.com')
      expect(answer(8)).to.equal('050123')
      expect(answer(9)).to.equal('Suomi')
      expect(answer(10)).to.equal('Katutie 12 B')
      expect(answer(11)).to.equal('40100')
      expect(answer(12)).to.equal('JYVÄSKYLÄ')
      expect(answer(13)).to.equal('Jyväskylä')
      expect(answer(14)).to.equal('suomi')
      expect(answer(15)).to.equal('Päätaso: B')
      expect(answer(16)).to.equal('Pudotusvalikko: A')
      expect(answer(17)).to.equal('Painikkeet, yksi valittavissa: A')
      expect(answer(18)).to.equal('Lista, monta valittavissa: A')
      expect(answer(19)).to.equal('Lista, monta valittavissa: B')
      expect(answer(20)).to.equal('Tekstikenttä, yksi vastaus: A')
      expect(answer(21)).to.equal('Tekstikenttä, monta vastausta: A')
      expect(answer(22)).to.equal('Tekstikenttä, monta vastausta: B')
      expect(answer(23)).to.equal('Tekstialue: AAAAA')
      expect(adjacentAnswer(0, 0)).to.equal(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus A'
      )
      expect(adjacentAnswer(0, 1)).to.equal(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus B'
      )
      expect(adjacentAnswer(1, 0)).to.equal(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus A1'
      )
      expect(adjacentAnswer(1, 1)).to.equal(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus B1'
      )
      expect(adjacentAnswer(1, 2)).to.equal(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus A2'
      )
      expect(adjacentAnswer(1, 3)).to.equal(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus B2'
      )
      expect(answer(24)).to.equal('Pudotusvalikko: B')
      expect(answer(25)).to.equal('Painikkeet, yksi valittavissa: B')
      expect(answer(26)).to.equal('Lista, monta valittavissa: B')
      expect(answer(27)).to.equal('Tekstikenttä, yksi vastaus: B')
      expect(answer(28)).to.equal('Tekstikenttä, monta vastausta: C')
      expect(answer(29)).to.equal('Tekstikenttä, monta vastausta: D')
      expect(answer(30)).to.equal('Tekstialue: BBBBB')
      expect(adjacentAnswer(2, 0)).to.equal(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus C'
      )
      expect(adjacentAnswer(2, 1)).to.equal(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus D'
      )
      expect(adjacentAnswer(3, 0)).to.equal(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus C1'
      )
      expect(adjacentAnswer(3, 1)).to.equal(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus D1'
      )
      expect(adjacentAnswer(3, 2)).to.equal(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus C2'
      )
      expect(adjacentAnswer(3, 3)).to.equal(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus D2'
      )
    })
  })

  const reviewStateButton = () => {
    return testFrame().find('.application-handling__review-state-row--selected')
  }

  const informationRequestStateButton = () => {
    return testFrame().find(
      '.application-handling__review-state-row:contains("Täydennyspyyntö")'
    )
  }

  const informationRequestStateButtonExists = () => {
    return elementExists(informationRequestStateButton())
  }

  const submitInformationRequestButton = () => {
    return testFrame().find(
      '.application-handling__send-information-request-button'
    )
  }

  const submitInformationRequestButtonIsDisabled = () => {
    return submitInformationRequestButton().prop('disabled') === true
  }

  const submitInformationRequestButtonIsEnabled = () => {
    return submitInformationRequestButton().prop('disabled') === false
  }

  const informationRequestSubject = () => {
    return testFrame().find(
      '.application-handling__information-request-text-input'
    )
  }

  const submitInformationRequestMessage = () => {
    return testFrame().find(
      '.application-handling__information-request-message-area'
    )
  }

  const informationRequestConfirmationIsDisplayed = () => {
    return elementExists(
      testFrame().find(
        '.application-handling__information-request-submitted-text:contains("Täydennyspyyntö lähetetty")'
      )
    )
  }

  const showInformationRequestFormLinkIsDisplayed = () => {
    return elementExists(
      testFrame().find(
        '.application-handling__information-request-show-container-link a:contains("Lähetä täydennyspyyntö hakijalle")'
      )
    )
  }

  describe('Sending information requests to the applicant', () => {
    before(
      clickElement(reviewStateButton),
      wait.until(informationRequestStateButtonExists),
      clickElement(informationRequestStateButton),
      wait.until(() =>
        elementExists(
          testFrame().find(
            '.application-handling__information-request-container'
          )
        )
      )
    )
    it('shows the information request form to the user', (done) => {
      expect(submitInformationRequestButtonIsDisabled()).to.equal(true)
      setTextFieldValue(informationRequestSubject, 'Täydennyspyyntö: otsikko')()
        .then(wait.until(submitInformationRequestButtonIsDisabled))
        .then(
          setTextFieldValue(
            submitInformationRequestMessage,
            'Täydennyspyyntö: viesti'
          )
        )
        .then(wait.until(submitInformationRequestButtonIsEnabled))
        .then(setTextFieldValue(submitInformationRequestMessage, ''))
        .then(wait.until(submitInformationRequestButtonIsDisabled))
        .then(
          setTextFieldValue(
            submitInformationRequestMessage,
            'Täydennyspyyntö: viesti'
          )
        )
        .then(wait.until(submitInformationRequestButtonIsEnabled))
        .then(clickElement(submitInformationRequestButton))
        .then(wait.until(informationRequestConfirmationIsDisplayed))
        .then(wait.until(showInformationRequestFormLinkIsDisplayed))
        .then(done)
    })
  })
})()
