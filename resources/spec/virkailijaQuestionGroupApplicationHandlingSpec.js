(function () {
  function answer(index, label) {
    return testFrame().find('.application__form-field:eq(' + index + ') .application__form-field-label:contains("' + label + '") + .application__form-field-value').text()
  }

  function adjacentAnswer(formFieldIndex, answerIndex, label) {
    return testFrame().find('.application__form-field:eq(' + formFieldIndex + ') .application__form-field-label:contains("' + label + '") + .application__readonly-adjacent td:eq(' + answerIndex +')').text()
  }

  function navigateToApplicationHandling() {
    var src = 'http://localhost:8350/lomake-editori/applications/' + config['form-key'];
    console.log(src)
    loadInFrame(src)
  }

  function linkToApplicationWithQuestionGroup() {
    return testFrame().find('.application-handling__list-row').not('.application-handling__list-header')
  }

  function linkToApplicationWithQuestionGroupIsRendered() {
    return elementExists(linkToApplicationWithQuestionGroup())
  }

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('Virkailija application handling for form with a question group', function () {
    before(
      navigateToApplicationHandling,
      wait.until(linkToApplicationWithQuestionGroupIsRendered)
    )
    it('automatically shows the only application belonging to the form', function() {
      expect(answer(0, 'Etunimet')).to.equal('Etunimi Tokanimi')
      expect(answer(1, 'Kutsumanimi')).to.equal('Etunimi')
      expect(answer(2, 'Sukunimi')).to.equal('Sukunimi')
      expect(answer(3, 'Kansalaisuus')).to.equal('Suomi')
      expect(answer(4, 'Henkilötunnus')).to.equal('020202A0202')
      expect(answer(5, 'Syntymäaika')).to.equal('02.02.2002')
      expect(answer(6, 'Sukupuoli')).to.equal('nainen')
      expect(answer(7, 'Syntymäpaikka ja -maa')).to.equal('')
      expect(answer(8, 'Passin numero')).to.equal('')
      expect(answer(9, 'Kansallinen ID-tunnus')).to.equal('')
      expect(answer(10, 'Sähköpostiosoite')).to.equal('test@example.com')
      expect(answer(11, 'Matkapuhelin')).to.equal('050123')
      expect(answer(12, 'Asuinmaa')).to.equal('Suomi')
      expect(answer(13, 'Katuosoite')).to.equal('Katutie 12 B')
      expect(answer(14, 'Postinumero')).to.equal('40100')
      expect(answer(15, 'Postitoimipaikka')).to.equal('JYVÄSKYLÄ')
      expect(answer(16, 'Kotikunta')).to.equal('Jyväskylä')
      expect(answer(17, 'Kaupunki')).to.equal('')
      expect(answer(18, 'Äidinkieli')).to.equal('suomi')
      expect(answer(19, 'Päätaso: pudotusvalikko')).to.equal('Päätaso: B')
      expect(answer(20, 'Kysymysryhmä: pudotusvalikko')).to.equal('Pudotusvalikko: A')
      expect(answer(21, 'Kysymysryhmä: painikkeet, yksi valittavissa')).to.equal('Painikkeet, yksi valittavissa: A')
      expect(answer(22, 'Kysymysryhmä: lista, monta valittavissa')).to.equal('Lista, monta valittavissa: ALista, monta valittavissa: B')
      expect(answer(23, 'Tekstikenttä, yksi vastaus')).to.equal('Tekstikenttä, yksi vastaus: A')
      expect(answer(24, 'Tekstikenttä, monta vastausta')).to.equal('Tekstikenttä, monta vastausta: ATekstikenttä, monta vastausta: B')
      expect(answer(25, 'Tekstialue')).to.equal('Tekstialue: AAAAA')
      expect(adjacentAnswer(26, 0, 'Vierekkäiset tekstikentät, yksi vastaus')).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus A')
      expect(adjacentAnswer(26, 1, 'Vierekkäiset tekstikentät, yksi vastaus')).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus B')
      expect(adjacentAnswer(27, 0, 'Vierekkäiset tekstikentät, monta vastausta')).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus A1')
      expect(adjacentAnswer(27, 1, 'Vierekkäiset tekstikentät, monta vastausta')).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus B1')
      expect(adjacentAnswer(27, 2, 'Vierekkäiset tekstikentät, monta vastausta')).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus A2')
      expect(adjacentAnswer(27, 3, 'Vierekkäiset tekstikentät, monta vastausta')).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus B2')
      expect(answer(28, 'Kysymysryhmä: pudotusvalikko')).to.equal('Pudotusvalikko: B')
      expect(answer(29, 'Kysymysryhmä: painikkeet, yksi valittavissa')).to.equal('Painikkeet, yksi valittavissa: B')
      expect(answer(30, 'Kysymysryhmä: lista, monta valittavissa')).to.equal('Lista, monta valittavissa: B')
      expect(answer(31, 'Tekstikenttä, yksi vastaus')).to.equal('Tekstikenttä, yksi vastaus: B')
      expect(answer(32, 'Tekstikenttä, monta vastausta')).to.equal('Tekstikenttä, monta vastausta: CTekstikenttä, monta vastausta: D')
      expect(answer(33, 'Tekstialue')).to.equal('Tekstialue: BBBBB')
      expect(adjacentAnswer(34, 0, 'Vierekkäiset tekstikentät, yksi vastaus')).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus C')
      expect(adjacentAnswer(34, 1, 'Vierekkäiset tekstikentät, yksi vastaus')).to.equal('Vierekkäiset tekstikentät, yksi vastaus: vastaus D')
      expect(adjacentAnswer(35, 0, 'Vierekkäiset tekstikentät, monta vastausta')).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus C1')
      expect(adjacentAnswer(35, 1, 'Vierekkäiset tekstikentät, monta vastausta')).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus D1')
      expect(adjacentAnswer(35, 2, 'Vierekkäiset tekstikentät, monta vastausta')).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus C2')
      expect(adjacentAnswer(35, 3, 'Vierekkäiset tekstikentät, monta vastausta')).to.equal('Vierekkäiset tekstikentät, monta vastausta: vastaus D2')
    })
  });
})();
