import * as hakijanNakyma from '../hakijanNakyma'

export default (testit: () => void) => {
  describe('Henkilötietomoduulin täyttäminen', () => {
    before(() => {
      hakijanNakyma.henkilotiedot.taytaTiedot()
    })

    it('Näyttää täytetyn henkilötietomoduulin', () => {
      hakijanNakyma.henkilotiedot
        .postitoimipaikka()
        .should('have.value', 'HELSINKI')
    })

    testit()
  })
}
