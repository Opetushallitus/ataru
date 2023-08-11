import * as asetukset from '../../../../../asetukset'
import * as lomakkeenMuokkaus from '../../../../../lomakkeenMuokkaus'

export const tekstikentta = {
  haeLisaaLinkki: () => cy.get('[data-test-id=component-toolbar-tekstikenttä]'),

  kysymysKenttä: () => cy.get('[data-test-id=tekstikenttä-kysymys]'),

  kenttäänVainNumeroitaValinta: () =>
    cy.get('[data-test-id=tekstikenttä-valinta-kenttään-vain-numeroita]'),

  voiLisätäUseitaValinta: () =>
    cy.get('[data-test-id=tekstikenttä-valinta-voi-lisätä-useita]'),

  nakyvyysLomakkeellaValinta: () =>
    cy.get(`button.belongs-to-hakukohteet__modal-toggle`),

  nakyvyysLomakkeellaLabel: () =>
    cy.get(`.belongs-to-hakukohteet__modal-toggle-label`),

  eiNaytetaLomakkeellaValinta: () =>
    cy.get('.hakukohde-and-hakukohderyhma-visibility-checkbox input'),

  lisaaTekstikentta: (formId: number) =>
    lomakkeenMuokkaus.teeJaodotaLomakkeenTallennusta(formId, () => {
      lomakkeenMuokkaus.komponentinLisays.avaaValikko()
      return tekstikentta.haeLisaaLinkki().click()
    }),

  asetaKysymys: (teksti: string) => {
    return tekstikentta
      .kysymysKenttä()
      .type(teksti, { delay: asetukset.tekstikentanSyotonViive })
  },

  valitseKenttäänVainNumeroita: () => {
    return tekstikentta.kenttäänVainNumeroitaValinta().click()
  },

  naytaTekstiKentta: () => {
    return tekstikentta
      .nakyvyysLomakkeellaValinta()
      .click()
      .then(() => tekstikentta.eiNaytetaLomakkeellaValinta().click())
  },
}
