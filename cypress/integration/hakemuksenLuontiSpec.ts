import * as hakemuksenMuokkaus from '../hakemuksenMuokkaus'
import * as hakijanNakyma from '../hakijanNakyma'
import * as reitit from '../reitit'
import { unsafeFoldOption } from '../option'

describe('Hakemuksen luonti', () => {
  let lomakkeenAvain: string

  describe('Virkailijanäkymä', () => {
    before(() => {
      Cypress.Cookies.defaults({
        whitelist: ['ring-session'],
      })
      cy.kirjauduVirkailijanNakymaan()
    })

    it('Avaa hakemuspalvelun editorinäkymän', () => {
      hakemuksenMuokkaus.haeLomakkeenLisaysNappi().should('be.enabled')
    })

    describe('Uuden lomakkeen luonti', () => {
      let lomakkeenId: number

      before(() => {
        hakemuksenMuokkaus.lisaaLomake().then((lomake) => {
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
        hakemuksenMuokkaus
          .haeLomakkeenNimenSyote()
          .should('have.attr', 'placeholder', 'Lomakkeen nimi')
        hakemuksenMuokkaus
          .haeLomakkeenNimenSyote()
          .should('have.value', 'Uusi lomake')
        hakemuksenMuokkaus
          .haeLomakkeenEsikatseluLinkki()
          .should('have.text', 'FI')
          .should(
            'have.attr',
            'href',
            reitit.virkailija.haeLomakkeenEsikatseluOsoite(lomakkeenAvain)
          )
      })

      it('Näyttää hakukohdeet -moduulin', () => {
        hakemuksenMuokkaus.hakukohteet
          .haeOtsikko()
          .should('have.text', 'Hakukohteet')
      })

      it('Näyttää henkilötietomoduulin', () => {
        hakemuksenMuokkaus.henkilotiedot
          .haeOtsikko()
          .should('have.text', 'Henkilötiedot')
        hakemuksenMuokkaus.henkilotiedot
          .haeHenkilotietojenValintaKomponentti()
          .find(':selected')
          .should('have.attr', 'value', 'onr')
          .should('have.text', 'Opiskelijavalinta')
        hakemuksenMuokkaus.henkilotiedot
          .haeKaytettavatHenkilotietoKentat()
          .should(
            'have.text',
            'Etunimet, Kutsumanimi, Sukunimi, Kansalaisuus, Onko sinulla suomalainen henkilötunnus?, Henkilötunnus, Syntymäaika, Sukupuoli, Syntymäpaikka ja -maa, Passin numero, Kansallinen ID-tunnus, Sähköpostiosoite, Matkapuhelin, Asuinmaa, Katuosoite, Postinumero, Postitoimipaikka, Kotikunta, Kaupunki ja maa, Äidinkieli'
          )
      })

      describe('Henkilötietomoduulin kenttien vaihtaminen', () => {
        before(() => {
          hakemuksenMuokkaus.henkilotiedot.valitseHenkilotietolomakkeenKentat(
            'Muu käyttö',
            lomakkeenId
          )
        })

        it('Näyttää henkilötietomoduulin muutetut kentät', () => {
          hakemuksenMuokkaus.henkilotiedot
            .haeKaytettavatHenkilotietoKentat()
            .should(
              'have.text',
              'Etunimet, Kutsumanimi, Sukunimi, Kansalaisuus, Onko sinulla suomalainen henkilötunnus?, Henkilötunnus, Syntymäaika, Sähköpostiosoite, Matkapuhelin, Asuinmaa, Katuosoite, Postinumero, Postitoimipaikka, Kotikunta, Kaupunki ja maa'
            )
        })

        after(() => {
          hakemuksenMuokkaus.henkilotiedot.valitseHenkilotietolomakkeenKentat(
            'Opiskelijavalinta',
            lomakkeenId
          )
        })
      })

      describe('Lomakkeen tietojen täyttäminen', () => {
        before(() => {
          hakemuksenMuokkaus.asetaLomakkeenNimi('Testilomake', lomakkeenId)
          hakemuksenMuokkaus.komponentinLisays
            .lisaaArvosanat(lomakkeenId)
            .then(({ result: arvosanatLinkki }) =>
              cy
                .wrap(arvosanatLinkki.text())
                .as('component-toolbar-arvosanat-text')
            )
        })

        it.only('Näyttää arvosanat -osion', () => {
          cy.get('@component-toolbar-arvosanat-text').then((arvosanatTeksti) =>
            expect(arvosanatTeksti).to.equal('Arvosanat (peruskoulu)')
          )
          hakemuksenMuokkaus.komponentinLisays
            .haeLisaaArvosanatLinkki()
            .should('have.text', 'Arvosanat (peruskoulu)')
          hakemuksenMuokkaus.arvosanat
            .haeOsionNimi()
            .should('have.text', 'Arvosanat (peruskoulu)')
        })

        it('Näyttää muokatun lomakkeen nimen', () => {
          hakemuksenMuokkaus
            .haeLomakkeenNimenSyote()
            .should('have.value', 'Testilomake')
        })

        describe('Hakemuspalvelun hakijan näkymään siirtyminen', () => {
          before(() => {
            cy.avaaLomakeHakijanNakymassa(lomakkeenAvain)
          })
          it('Lataa hakemuspalvelun hakijanäkymän', () => {
            hakijanNakyma.haeHakemuksenNimi().should('have.text', 'Testilomake')
          })
        })
      })
    })
  })
})
