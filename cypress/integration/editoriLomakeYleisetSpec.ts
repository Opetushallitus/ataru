import { yleisetAsetukset } from '../lomakkeenMuokkaus'
import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'
import { unsafeFoldOption } from '../option'

describe('Lomake-editori Yleiset asetukset -osio', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    let lomakkeenAvain: string
    let lomakkeenId: number

    before(() => {
      cy.server()
      cy.route('/lomake-editori/api/tarjonta/haku**', [
        { oid: '1.2.246.562.29.00000000000000009710', yhteishaku: true },
      ])
      cy.route(
        '/lomake-editori/api/tarjonta/haku/1.2.246.562.29.00000000000000009710',
        { yhteishaku: true }
      )

      lomakkeenMuokkaus
        .lisaaLomake()
        .then((lomake) => {
          lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)
          lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
        })
        .then(() =>
          lomakkeenMuokkaus.asetaLomakkeenNimi('Testilomake', lomakkeenId)
        )
    })

    after(() => {
      cy.poistaLomake(lomakkeenAvain)
      cy.server({ enable: false })
    })

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
