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
      cy.get(
        `#application-list-row-${hakemusoid.replace(/\./g, '\\.')}`
      ).click()
    })

    testit()
  })
}
