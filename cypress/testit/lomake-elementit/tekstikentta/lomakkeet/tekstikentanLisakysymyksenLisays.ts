import LomakkeenTunnisteet from '../../../../LomakkeenTunnisteet'
import * as lomakkeenMuokkaus from '../../../../lomakkeenMuokkaus'
import { tekstikentta } from './tekstikentta'

export default (lomakkeenTunnisteet: () => LomakkeenTunnisteet) => {
  describe('Tekstikentän lisäkysymyksen lisäys', () => {
    before(() => {
      lomakkeenMuokkaus.tekstikentta
        .lisaaTekstikentta(lomakkeenTunnisteet().lomakkeenId)
        .then(() => tekstikentta.asetaKysymys('Kysymys'))
        .then(() => tekstikentta.valitseLisäkysymys())
        .then(() => tekstikentta.lisääLisäkysymys('Lisäkysymys'))
    })

    it('pitäisi olla lisäkysymyksen kysymys asetettuna', () => {
      tekstikentta.haeLisäkysymyksenTeksti().should('have.value', 'Lisäkysymys')
    })
  })
}
