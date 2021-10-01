import { yleisetAsetukset } from '../lomakkeenMuokkaus'
import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'

describe('Lomake-editori Yleiset asetukset -osio', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    lomakkeenLuonti(() => {
      it('Näyttää Yleiset asetukset', () => {
        yleisetAsetukset.haeOtsikko().should('have.text', 'Yleiset asetukset')
      })

      it('Näyttää demon alkamisajankohdan valinnan', () => {
        yleisetAsetukset.haeDemoAlkaa().should('be.visible')
      })

      it('Näyttää demon päättymisajankohdan valinnan', () => {
        yleisetAsetukset.haeDemoPaattyy().should('be.visible')
      })

      it('Demolinkkiä ei näytetä', () => {
        yleisetAsetukset.haeLinkkiDemoon().should('not.be.visible')
      })

      it('Demon aikavälin asettaminen toimii', () => {
        yleisetAsetukset.haeDemoAlkaa().type('2021-01-01')
        yleisetAsetukset.haeDemoPaattyy().type('2021-12-31')
      })
    })
  })
})
