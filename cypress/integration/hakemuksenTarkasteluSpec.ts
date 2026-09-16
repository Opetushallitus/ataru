import avaaHenkilonHakemus from '../testit/hakemusEditorinVirheidenTarkastus'
import kirjautuminenVirkailijanNakymaanKayttajana from '../testit/kirjautuminenVirkailijanNakymaanKayttajana'

describe('Hakemuksen tietojen tarkastelu', () => {
  kirjautuminenVirkailijanNakymaanKayttajana(
    'OPINTO-OHJAAJA',
    'hakemusten käsittelyä varten',
    () => {
      beforeEach(() => {
        cy.fixture('yksiloimatonHakijaHakemuksessa.json').as(
          'yksiloimatonHakija'
        )
        cy.fixture('aidinkieletonHakijaHakemuksessa.json').as(
          'aidinkieletonHakija'
        )
        cy.fixture('hakemukset.json').as('hakemuslistaus')
        cy.fixture('lahtokoulu.json').as('lahtokoulu')
        cy.fixture('lahtokoulut.json').as('lahtokoulut')
        cy.fixture('lahtokoulunLuokat.json').as('luokat')
        cy.fixture('haut.json').as('haut')

        cy.server()
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
          '/lomake-editori/api/valinta-tulos-service/valinnan-tulos/hakemus?hakemusOid=*',
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
        cy.route('GET', '/lomake-editori/api/haut*', '@haut')
        cy.route(
          'GET',
          '/lomake-editori/api/organization/user-organizations*',
          '@lahtokoulut'
        )
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

      it('Navigoi hakemusten käsittelynäkymään', () => {
        goToApplicationHandling()
      })

      describe('Hakemusten rajaus toisen asteen yhteishaussa', () => {
        before(() => {
          goToApplicationHandling()
        })

        after(() => {
          navigateToUnprocessedHautTab()
          cy.reload()
        })

        it('Valpas-linkissä ei ole lähtökoulua', () => {
          clickFirstHaku()
          cy.get('#open-application-filters').click()
          cy.get('#valpas-hakutilanne-link')
            .should('have.attr', 'href')
            .and('match', /.*\/hakutilanne\/$/)
        })

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

        it('Valpas-linkissä on lähtökoulu', () => {
          cy.get('#valpas-hakutilanne-link')
            .should('have.attr', 'href')
            .and('match', /.*\/hakutilanne\/1\.2\.3\.4\.6$/)
        })

        it('Hakijoiden luokat voi valita lähtökoulusta', () => {
          cy.get('.multi-option-dropdown__dropdown').click()
          cy.get('li.multi-option-dropdown__option')
            .its('length')
            .should('be', 4)
        })

        it('Lähtökoulu valinnan voi poistaa jolloin myös valittavat luokat poistuu', () => {
          cy.get('#remove-selected-school-button').click()
          cy.get('#selected-school').should('not.exist')
          cy.get('#school-search').should('exist')
          cy.get('.multi-option-dropdown__dropdown').click()
          cy.get('li.multi-option-dropdown__option').should('not.exist')
        })

        describe('Käyttäjällä on oikeus vain yhteen organisaatioon', () => {
          // Lähtökoulukyselyn dispatchaa ainoastaan #open-application-filters -klikkaus (ja
          // haun/lomakkeen valinta), ja molemmat on ehdollistettu käyttäjän roolilla, joka
          // selviää vasta asynkronisesti /user-info -kutsusta. Jos klikkaus ehtii ennen sitä,
          // kyselyä ei koskaan tehdä eikä sovellus yritä sitä uudelleen. Avataan siis suodatin
          // uudestaan kunnes lähtökoulusuodatin on oikeasti renderöitynyt.
          const openFiltersUntilSchoolFilterRendered = (attemptsLeft = 8) => {
            cy.get('#open-application-filters').click()
            cy.get('.application-handling__filters-popup').should('exist')
            cy.get('body').then(($body) => {
              if ($body.find('#school-search, #selected-school').length === 0) {
                if (attemptsLeft <= 1) {
                  throw new Error(
                    'Lähtökoulusuodatin ei renderöitynyt suodatinikkunaan'
                  )
                }
                cy.get('#open-application-filters').click()
                cy.get('.application-handling__filters-popup').should(
                  'not.exist'
                )
                // Odotus ei ole settle-arvaus vaan antaa /user-info -kutsulle aikaa ehtiä
                // maaliin ennen seuraavaa yritystä (8 x 1s).
                cy.wait(1000)
                openFiltersUntilSchoolFilterRendered(attemptsLeft - 1)
              }
            })
          }

          it('Hakemusten rajauksessa on valittu lähtökouluksi käyttäjän ainoa organisaatio', () => {
            // Tämä route on rekisteröitävä uloimman beforeEachin (3 organisaatiota) jälkeen,
            // jotta se voittaa — järjestysriippuvuus, joka rikkoutuu helposti.
            cy.route(
              'GET',
              '/lomake-editori/api/organization/user-organizations*',
              '@lahtokoulu'
            )
            goToApplicationHandling()
            navigateToUnprocessedHautTab()
            cy.reload()
            clickFirstHaku()

            openFiltersUntilSchoolFilterRendered()

            cy.get('#school-search').should('not.exist')
            cy.get('#selected-school', { timeout: 15000 }).should('exist')
            cy.get('#selected-school').contains('Haagan peruskoulu')
            cy.get('#open-application-filters').click()
          })
        })
      })

      describe('Hakemusten toiminnallisuuksien testaus', () => {
        avaaHenkilonHakemus(
          'Toimivan hakemuksen lähettänyt testihenkilö',
          'Tatu Tuntematon',
          '1.2.246.562.11.00000000000000000001',
          () => {
            it('Varmista, että hakemuslomakkeen kysymys ja vastaus näkyvät', () => {
              cy.get('#beb7f478-8fe0-4db3-984b-2a9b06d290f6').should(
                'be.visible'
              )
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

      describe('Massaviestin lähetystoiminto', () => {
        avaaHenkilonHakemus(
          'Toimivan hakemuksen lähettänyt testihenkilö',
          'Tatu Tuntematon',
          '1.2.246.562.11.00000000000000000001',
          () => {
            it('Massaviestipainike on näkyvissä', () => {
              cy.get(
                '.application-handling__mass-information-request-link'
              ).should('be.visible')
            })
            it('Massaviesti-ikkuna latautuu oikeilla teksteillä', () => {
              cy.get(
                '.application-handling__mass-information-request-link'
              ).click()
              cy.get(
                '.application-handling__mass-information-request-popup'
              ).should('be.visible')
              cy.get(
                '.application-handling__mass-edit-review-states-title-container > h4'
              ).contains('Massaviesti')
              cy.get('p')
                .contains('Lähetä sähköposti 2 hakijalle')
                .should('exist')
            })
          }
        )
      })

      describe('Yksittäisen viestin lähetystoiminto', () => {
        avaaHenkilonHakemus(
          'Toimivan hakemuksen lähettänyt testihenkilö',
          'Tatu Tuntematon',
          '1.2.246.562.11.00000000000000000001',
          () => {
            it('Yksittäisen viestin lähetyspainike on näkyvissä', () => {
              cy.get('.application-handling__send-message-button').should(
                'be.visible'
              )
            })
            it('Yksittäisen viestin ikkuna latautuu oikeilla teksteillä', () => {
              cy.get('.application-handling__send-message-button').click()
              cy.get(
                '.application-handling__-information-request-popup'
              ).should('be.visible')
              cy.get(
                '.application-handling__mass-edit-review-states-title-container > h4'
              ).contains('Viesti')
              cy.get('p')
                .contains('Olet lähettämässä sähköpostia 1 hakijalle')
                .should('exist')
            })
          }
        )
      })
    }
  )
})
