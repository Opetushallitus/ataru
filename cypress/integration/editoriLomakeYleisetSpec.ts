import { yleisetAsetukset } from '../lomakkeenMuokkaus'
import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'

describe('Lomake-editori Yleiset asetukset -osio', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    lomakkeenLuonti(() => {
      it('Näyttää Yleiset asetukset', () => {
        yleisetAsetukset.haeOtsikko().should('have.text', 'Yleiset asetukset')
      })

      it('Näyttää checkboxin demon togglaamiseen', () => {
        yleisetAsetukset.haeDemoCheckbox().should('be.visible')
      })

      it('Demolinkkiä ei näytetä', () => {
        yleisetAsetukset.haeLinkkiDemoon().should('not.be.visible')
      })

      it('Demo checkboxin asettaminen päälle toimii', () => {
        yleisetAsetukset.haeDemoCheckbox().should('not.be.checked')
        yleisetAsetukset
          .haeDemoCheckbox()
          .check()
          .then(() => {
            yleisetAsetukset.haeDemoCheckbox().should('be.checked')
          })
      })
    })
  })
})
