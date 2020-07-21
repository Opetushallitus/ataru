import * as asetukset from '../../../../../asetukset'
import * as lomakkeenMuokkaus from '../../../../../lomakkeenMuokkaus'

export const tekstikentta = {
  haeLisaaLinkki: () => cy.get('[data-test-id=component-toolbar-tekstikenttä]'),

  kysymysKenttä: () => cy.get('[data-test-id=tekstikenttä-kysymys]'),

  kenttäänVainNumeroitaValinta: () =>
    cy.get('[data-test-id=tekstikenttä-valinta-kenttään-vain-numeroita]'),

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
}
