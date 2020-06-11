import * as hakijanNakyma from '../hakijanNakyma'

export default (testit: () => void) => {
  describe('Hakemuksen lähettäminen', () => {
    before(() => {
      hakijanNakyma
        .lahetaHakemus()
        .then(hakijanNakyma.painaOkPalautenakymassa)
        .then(hakijanNakyma.suljePalaute)
    })

    it('Näyttää lomakkeen nimen', () => {
      hakijanNakyma.haeLomakkeenNimi().should('have.text', 'Testilomake')
    })

    testit()
  })
}
