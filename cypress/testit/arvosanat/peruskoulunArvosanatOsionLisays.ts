import * as lomakkeenMuokkaus from '../../lomakkeenMuokkaus'
import LomakkeenTunnisteet from '../../LomakkeenTunnisteet'

export default (
  lomakkeenTunnisteet: () => LomakkeenTunnisteet,
  testit: () => void
) => {
  describe('Peruskoulun arvosanat -osion lisäys', () => {
    before(() => {
      lomakkeenMuokkaus.komponentinLisays
        .lisaaArvosanat(lomakkeenTunnisteet().lomakkeenId)
        .then(({ result: arvosanatLinkki }) =>
          cy.wrap(arvosanatLinkki.text()).as('component-toolbar-arvosanat-text')
        )
    })

    it('Näyttää arvosanat -osion', () => {
      cy.get('@component-toolbar-arvosanat-text').then((arvosanatTeksti) =>
        expect(arvosanatTeksti).to.equal('Arvosanat (peruskoulu)')
      )
      lomakkeenMuokkaus.komponentinLisays
        .haeLisaaArvosanatLinkki()
        .should('have.text', 'Arvosanat (peruskoulu)')
      lomakkeenMuokkaus.arvosanat
        .haeOsionNimi()
        .should('have.text', 'Arvosanat (peruskoulu)')
      lomakkeenMuokkaus.arvosanat.haePoistaOsioNappi().should('be.enabled')
      lomakkeenMuokkaus.arvosanat.haeLeikkaaOsioNappi().should('be.enabled')
    })

    testit()
  })
}
