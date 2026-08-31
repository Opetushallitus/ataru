export default (
  kuvaus: string,
  hakuteksti: string,
  hakemusoid: string,
  testit: () => void
) => {
  describe(kuvaus, () => {
    it(`Hae testihenkilön ${hakuteksti} hakemus`, () => {
      cy.get(
        '.application__search-control-tab-selector-wrapper--search'
      ).click()
      cy.get('#ssn-search-field').clear().type(hakuteksti, { delay: 50 })
      cy.wait('@listApplications')
    })

    it('Avaa hakemus tarkasteltavaksi', () => {
      const rowSelector = `#application-list-row-${hakemusoid.replace(
        /\./g,
        '\\.'
      )}`
      // The row can detach mid re-render right as we click it; retry with a fresh cy.get()
      // when that happens instead of guessing a settle wait.
      const clickRow = (attemptsLeft = 4) => {
        const onFail = (err: Error) => {
          if (attemptsLeft > 1 && /detached from the DOM/.test(err.message)) {
            clickRow(attemptsLeft - 1)
          } else {
            throw err
          }
        }
        Cypress.once('fail', onFail)
        cy.get(rowSelector).should('be.visible').click()
        cy.then(() => Cypress.off('fail', onFail))
      }
      clickRow()
    })

    testit()
  })
}
