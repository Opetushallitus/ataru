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
    })

    afterEach(() => {
      cy.server({ enable: false })
    })

    it('Navigoi hakemusten käsittelynäkymään', () => {
      // editorin pääsivujen lataus välillä hidastelee rankasti, joten timeouttia on annettu tässä kohdin reilusti
      cy.get('div.section-link.application > a', { timeout: 30000 }).click()
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
