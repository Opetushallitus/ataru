import * as lomakkeenMuokkaus from '../../lomakkeenMuokkaus'
import LomakkeenTunnisteet from '../../LomakkeenTunnisteet'

export default (lomakkeenTunnisteet: () => LomakkeenTunnisteet) => {
  describe('Arvosanat -osion poistaminen', () => {
    before(() => {
      lomakkeenMuokkaus.arvosanat.poistaArvosanat(
        lomakkeenTunnisteet().lomakkeenId
      )
    })

    it('Poistaa arvosanat -osion lomakkeelta', () => {
      lomakkeenMuokkaus.arvosanat.haeOsionNimi().should('not.exist')
    })

    after(() => {
      lomakkeenMuokkaus.komponentinLisays.lisaaArvosanat(
        lomakkeenTunnisteet().lomakkeenId
      )
    })
  })
}
