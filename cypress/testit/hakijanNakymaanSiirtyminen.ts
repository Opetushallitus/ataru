import * as hakijanNakyma from '../hakijanNakyma'
import LomakkeenTunnisteet from '../LomakkeenTunnisteet'

export default (
  lomakkeenTunnisteet: () => LomakkeenTunnisteet,
  testit: () => void
) => {
  describe('Hakijan näkymään siirtyminen', () => {
    before(() => {
      cy.avaaLomakeHakijanNakymassa(lomakkeenTunnisteet().lomakkeenAvain)
    })

    it('Lataa hakijan näkymän', () => {
      hakijanNakyma.haeLomakkeenNimi().should('have.text', 'Testilomake')
    })

    testit()
  })
}
