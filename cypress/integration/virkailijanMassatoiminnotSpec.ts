import kirjautuminenVirkailijanNakymaanKayttajana from '../testit/kirjautuminenVirkailijanNakymaanKayttajana'

describe('Hakemusten massatoiminnot ei-rekisterinpitäjälle', () => {
  kirjautuminenVirkailijanNakymaanKayttajana(
    'OPINTO-OHJAAJA',
    'hakemusten käsittelyä varten',
    () => {
      before(() => {
        cy.fixture('yksiloimatonHakijaHakemuksessa.json').as(
          'yksiloimatonHakija'
        )
        cy.fixture('aidinkieletonHakijaHakemuksessa.json').as(
          'aidinkieletonHakija'
        )
        cy.fixture('hakemuksetmassatoiminnoille.json').as('hakemuslistaus')
        cy.fixture('hautmassatoiminnoille.json').as('haut')
        cy.fixture('randomlomake.json').as('lomake')

        cy.server()
        cy.route('GET', '/valinta-tulos-service/auth/login', {}).as(
          'VTS-kirjautuminen'
        )
        cy.route(
          'GET',
          '/lomake-editori/api/applications/1.2.246.562.11.00000000000000000001',
          '@yksiloimatonHakija'
        ).as('yksiloimatonHakemus')
        cy.route(
          'GET',
          '/lomake-editori/api/applications/1.2.246.562.11.00000000000000000002',
          '@aidinkieletonHakija'
        ).as('aidinkieletonHakemus')
        cy.route(
          'GET',
          '/valinta-tulos-service/auth/valinnan-tulos/hakemus/?hakemusOid=*',
          '[]'
        )
        cy.route(
          'POST',
          '/lomake-editori/api/applications/list',
          '@hakemuslistaus'
        ).as('listApplications')
        cy.route('GET', '/lomake-editori/api/haut*', '@haut')
        cy.route('GET', '/lomake-editori/api/forms/latest*', '@lomake')
        cy.route('GET', '/lomake-editori/api/haku*', '@haut')
      })

      after(() => {
        cy.server({ enable: false })
      })
      const goToApplicationHandling = () => {
        // editorin pääsivujen lataus välillä hidastelee rankasti, joten timeouttia on annettu tässä kohdin reilusti
        cy.get('div.section-link.application > a', { timeout: 30000 }).click()
      }

      const navigateToUnprocessedHautTab = () => {
        cy.get('.application__search-control-tab-selector').first().click()
      }

      const clickFirstHaku = () => {
        cy.get('.application__search-control-haku').first().click()
      }

      describe('Toisen asteen yhteishaussa', () => {
        before(() => {
          goToApplicationHandling()
        })

        beforeEach(() => {
          navigateToUnprocessedHautTab()
          cy.reload()
          clickFirstHaku()
        })

        it('Massaviestipainike on näkyvissä ja massaviesti-ikkuna latautuu oikeilla teksteillä', () => {
          cy.get('[data-test-id="show-results"]').should('exist')
          cy.get('[data-test-id="show-results"]').click()
          cy.get('.application-handling__mass-information-request-link').should(
            'be.visible'
          )
          cy.get('.application-handling__mass-information-request-link').click()
          cy.get(
            '.application-handling__mass-information-request-popup'
          ).should('be.visible')
          cy.get(
            '.application-handling__mass-edit-review-states-title-container > h4'
          ).contains('Massaviesti')
          cy.get('p').contains('Lähetä sähköposti 2 hakijalle').should('exist')
        })

        it('Ilman hakukohderajausta ei näy massamuistiinpanotoimintoa', () => {
          cy.get('[data-test-id="show-results"]').should('exist')
          cy.get('[data-test-id="show-results"]').click()

          cy.get('[data-test-id="mass-review-notes-button"]').should(
            'not.exist'
          )
        })
        it('Hakukohderajauksella massamuistiinpanotoiminto näkyy', () => {
          cy.get('[data-test-id="hakukohde-rajaus"]').click()
          cy.get('.hakukohde-and-hakukohderyhma-category-list-item')
            .first()
            .click()
          cy.get('[data-test-id="show-results"]').click()
          cy.get('[data-test-id="mass-review-notes-button"]').should('exist')
        })
      })
    }
  )
})
