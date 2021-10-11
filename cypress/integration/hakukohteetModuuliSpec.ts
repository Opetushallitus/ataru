import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'
import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'

describe('Hakukohteet -moduuli', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    it('Navigoi lomakkeen muokkausnäkymään', () => {
      lomakkeenMuokkaus.haeLomakkeenLisaysNappi().should('be.enabled')
    })

    lomakkeenLuonti(() => {
      it('Näyttää hakukohdeet -moduulin', () => {
        lomakkeenMuokkaus.hakukohteet
          .haeOtsikko()
          .should('have.text', 'Hakukohteet')
      })

      it('Näyttää checkboxin hakukohteiden togglaamiseen', () => {
        lomakkeenMuokkaus.hakukohteet.haeCheckbox().should('be.visible')
      })

      it('Checkboxin toggleaminen toimii', () => {
        lomakkeenMuokkaus.hakukohteet.haeCheckbox().should('not.be.checked')
        lomakkeenMuokkaus.hakukohteet
          .haeCheckbox()
          .check()
          .then(() =>
            lomakkeenMuokkaus.hakukohteet.haeCheckbox().should('be.checked')
          )
      })
    })
  })
})
