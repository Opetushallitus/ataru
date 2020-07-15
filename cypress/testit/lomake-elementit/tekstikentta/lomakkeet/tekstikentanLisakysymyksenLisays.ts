import LomakkeenTunnisteet from '../../../../LomakkeenTunnisteet'
import * as lomakkeenMuokkaus from '../../../../lomakkeenMuokkaus'
import { tekstikentta } from './tekstikentta'

export default (
  lomakkeenTunnisteet: () => LomakkeenTunnisteet,
  testit: () => void
) => {
  describe('Tekstikentän lisäkysymyksen lisäys', () => {
    before(() => {
      lomakkeenMuokkaus.tekstikentta
        .lisaaTekstikentta(lomakkeenTunnisteet().lomakkeenId)
        .then(() => tekstikentta.asetaKysymys('Kysymys'))
        .then(() => tekstikentta.valitseLisäkysymys())
      lomakkeenMuokkaus.teeJaodotaLomakkeenTallennusta(
        lomakkeenTunnisteet().lomakkeenId,
        () => tekstikentta.lisääLisäkysymys('Lisäkysymys')
      )
    })

    it('Näyttää lisäkysymyksen kysymystekstin', () => {
      tekstikentta
        .haeLisäkysymyksenKysymysteksti()
        .should('have.value', 'Lisäkysymys')
    })

    testit()
  })
}
