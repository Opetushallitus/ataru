import * as hakijanNakyma from '../hakijanNakyma'

export default (testit: () => void) => {
  describe('Henkilötietomoduulin täyttäminen', () => {
    before(() => {
      hakijanNakyma.henkilotiedot.taytaTiedot()
    })

    it('Näyttää täytetyn henkilötietomoduulin', () => {
      hakijanNakyma.henkilotiedot
        .haePostitoimipaikkaKentta()
        .should('have.value', 'HELSINKI')
    })

    testit()
  })
}
