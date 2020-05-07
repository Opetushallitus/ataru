import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'

export default (testit: () => void) => {
  describe('Virkailijan näkymään kirjautuminen', () => {
    before(() => {
      Cypress.Cookies.defaults({
        whitelist: ['ring-session'],
      })
      cy.kirjauduVirkailijanNakymaan()
    })

    it('Avaa lomakkeen muokkausnäkymän', () => {
      lomakkeenMuokkaus.haeLomakkeenLisaysNappi().should('be.enabled')
    })

    testit()
  })
}
