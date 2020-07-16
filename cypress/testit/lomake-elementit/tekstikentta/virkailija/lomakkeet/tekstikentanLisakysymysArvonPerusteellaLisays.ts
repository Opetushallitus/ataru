import LomakkeenTunnisteet from '../../../../../LomakkeenTunnisteet'
import { tekstikentta } from './tekstikentta'
import * as lomakkeenMuokkaus from '../../../../../lomakkeenMuokkaus'

export default (
  lomakkeenTunnisteet: () => LomakkeenTunnisteet,
  testit: () => void
) => {
  describe('Tekstikentän lisäkysymys arvon perusteella lisäys', () => {
    before(() => {
      tekstikentta
        .lisaaTekstikentta(lomakkeenTunnisteet().lomakkeenId)
        .then(() => tekstikentta.asetaKysymys('Kysymys'))
        .then(() => tekstikentta.valitseKenttäänVainNumeroita())
        .then(() => tekstikentta.valitseLisäkysymysArvonPerusteella())
        .then(() => tekstikentta.asetaLisäkysymysArvonPerusteellaEhto('>', 1))
        .then(() =>
          tekstikentta.avaaLisäkysymysArvonPerusteellaEhdonLisäkysymykset()
        )
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
