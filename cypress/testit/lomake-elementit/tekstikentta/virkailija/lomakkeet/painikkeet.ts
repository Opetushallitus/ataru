import * as asetukset from '../../../../../asetukset'
import * as lomakkeenMuokkaus from '../../../../../lomakkeenMuokkaus'

const haeLisaaLinkki = () =>
  cy.get('[data-test-id=component-toolbar-painikkeet-yksi-valittavissa]')
const haeKysymysKenttä = () =>
  cy.get('[data-test-id=editor-form__singleChoice-label]')
const haeLomakeosionPiilottaminenArvonPerusteella = () =>
  cy.get('[data-test-id=dropdown-lomakeosion-nayttaminen-arvon-perusteella]')
const haeLomakeosionPiilottaminenArvonPerusteellaVertailuarvo = () =>
  cy.get(
    '[data-test-id=dropdown-lomakeosion-piilottaminen-arvon-perusteella-vertailuarvo]'
  )
const haeLomakeosionPiilottaminenArvonPerusteellaOsio = () =>
  cy.get(
    '[data-test-id=dropdown-lomakeosion-piilottaminen-arvon-perusteella-osio]'
  )

export const painikkeet = {
  lisaaPainikkeetYksiValittavissa: (formId: number) =>
    lomakkeenMuokkaus.teeJaodotaLomakkeenTallennusta(formId, () => {
      lomakkeenMuokkaus.komponentinLisays.avaaValikko()
      return haeLisaaLinkki().click()
    }),

  asetaKysymys: (teksti: string) => {
    return haeKysymysKenttä().type(teksti, {
      delay: asetukset.tekstikentanSyotonViive,
    })
  },

  valitseLomakeosionPiilottaminenArvonPerusteella: () => {
    return haeLomakeosionPiilottaminenArvonPerusteella().click()
  },

  asetaLomakeosionPiilottaminenArvonPerusteellaVertailuarvo: (arvo: string) => {
    return haeLomakeosionPiilottaminenArvonPerusteellaVertailuarvo().select(
      arvo
    )
  },

  asetaLomakeosionPiilottaminenArvonPerusteellaOsio: (osio: string) => {
    return haeLomakeosionPiilottaminenArvonPerusteellaOsio().select(osio)
  },

  lisääVastausvaihtoehto: (arvo: string) => {
    return cy
      .get('.editor-form__add-dropdown-item a:contains("Lisää")')
      .click()
      .then(() =>
        cy
          .get('.editor-form__multi-options-container .editor-form__text-field')
          .last()
          .type(arvo)
      )
  },
}
