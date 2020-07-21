import LomakkeenTunnisteet from '../../../../../LomakkeenTunnisteet'
import * as lomakkeenMuokkaus from '../../../../../lomakkeenMuokkaus'
import { tekstikentta } from './tekstikentta'
import { lisakysymys } from './lisakysymys'

export default (
  lomakkeenTunnisteet: () => LomakkeenTunnisteet,
  testit: () => void
) => {
  describe('Tekstikentän lisäkysymyksen lisäys', () => {
    before(() => {
      tekstikentta
        .lisaaTekstikentta(lomakkeenTunnisteet().lomakkeenId)
        .then(() => tekstikentta.asetaKysymys('Kysymys'))
        .then(() => lisakysymys.valitseLisäkysymys())
      lomakkeenMuokkaus.teeJaodotaLomakkeenTallennusta(
        lomakkeenTunnisteet().lomakkeenId,
        () => lisakysymys.lisääLisäkysymys('Lisäkysymys')
      )
    })

    it('Näyttää lisäkysymyksen kysymystekstin', () => {
      lisakysymys
        .haeLisäkysymyksenKysymysteksti()
        .should('have.value', 'Lisäkysymys')
    })

    testit()
  })
}
