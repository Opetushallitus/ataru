import * as asetukset from '../../../../../asetukset'
import * as tekstinSyotto from '../../../../../tekstinSyotto'
import * as lomakkeenMuokkaus from '../../../../../lomakkeenMuokkaus'

export const tekstikentta = {
  haeLisaaLinkki: () => cy.get('[data-test-id=component-toolbar-tekstikenttä]'),

  kysymysKenttä: () => cy.get('[data-test-id=tekstikenttä-kysymys]'),

  lisäkysymysValinta: () =>
    cy.get('[data-test-id=tekstikenttä-valinta-lisäkysymys]'),

  lisäkysymyksenValintaToolbar: () =>
    cy.get(
      '[data-test-id=tekstikenttä-lisäkysymys-lista] [data-test-id=component-toolbar]'
    ),

  lisäkysymyksenValintaToolbarTekstikenttä: () =>
    cy.get(
      '[data-test-id=tekstikenttä-lisäkysymys-lista] [data-test-id=component-toolbar-tekstikenttä]'
    ),

  lisäkysymysLista: () =>
    cy.get('[data-test-id=tekstikenttä-lisäkysymys-lista]'),

  lisäkysymysListaTekstikenttäKysymys: () =>
    tekstikentta.lisäkysymysLista().find('[data-test-id=tekstikenttä-kysymys]'),

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

  valitseLisäkysymys: () => {
    return tekstikentta.lisäkysymysValinta().click()
  },

  avaaLisäkysymyksenValinta: () => {
    return tekstikentta.lisäkysymyksenValintaToolbar().trigger('mouseover')
  },

  valitseLisäkysymysTekstikenttä: () => {
    return tekstikentta.lisäkysymyksenValintaToolbarTekstikenttä().click()
  },

  asetaLisäkysymysTekstikenttäKysymys: (teksti: string) => {
    return tekstinSyotto.syotaTekstiTarkistamatta(
      tekstikentta.lisäkysymysListaTekstikenttäKysymys(),
      teksti
    )
  },

  lisääLisäkysymys: (teksti: string) => {
    return tekstikentta
      .avaaLisäkysymyksenValinta()
      .then(() => tekstikentta.valitseLisäkysymysTekstikenttä())
      .then(() => tekstikentta.asetaLisäkysymysTekstikenttäKysymys(teksti))
  },

  haeLisäkysymyksenKysymysteksti: () => {
    return tekstikentta.lisäkysymysListaTekstikenttäKysymys()
  },
}
