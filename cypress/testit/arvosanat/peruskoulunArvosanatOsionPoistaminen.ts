import * as lomakkeenMuokkaus from '../../lomakkeenMuokkaus'
import * as arvosanat from '../../arvosanatOsio'
import LomakkeenTunnisteet from '../../LomakkeenTunnisteet'

export default (lomakkeenTunnisteet: () => LomakkeenTunnisteet) => {
  describe('Arvosanat -osion poistaminen', () => {
    before(() => {
      arvosanat.poistaArvosanat(lomakkeenTunnisteet().lomakkeenId)
    })

    it('Poistaa arvosanat -osion lomakkeelta', () => {
      arvosanat.haeOsionNimi().should('not.exist')
    })

    after(() => {
      lomakkeenMuokkaus.komponentinLisays.lisaaArvosanat(
        lomakkeenTunnisteet().lomakkeenId
      )
    })
  })
}
