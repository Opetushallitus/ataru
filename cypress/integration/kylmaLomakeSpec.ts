import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import {
  yleisetAsetukset,
  teeJaodotaLomakkeenTallennusta,
} from '../lomakkeenMuokkaus'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'

describe('Kylmän lomakkeen sulkeminen', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      it('Asettaa lomakkeen suljetuksi', () => {
        yleisetAsetukset.haeSuljeLomake().should('not.be.disabled')
        yleisetAsetukset
          .haeSuljeLomake()
          .click()
          .then(() =>
            teeJaodotaLomakkeenTallennusta(
              lomakkeenTunnisteet().lomakkeenId,
              () => yleisetAsetukset.haeSuljeLomake().should('be.checked')
            )
          )
      })
      hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
        it('Näyttää lomake on suljettu tekstin', () => {
          cy.get('.application__sub-header-container').contains(
            'Hakulomake ei ole enää käytössä'
          )
        })
      })
    })
  })
})
