import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'
import * as hakijanNakyma from '../hakijanNakyma'
import * as reitit from '../reitit'
import { unsafeFoldOption } from '../option'

describe('Lomakkeen luonti', () => {
  let lomakkeenAvain: string

  describe('Virkailijanäkymä', () => {
    before(() => {
      Cypress.Cookies.defaults({
        whitelist: ['ring-session'],
      })
      cy.kirjauduVirkailijanNakymaan()
    })

    it('Avaa lomakkeen muokkausnäkymän', () => {
      lomakkeenMuokkaus.haeLomakkeenLisaysNappi().should('be.enabled')
    })

    describe('Uuden lomakkeen luonti', () => {
      let lomakkeenId: number

      before(() => {
        lomakkeenMuokkaus.lisaaLomake().then((lomake) => {
          lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)
          lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
        })
      })

      after(() => {
        cy.poistaLomake(lomakkeenAvain)
      })

      it('Näyttää uuden lomakkeen luontinäkymän', () => {
        cy.url().should(
          (osoite) => expect(osoite.endsWith(lomakkeenAvain)).to.be.true
        )
        lomakkeenMuokkaus
          .haeLomakkeenNimenSyote()
          .should('have.attr', 'placeholder', 'Lomakkeen nimi')
        lomakkeenMuokkaus
          .haeLomakkeenNimenSyote()
          .should('have.value', 'Uusi lomake')
        lomakkeenMuokkaus
          .haeLomakkeenEsikatseluLinkki()
          .should('have.text', 'FI')
          .should(
            'have.attr',
            'href',
            reitit.virkailija.haeLomakkeenEsikatseluOsoite(lomakkeenAvain)
          )
      })

      it('Näyttää hakukohdeet -moduulin', () => {
        lomakkeenMuokkaus.hakukohteet
          .haeOtsikko()
          .should('have.text', 'Hakukohteet')
      })

      it('Näyttää henkilötietomoduulin', () => {
        lomakkeenMuokkaus.henkilotiedot
          .haeOtsikko()
          .should('have.text', 'Henkilötiedot')
        lomakkeenMuokkaus.henkilotiedot
          .haeHenkilotietojenValintaKomponentti()
          .find(':selected')
          .should('have.attr', 'value', 'onr')
          .should('have.text', 'Opiskelijavalinta')
        lomakkeenMuokkaus.henkilotiedot
          .haeKaytettavatHenkilotietoKentat()
          .should(
            'have.text',
            'Etunimet, Kutsumanimi, Sukunimi, Kansalaisuus, Onko sinulla suomalainen henkilötunnus?, Henkilötunnus, Syntymäaika, Sukupuoli, Syntymäpaikka ja -maa, Passin numero, Kansallinen ID-tunnus, Sähköpostiosoite, Matkapuhelin, Asuinmaa, Katuosoite, Postinumero, Postitoimipaikka, Kotikunta, Kaupunki ja maa, Äidinkieli'
          )
      })

      describe('Henkilötietomoduulin kenttien vaihtaminen', () => {
        before(() => {
          lomakkeenMuokkaus.henkilotiedot.valitseHenkilotietolomakkeenKentat(
            'Muu käyttö',
            lomakkeenId
          )
        })

        it('Näyttää henkilötietomoduulin muutetut kentät', () => {
          lomakkeenMuokkaus.henkilotiedot
            .haeKaytettavatHenkilotietoKentat()
            .should(
              'have.text',
              'Etunimet, Kutsumanimi, Sukunimi, Kansalaisuus, Onko sinulla suomalainen henkilötunnus?, Henkilötunnus, Syntymäaika, Sähköpostiosoite, Matkapuhelin, Asuinmaa, Katuosoite, Postinumero, Postitoimipaikka, Kotikunta, Kaupunki ja maa'
            )
        })

        after(() => {
          lomakkeenMuokkaus.henkilotiedot.valitseHenkilotietolomakkeenKentat(
            'Opiskelijavalinta',
            lomakkeenId
          )
        })
      })

      describe('Lomakkeen tietojen täyttäminen', () => {
        before(() => {
          lomakkeenMuokkaus.asetaLomakkeenNimi('Testilomake', lomakkeenId)
          lomakkeenMuokkaus.komponentinLisays
            .lisaaArvosanat(lomakkeenId)
            .then(({ result: arvosanatLinkki }) =>
              cy
                .wrap(arvosanatLinkki.text())
                .as('component-toolbar-arvosanat-text')
            )
        })

        it('Näyttää arvosanat -osion', () => {
          cy.get('@component-toolbar-arvosanat-text').then((arvosanatTeksti) =>
            expect(arvosanatTeksti).to.equal('Arvosanat (peruskoulu)')
          )
          lomakkeenMuokkaus.komponentinLisays
            .haeLisaaArvosanatLinkki()
            .should('have.text', 'Arvosanat (peruskoulu)')
          lomakkeenMuokkaus.arvosanat
            .haeOsionNimi()
            .should('have.text', 'Arvosanat (peruskoulu)')
          lomakkeenMuokkaus.arvosanat.haePoistaOsioNappi().should('be.enabled')
        })

        describe('Arvosanat -osion poistaminen', () => {
          before(() => {
            lomakkeenMuokkaus.arvosanat.poistaArvosanat(lomakkeenId)
          })

          it('Poistaa arvosanat -osion lomakkeelta', () => {
            lomakkeenMuokkaus.arvosanat.haeOsionNimi().should('not.exist')
          })

          after(() => {
            lomakkeenMuokkaus.komponentinLisays.lisaaArvosanat(lomakkeenId)
          })
        })

        it('Näyttää muokatun lomakkeen nimen', () => {
          lomakkeenMuokkaus
            .haeLomakkeenNimenSyote()
            .should('have.value', 'Testilomake')
        })

        describe.skip('Hakijan näkymään siirtyminen', () => {
          before(() => {
            cy.avaaLomakeHakijanNakymassa(lomakkeenAvain)
          })
          it('Lataa hakijan näkymän', () => {
            hakijanNakyma.haeLomakkeenNimi().should('have.text', 'Testilomake')
          })
        })
      })
    })
  })
})
