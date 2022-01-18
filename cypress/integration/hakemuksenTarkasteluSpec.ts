import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import avaaHenkilonHakemus from '../testit/hakemusEditorinVirheidenTarkastus'

describe('Hakemuksen tietojen tarkastelu', () => {
  kirjautuminenVirkailijanNakymaan('hakemusten käsittelyä varten', () => {
    beforeEach(() => {
      cy.fixture('yksiloimatonHakijaHakemuksessa.json').as('yksiloimatonHakija')
      cy.fixture('aidinkieletonHakijaHakemuksessa.json').as(
        'aidinkieletonHakija'
      )
      cy.fixture('hakemukset.json').as('hakemuslistaus')
      cy.fixture('lahtokoulu.json').as('lahtokoulu')
      cy.fixture('lahtokoulut.json').as('lahtokoulut')
      cy.fixture('lahtokoulunLuokat.json').as('luokat')

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
      cy.route(
        'GET',
        '/lomake-editori/api/applications/oppilaitos/1.2.3.4.6/luokat',
        '@luokat'
      )
    })

    afterEach(() => {
      cy.server({ enable: false })
    })

    it('Navigoi hakemusten käsittelynäkymään', () => {
      // editorin pääsivujen lataus välillä hidastelee rankasti, joten timeouttia on annettu tässä kohdin reilusti
      cy.get('div.section-link.application > a', { timeout: 30000 }).click()
    })

    describe('Hakemusten rajaus', () => {
      it('Lähtökouluksi voi hakea käyttäjän organisaatioita', () => {
        const searchAndAssertSchoolOptions = (
          searchTerm: string,
          expectation: string,
          expectedValue?: number
        ) => {
          cy.get('#school-search').clear().type(searchTerm, { delay: 50 })
          if (expectedValue) {
            cy.get('div.school-filter__option')
              .its('length')
              .should(expectation, expectedValue)
          } else {
            cy.get('div.school-filter__option').should(expectation)
          }
        }
        cy.route(
          'GET',
          '/lomake-editori/api/organization/user-organizations?organizations=true&hakukohde-groups=false&perusaste-only=true&oppilaitos-only=true&results-page=10000',
          '@lahtokoulut'
        )
        cy.get(
          '.application__search-control-tab-selector-wrapper--search'
        ).click()
        cy.get('#ssn-search-field').clear().type('Tuntematon', { delay: 50 })
        cy.get('#open-application-filters').click()
        cy.get('#selected-school').should('not.exist')
        searchAndAssertSchoolOptions('perus', 'be', 3)
        searchAndAssertSchoolOptions('Haa', 'be', 1)
        searchAndAssertSchoolOptions('haaga', 'be', 1)
        searchAndAssertSchoolOptions('1.2.3.4.7', 'be', 1)
        searchAndAssertSchoolOptions('lukio', 'not.exist')
        searchAndAssertSchoolOptions('Pell', 'be', 1)
      })

      it('Lähtökouluksi voi asettaa käyttäjän organisaation', () => {
        cy.get('div.school-filter__option').its(0).click()
        cy.get('#selected-school').should('exist')
        cy.get('#selected-school').contains('Pellon peruskoulu')
        cy.get('#school-search').should('not.exist')
      })

      it('Hakijoiden luokat voi valita lähtökoulusta', () => {
        cy.get('.multi-option-dropdown__dropdown').click()
        cy.get('li.multi-option-dropdown__option').its('length').should('be', 4)
      })

      it('Lähtökoulu valinnan voi poistaa jolloin myös valittavat luokat poistuu', () => {
        cy.get('#remove-selected-school-button').click()
        cy.get('#selected-school').should('not.exist')
        cy.get('#school-search').should('exist')
        cy.get('.multi-option-dropdown__dropdown').click()
        cy.get('li.multi-option-dropdown__option').should('not.exist')
      })

      it('Hakemusten rajauksessa on valittu lähtökouluksi käyttäjän ainoa organisaatio', () => {
        cy.reload()
        cy.route(
          'GET',
          '/lomake-editori/api/organization/user-organizations?organizations=true&hakukohde-groups=false&perusaste-only=true&oppilaitos-only=true&results-page=10000',
          '@lahtokoulu'
        )
        cy.get(
          '.application__search-control-tab-selector-wrapper--search'
        ).click()
        cy.get('#ssn-search-field').clear().type('Tuntematon', { delay: 50 })
        cy.get('#open-application-filters').click()
        cy.get('#school-search').should('not.exist')
        cy.get('#selected-school').should('exist')
        cy.get('#selected-school').contains('Haagan peruskoulu')
        cy.get('#open-application-filters').click()
      })
    })

    describe('Hakemusten toiminnallisuuksien testaus', () => {
      avaaHenkilonHakemus(
        'Toimivan hakemuksen lähettänyt testihenkilö',
        'Tatu Tuntematon',
        '1.2.246.562.11.00000000000000000001',
        () => {
          it('Varmista, että hakemuslomakkeen kysymys ja vastaus näkyvät', () => {
            cy.get('#beb7f478-8fe0-4db3-984b-2a9b06d290f6').should('be.visible')
            cy.get('#beb7f478-8fe0-4db3-984b-2a9b06d290f6 > label').contains(
              'Kuinka paljon ajattelit pitää lomaa?'
            )
            cy.get(
              '#beb7f478-8fe0-4db3-984b-2a9b06d290f6 > div > div > p'
            ).contains('rutkasti')
          })
        }
      )
    })

    describe('Hakemuksen virhetilanteiden esittäminen virkailijalle', () => {
      avaaHenkilonHakemus(
        'Testihenkilö, jolta puuttuu äidinkieli',
        'Erkki Esimerkki',
        '1.2.246.562.11.00000000000000000002',
        () => {
          it('Varmista, että oikeat virheviestit näkyvät', () => {
            cy.get('#notification-label-henkilo-info-incomplete').should(
              'be.visible'
            )
            cy.get('#notification-link-henkilo-info-incomplete').should(
              'be.visible'
            )
          })
        }
      )
    })
  })
})
