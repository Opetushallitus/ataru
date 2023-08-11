import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'
import { unsafeFoldOption } from '../option'
import { tekstikentta } from '../testit/lomake-elementit/tekstikentta/virkailija/lomakkeet/tekstikentta'

describe('Lomake-editori lomakkeen kopiointi', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    let lomakkeenAvain: string
    let lomakkeenId: number
    let kopioLomakeAvain: string

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
        .then(() => tekstikentta.lisaaTekstikentta(lomakkeenId))
        .then(() =>
          tekstikentta.asetaKysymys('Näytettävän tekstikentän kysymys')
        )
        .then(() => tekstikentta.naytaTekstiKentta())
    })

    after(() => {
      cy.poistaLomake(lomakkeenAvain)
      cy.poistaLomake(kopioLomakeAvain)
      cy.server({ enable: false })
    })

    it('Kopioi lomakkeen', () => {
      lomakkeenMuokkaus
        .kopioiLomake()
        .then((lomake) => {
          kopioLomakeAvain = unsafeFoldOption(lomake.lomakkeenAvain)
        })
        .then(() =>
          cy
            .get('[data-test-id=form-name-input]')
            .should('have.value', 'Testilomake - KOPIO')
        )
    })

    it('Tekstikenttä näytetään kopioidussa lomakkeessa', () =>
      cy
        .get('button.editor-form__component-fold-button')
        .click()
        .then(() =>
          tekstikentta
            .nakyvyysLomakkeellaLabel()
            .should('have.text', 'näkyy kaikille')
        ))
  })
})
