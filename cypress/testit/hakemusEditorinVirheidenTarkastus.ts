export default (
  kuvaus: string,
  hakuteksti: string,
  hakemusoid: string,
  testit: () => void
) => {
  describe(kuvaus, () => {
    const rowSelector = `#application-list-row-${hakemusoid.replace(
      /\./g,
      '\\.'
    )}`

    it(`Hae testihenkilön ${hakuteksti} hakemus`, () => {
      cy.get(
        '.application__search-control-tab-selector-wrapper--search'
      ).click()
      // A search reloads the list through two chained debounces: :application/search-by-term
      // waits 500 ms for a real term (0 ms for a blank one), and the :application/reload-applications
      // it dispatches waits another 500 ms before the POST. So clearing and then typing produces
      // two list responses, roughly a second apart. Both have to be consumed here: one left in
      // flight lands during the next test and re-renders the list right as the row is clicked.
      cy.get('#ssn-search-field').then(($field) => {
        // .clear() on an already empty field fires no on-change, so it triggers no reload
        // to wait for. Only the later invocations of this helper have a term to clear.
        if ($field.val() !== '') {
          cy.get('#ssn-search-field').clear()
          cy.wait('@listApplications')
        }
      })
      cy.get('#ssn-search-field').type(hakuteksti, { delay: 50 })
      cy.wait('@listApplications')
    })

    it('Avaa hakemus tarkasteltavaksi', () => {
      cy.get(rowSelector).should('be.visible').click()
    })

    testit()
  })
}
