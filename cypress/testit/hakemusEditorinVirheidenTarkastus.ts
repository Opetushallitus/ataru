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
      cy.get('#ssn-search-field').clear().type(hakuteksti, { delay: 50 })
      cy.wait('@listApplications')
      // Hakukenttä on debounced (500ms) ja välilehden vaihto laukaisee oman hakemuslistauksen,
      // joten @listApplications voi ratketa väärään pyyntöön. Odotetaan että rivi on oikeasti
      // listalla, ettei myöhäinen vastaus renderöi listaa uusiksi kesken seuraavan testin.
      cy.get(rowSelector).should('exist')
    })

    it('Avaa hakemus tarkasteltavaksi', () => {
      // Haetaan selektori uudelleen joka komennolle, jottei irronnutta (detached) elementtiä
      // jää käsiin listan uudelleenrenderöinnin aikana.
      cy.get(rowSelector).should('be.visible')
      cy.get(rowSelector).click()
      cy.get(rowSelector).should(
        'have.class',
        'application-handling__list-row--selected'
      )
    })

    testit()
  })
}
