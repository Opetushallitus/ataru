import LomakkeenTunnisteet from '../../../../../LomakkeenTunnisteet'
import { tekstikentta } from './tekstikentta'
import { lomakeosionNayttaminenArvonPerusteella } from './lomakeosionNayttaminenArvonPerusteella'
import { lomakeosio } from './lomakeosio'

export default (
  lomakkeenTunnisteet: () => LomakkeenTunnisteet,
  testit: () => void
) => {
  describe('Lomakeosion näyttäminen tekstikentän arvon perusteella lisäys', () => {
    before(() => {
      tekstikentta
        .lisaaTekstikentta(lomakkeenTunnisteet().lomakkeenId)
        .then(() => tekstikentta.asetaKysymys('Kysymys'))
        .then(() => tekstikentta.valitseKenttäänVainNumeroita())
        .then(() =>
          lomakeosionNayttaminenArvonPerusteella.valitseLomakeosionNayttaminenArvonPerusteella()
        )
        .then(() =>
          lomakeosionNayttaminenArvonPerusteella.asetaLomakeosionNayttaminenArvonPerusteellaEhto(
            '<',
            0
          )
        )
        .then(() =>
          lomakeosio.lisaaLomakeosio(lomakkeenTunnisteet().lomakkeenId)
        )
    })

    it('Näyttää piilottettavan osion nimen', () => {
      lomakeosionNayttaminenArvonPerusteella
        .haePiilotettavanLomakeosionTeksti()
        .should('have.value', 'onr')
    })

    testit()
  })
}
