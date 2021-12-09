import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'
import * as hakijanNakyma from '../hakijanNakyma'
import { painikkeet } from '../testit/lomake-elementit/tekstikentta/virkailija/lomakkeet/painikkeet'
import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'

describe('Lomakeosion näkyvyys painikkeen perusteella', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      before(() =>
        lomakkeenMuokkaus.teeJaodotaLomakkeenTallennusta(
          lomakkeenTunnisteet().lomakkeenId,
          () =>
            painikkeet
              .lisaaPainikkeetYksiValittavissa(
                lomakkeenTunnisteet().lomakkeenId
              )
              .then(() => painikkeet.asetaKysymys('Kysymys'))
              .then(() =>
                painikkeet.valitseLomakeosionPiilottaminenArvonPerusteella()
              )
              .then(() => painikkeet.lisääVastausvaihtoehto('A'))
              .then(() => painikkeet.lisääVastausvaihtoehto('B'))
              .then(() =>
                painikkeet.asetaLomakeosionPiilottaminenArvonPerusteellaVertailuarvo(
                  'B'
                )
              )
              .then(() =>
                painikkeet.asetaLomakeosionPiilottaminenArvonPerusteellaOsio(
                  'Henkilötiedot'
                )
              )
        )
      )

      hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
        it('Näyttää valitun lomakeosion, kun lomakkeen avaa ensimmäistä kertaa', () => {
          hakijanNakyma.henkilotiedot.etunimi().should('exist')
        })

        it('Piilottaa valitun lomakeosion, kun ehdoksi asetettu painike on valittu', () => {
          hakijanNakyma.klikkaa('B')
          hakijanNakyma.henkilotiedot.etunimi().should('not.exist')
        })

        it('Näyttää valitun lomakeosion, kun eri painike on valittu', () => {
          hakijanNakyma.klikkaa('A')
          hakijanNakyma.henkilotiedot.etunimi().should('exist')
        })
      })
    })
  })
})
