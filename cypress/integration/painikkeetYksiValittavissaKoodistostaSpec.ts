import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import painikkeetYksiValittavissaKoodistostaElementinLisays from '../testit/lomake-elementit/painikkeetYksiValittavissaKoodistostaElementinLisays'
import painikkeetYksiValittavissaKoodistostaHakijalle from '../testit/lomake-elementit/painikkeetYksiValittavissaKoodistostaHakijalle'
import henkilotietoModuulinTayttaminen from '../testit/henkilotietoModuulinTayttaminen'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'
import { getSensitiveAnswer } from '../checkbox'
import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'
import { unsafeFoldOption } from '../option'

describe('Painikkeet, yksi valittavissa, koodisto -lomake-elementti', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    let lomakkeenTunnisteet: { lomakkeenAvain: string; lomakkeenId: number }

    before(() => {
      cy.server()
      cy.route('/lomake-editori/api/tarjonta/haku?form-key=*', [
        {
          oid: '1.2.246.562.29.00000000000000009710',
          yhteishaku: true,
          'kohdejoukko-uri': 'haunkohdejoukko_11#1',
        },
      ])
      cy.route(
        '/lomake-editori/api/tarjonta/haku/1.2.246.562.29.00000000000000009710',
        { yhteishaku: true, 'kohdejoukko-uri': 'haunkohdejoukko_11#1' }
      )

      lomakkeenMuokkaus
        .lisaaLomake()
        .then((lomake) => {
          lomakkeenTunnisteet = {
            lomakkeenAvain: unsafeFoldOption(lomake.lomakkeenAvain),
            lomakkeenId: unsafeFoldOption(lomake.lomakkeenId),
          }
        })
        .then(() =>
          lomakkeenMuokkaus.asetaLomakkeenNimi(
            'Testilomake',
            lomakkeenTunnisteet.lomakkeenId
          )
        )
    })

    after(() => {
      cy.poistaLomake(lomakkeenTunnisteet.lomakkeenAvain)
      cy.server({ enable: false })
    })

    painikkeetYksiValittavissaKoodistostaElementinLisays(
      () => lomakkeenTunnisteet,
      () => {
        it('Asetetaan tieto arkaluontoiseksi', () => {
          getSensitiveAnswer().should('not.be.checked')
          getSensitiveAnswer()
            .check()
            .then(() => getSensitiveAnswer().should('be.checked'))
        })

        it('Aseta kysymys näkyväksi, koska yhteishaussa kysymys on oletuksena piilotettu', () => {
          lomakkeenMuokkaus.teeJaodotaLomakkeenTallennusta(
            lomakkeenTunnisteet.lomakkeenId,
            () => {
              return cy
                .contains('Näkyvyys lomakkeella')
                .click()
                .then(() => {
                  cy.contains('ei näytetä lomakkeella').click()
                })
            }
          )
        })

        hakijanNakymaanSiirtyminen(
          () => lomakkeenTunnisteet,
          () => {
            henkilotietoModuulinTayttaminen(() => {
              painikkeetYksiValittavissaKoodistostaHakijalle()
            })
          }
        )
      }
    )
  })
})
