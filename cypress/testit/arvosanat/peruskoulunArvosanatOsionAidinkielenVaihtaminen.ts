import * as hakijanNakyma from '../../hakijanNakyma'
import * as dropdown from '../../dropdown'

export default () => {
  describe('Peruskoulun arvosanat -osion äidinkielen vaihtaminen ruotsiksi', () => {
    before(() => {
      dropdown.asetaHakijanNakymanPudotusvalikonArvo('language-input', 'SV')
    })

    after(() => {
      dropdown.asetaHakijanNakymanPudotusvalikonArvo('language-input', 'FI')
    })

    it('Näyttää peruskoulun arvosanat -osiossa ruotsin kielen edellyttämät oppiaineet', () => {
      hakijanNakyma.arvosanat
        .haeOppiaineenArvosanaRivi({ oppiaine: 'A2' })
        .should('exist')
        .should('be.visible')
        .then(() =>
          hakijanNakyma.arvosanat.haeOppiaineenArvosanaRivi({ oppiaine: 'B1' })
        )
        .should('not.exist')
    })
  })
}
