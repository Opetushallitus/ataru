import * as hakemuseditori from '../hakemuseditori'
import * as hakemuksentaytto from '../hakemuksentaytto'
import * as routes from '../routes'
import { unsafeFoldOption } from '../option'

describe('Hakemuspalvelu', () => {
  let formKey: string

  describe('Virkailijanäkymä', () => {
    before(() => {
      Cypress.Cookies.defaults({
        whitelist: ['ring-session'],
      })
      cy.loginToVirkailija()
    })

    it('Avaa hakemuspalvelun editorinäkymän', () => {
      hakemuseditori.getAddFormButton().should('be.enabled')
    })

    describe('Uuden lomakkeen luonti', () => {
      let formId: number

      before(() => {
        hakemuseditori.addForm().then((form) => {
          formKey = unsafeFoldOption(form.formKey)
          formId = unsafeFoldOption(form.formId)
        })
      })

      after(() => {
        cy.deleteForm(formKey)
      })

      it('Näyttää uuden lomakkeen luontinäkymän', () => {
        cy.url().should((url) => expect(url.endsWith(formKey)).to.be.true)
        hakemuseditori
          .getFormNameInput()
          .should('have.attr', 'placeholder', 'Lomakkeen nimi')
        hakemuseditori.getFormNameInput().should('have.value', 'Uusi lomake')
        hakemuseditori
          .getPreviewLink()
          .should('have.text', 'FI')
          .should(
            'have.attr',
            'href',
            routes.virkailija.getFormPreviewUrl(formKey)
          )
      })

      it('Näyttää hakukohdeet -moduulin', () => {
        hakemuseditori.hakukohteet
          .getHeaderLabel()
          .should('have.text', 'Hakukohteet')
      })

      it('Näyttää henkilötietomoduulin', () => {
        hakemuseditori.henkilotiedot
          .getHeaderLabel()
          .should('have.text', 'Henkilötiedot')
        hakemuseditori.henkilotiedot
          .getSelectComponent()
          .find(':selected')
          .should('have.attr', 'value', 'onr')
          .should('have.text', 'Opiskelijavalinta')
        hakemuseditori.henkilotiedot
          .getFieldsLabel()
          .should(
            'have.text',
            'Etunimet, Kutsumanimi, Sukunimi, Kansalaisuus, Onko sinulla suomalainen henkilötunnus?, Henkilötunnus, Syntymäaika, Sukupuoli, Syntymäpaikka ja -maa, Passin numero, Kansallinen ID-tunnus, Sähköpostiosoite, Matkapuhelin, Asuinmaa, Katuosoite, Postinumero, Postitoimipaikka, Kotikunta, Kaupunki ja maa, Äidinkieli'
          )
      })

      describe('Henkilötietomoduulin kenttien vaihtaminen', () => {
        before(() => {
          hakemuseditori.henkilotiedot.selectOption('Muu käyttö', formId)
        })

        it('Näyttää henkilötietomoduulin muutetut kentät', () => {
          hakemuseditori.henkilotiedot
            .getFieldsLabel()
            .should(
              'have.text',
              'Etunimet, Kutsumanimi, Sukunimi, Kansalaisuus, Onko sinulla suomalainen henkilötunnus?, Henkilötunnus, Syntymäaika, Sähköpostiosoite, Matkapuhelin, Asuinmaa, Katuosoite, Postinumero, Postitoimipaikka, Kotikunta, Kaupunki ja maa'
            )
        })

        after(() => {
          hakemuseditori.henkilotiedot.selectOption('Opiskelijavalinta', formId)
        })
      })

      describe('Lomakkeen tietojen täyttäminen', () => {
        before(() => {
          hakemuseditori.setFormName('Testilomake', formId)
        })

        it('Näyttää muokatun lomakkeen nimen', () => {
          hakemuseditori.getFormNameInput().should('have.value', 'Testilomake')
        })

        describe('Hakemuspalvelun hakijan näkymään siirtyminen', () => {
          before(() => {
            cy.loadHakija(formKey)
          })
          it('Lataa hakemuspalvelun hakijanäkymän', () => {
            hakemuksentaytto
              .getApplicationLabel()
              .should('have.text', 'Testilomake')
          })
        })
      })
    })
  })
})
