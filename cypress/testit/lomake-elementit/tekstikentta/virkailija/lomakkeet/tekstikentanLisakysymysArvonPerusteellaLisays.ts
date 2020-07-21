import LomakkeenTunnisteet from '../../../../../LomakkeenTunnisteet'
import * as lomakkeenMuokkaus from '../../../../../lomakkeenMuokkaus'
import { tekstikentta } from './tekstikentta'
import { lisakysymysArvonPerusteella } from './lisakysymysArvonPerusteella'

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
        .then(() =>
          lisakysymysArvonPerusteella.valitseLisäkysymysArvonPerusteella()
        )
        .then(() =>
          lisakysymysArvonPerusteella.asetaLisäkysymysArvonPerusteellaEhto(
            '>',
            1
          )
        )
        .then(() =>
          lisakysymysArvonPerusteella.avaaLisäkysymysArvonPerusteellaEhdonLisäkysymykset()
        )
      lomakkeenMuokkaus.teeJaodotaLomakkeenTallennusta(
        lomakkeenTunnisteet().lomakkeenId,
        () => lisakysymysArvonPerusteella.lisääLisäkysymys('Lisäkysymys')
      )
    })

    it('Näyttää lisäkysymyksen kysymystekstin', () => {
      lisakysymysArvonPerusteella
        .haeLisäkysymyksenKysymysteksti()
        .should('have.value', 'Lisäkysymys')
    })

    testit()
  })
}
