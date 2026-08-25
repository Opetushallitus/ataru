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
      // A short settle wait before clicking: the row can still be mid re-render right after
      // the previous test's list load/selection state settles, which detaches it from the DOM
      // for an instant even with a stable React key on the row.
      cy.wait(500)
      cy.get(`#application-list-row-${hakemusoid.replace(/\./g, '\\.')}`)
        .should('be.visible')
        .click()
    })

    testit()
  })
}
