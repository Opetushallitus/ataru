//import * as hakijanNakyma from '../../hakijanNakyma'
import hakemuksenLahettaminen from '../hakemuksenLahettaminen'

export default () => {
  describe('Hakijan lomake, jolla on "Painikkeet, yksi valittavissa, koodisto"', () => {
    before(() => {
      cy.get('higher-completed-base-education')
        .get(
          `label:contains('Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)')`
        )
        .click({ multiple: true })
      // hakijanNakyma.klikkaa(
      //   'Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)'
      // )
    })

    hakemuksenLahettaminen(() => {
      describe('Painikkeet, yksi valittavissa, koodisto -toiminnon arvojen näyttäminen hakemuksen lähettämisen jälkeen', () => {
        it('Näyttää kysymyksen tekstin', () => {
          cy.contains('Minkä koulutuksen olet suorittanut?')
        })

        it('Näyttää valitun koodiarvon', () => {
          cy.contains(
            'Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)'
          )
        })
      })
    })
  })
}
