import * as lomakkeenMuokkaus from '../../lomakkeenMuokkaus'
import * as arvosanat from '../../arvosanatOsio'
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
      arvosanat.haeOsionNimi().should('have.text', 'Arvosanat (peruskoulu)')
      arvosanat.haePoistaOsioNappi().should('be.enabled')
      arvosanat.haeLeikkaaOsioNappi().should('be.enabled')
    })

    testit()
  })
}
