import * as hakijanNakyma from '../../hakijanNakyma'

export default () => {
  describe('Hakijan lomake, jolla on "Painikkeet, yksi valittavissa, koodisto"', () => {
    before(() => {
      hakijanNakyma
        .klikkaa(
          'Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)'
        )
        .then(hakijanNakyma.lahetaHakemus)
        .then(hakijanNakyma.painaOkPalautenakymassa)
        .then(hakijanNakyma.suljePalaute)
    })

    it('Näyttää lomakkeen nimen', () =>
      hakijanNakyma.haeLomakkeenNimi().should('have.text', 'Testilomake'))

    it('Näyttää kysymyksen tekstin', () =>
      cy.contains('Minkä koulutuksen olet suorittanut?'))

    it('Näyttää valitun koodiarvon', () =>
      cy.contains(
        'Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)'
      ))
  })
}
