export default (kuvaus: string, testit: () => void) => {
  describe(`Virkailijan näkymään kirjautuminen ${kuvaus}`, () => {
    before(() => {
      Cypress.Cookies.defaults({
        whitelist: ['ring-session'],
      })
      cy.kirjauduVirkailijanNakymaan()
    })

    testit()
  })
}
