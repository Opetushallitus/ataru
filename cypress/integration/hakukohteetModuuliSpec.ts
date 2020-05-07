import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'
import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'

describe('Hakukohteet -moduuli', () => {
  kirjautuminenVirkailijanNakymaan(() => {
    lomakkeenLuonti(() => {
      it('Näyttää hakukohdeet -moduulin', () => {
        lomakkeenMuokkaus.hakukohteet
          .haeOtsikko()
          .should('have.text', 'Hakukohteet')
      })
    })
  })
})
