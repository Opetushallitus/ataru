import kirjautuminenVirkailijanNakymaanKayttajana from '../testit/kirjautuminenVirkailijanNakymaanKayttajana'

describe('Hakemusten filtteröinti kevyt-valinnan tiedoilla', () => {
  kirjautuminenVirkailijanNakymaanKayttajana(
    '1.2.246.562.11.11111111111',
    'hakemusten käsittelyä varten',
    () => {
      beforeEach(() => {
        cy.fixture('hautkevyt.json').as('hautkevyt')
        cy.fixture('hakemuksetkevyt.json').as('hakemuslistauskevyt')
        cy.fixture('valinnantulokset.json').as('valinnantulokset')
        cy.fixture('kayttaavalintalaskentaa.json').as('kayttaavalintalaskentaa')

        cy.server()
        cy.route('GET', '/valinta-tulos-service/auth/login', {}).as(
          'VTS-kirjautuminen'
        )
        cy.route(
          'GET',
          '/valinta-tulos-service/auth/valinnan-tulos/hakemus/?hakemusOid=*',
          '[]'
        )
        cy.route(
          'POST',
          '/valinta-tulos-service/auth/valinnan-tulos/hakemus/',
          '@valinnantulokset'
        )
        cy.route(
          'GET',
          '/lomake-editori/api/valintalaskentakoostepalvelu/valintaperusteet/hakukohde/1.2.246.562.20.10000000001/kayttaa-valintalaskentaa',
          '@kayttaavalintalaskentaa'
        )
        cy.route(
          'POST',
          '/lomake-editori/api/applications/list',
          '@hakemuslistauskevyt'
        ).as('listApplications')
        cy.route('GET', '/lomake-editori/api/haut*', '@hautkevyt')
      })

      afterEach(() => {
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

      describe('Kevyt-valinta-vastaanotto-filtteri', () => {
        before(() => {
          goToApplicationHandling()
        })

        after(() => {
          navigateToUnprocessedHautTab()
          cy.reload()
        })

        it('Hakemuksia voi filtteröidä vastaanoton tilalla', () => {
          clickFirstHaku()
          cy.get('[data-test-id=show-results]').click()
          cy.get('.application-handling__list--expanded')
            .find('.application-handling__list-row')
            .should('have.length', 2)
          cy.get('.application-handling__list-row--vastaanotto').click()
          cy.get(
            '.application-handling__filter-state-selection-row--all > label > input'
          ).click()
          cy.get('.application-handling__list--expanded')
            .find('.application-handling__list-row')
            .should('have.length', 0)

          cy.get('.application-handling__filter-state-selection')
            .find('span')
            .should('have.length', 7)
          cy.get('.application-handling__filter-state-selection')
            .find('span')
            .filter(':contains("Vastaanottanut (1)")')
            .should('have.length', 1)
          cy.get('.application-handling__filter-state-selection')
            .find('span')
            .filter(':contains("Kesken (1)")')
            .should('have.length', 1)
          cy.get('.application-handling__filter-state-selection')
            .find('span')
            .filter(':contains("Vastaanottanut (1)")')
            .first()
            .click()
          cy.get('.application-handling__list--expanded')
            .find('.application-handling__list-row')
            .should('have.length', 1)
        })
      })
    }
  )
})
