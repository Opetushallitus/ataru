export default (ticket: string, kuvaus: String, testit: () => void) => {
  describe(`Virkailijan näkymään kirjautuminen käyttäjänä ${ticket} ${kuvaus}`, () => {
    before(() => {
      Cypress.Cookies.defaults({
        whitelist: ['ring-session'],
      })
      cy.kirjauduVirkailijanNakymaan(ticket)
    })

    testit()
  })
}
