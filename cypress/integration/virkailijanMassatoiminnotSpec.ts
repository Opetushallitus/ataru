import kirjautuminenVirkailijanNakymaanKayttajana from '../testit/kirjautuminenVirkailijanNakymaanKayttajana'

describe('Hakemusten massatoiminnot ei-rekisterinpitäjälle', () => {
  kirjautuminenVirkailijanNakymaanKayttajana(
    'OPINTO-OHJAAJA',
    'hakemusten käsittelyä varten',
    () => {
      beforeEach(() => {
        cy.fixture('hakemuksetmassatoiminnoille.json').as('hakemuslistaus')
        cy.fixture('hautmassatoiminnoille.json').as('haut')

        cy.server()
        cy.route(
          'GET',
          'lomake-editori/api/valinta-tulos-service/valinnan-tulos/hakemus/?hakemusOid=*',
          '[]'
        )
        cy.route(
          'POST',
          '/lomake-editori/api/applications/list',
          '@hakemuslistaus'
        ).as('listApplications')
        cy.route('GET', '/lomake-editori/api/haku*', '@haut')
        cy.route('GET', '/lomake-editori/api/haut*', '@haut')
      })

      afterEach(() => {
        cy.server({ enable: false })
      })

      it('Massaviestipainike on näkyvissä ja massaviesti-ikkuna latautuu oikeilla teksteillä', () => {
        cy.avaaHaunHakemuksetVirkailijanNakymassa(
          '1.2.246.562.29.00000000000000018308'
        )
        cy.get('[data-test-id="show-results"]').should('be.visible')
        cy.get('[data-test-id="show-results"]').click()
        cy.get('.application-handling__mass-information-request-link').should(
          'be.visible'
        )
        cy.get('.application-handling__mass-information-request-link').click()
        cy.get('.application-handling__mass-information-request-popup').should(
          'be.visible'
        )
        cy.get('[data-test-id="mass-send-update-link"]').should('be.visible')
        cy.contains(
          'Viestin mukana lähetetään hakemuksen muokkauslinkki hakijalle'
        ).should('be.visible')
        cy.get(
          '.application-handling__mass-edit-review-states-title-container > h4'
        ).contains('Massaviesti')
        cy.get('p').contains('Lähetä sähköposti 2 hakijalle').should('exist')
      })

      it('Toisen asteen yhteishaussa ei näy massamuistiinpanotoimintoa', () => {
        cy.avaaHaunHakemuksetVirkailijanNakymassa('1.2.246.562.29.10000000001')
        cy.get('[data-test-id="show-results"]').should('be.visible')
        cy.get('[data-test-id="show-results"]').click()
        cy.get('.application-handling__mass-information-request-link').should(
          'be.visible'
        )
        cy.get('[data-test-id="mass-review-notes-button"]').should(
          'not.be.visible'
        )
      })

      it('Ei-yhteishaulle massamuistiinpanotoiminto näkyy', () => {
        cy.avaaHaunHakemuksetVirkailijanNakymassa(
          '1.2.246.562.29.00000000000000018308'
        )
        cy.get('[data-test-id="show-results"]').click()
        cy.get('[data-test-id="mass-review-notes-button"]').should('be.visible')
      })
    }
  )
})
