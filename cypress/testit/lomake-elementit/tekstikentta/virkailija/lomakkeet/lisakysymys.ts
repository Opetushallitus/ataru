import * as tekstinSyotto from '../../../../../tekstinSyotto'

export const lisakysymys = {
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
    lisakysymys.lisäkysymysLista().find('[data-test-id=tekstikenttä-kysymys]'),

  valitseLisäkysymys: () => {
    return lisakysymys.lisäkysymysValinta().click()
  },

  avaaLisäkysymyksenValinta: () => {
    return lisakysymys.lisäkysymyksenValintaToolbar().trigger('mouseover')
  },

  valitseLisäkysymysTekstikenttä: () => {
    return lisakysymys.lisäkysymyksenValintaToolbarTekstikenttä().click()
  },

  asetaLisäkysymysTekstikenttäKysymys: (teksti: string) => {
    return tekstinSyotto.syotaTekstiTarkistamatta(
      lisakysymys.lisäkysymysListaTekstikenttäKysymys(),
      teksti
    )
  },

  lisääLisäkysymys: (teksti: string) => {
    return lisakysymys
      .avaaLisäkysymyksenValinta()
      .then(() => lisakysymys.valitseLisäkysymysTekstikenttä())
      .then(() => lisakysymys.asetaLisäkysymysTekstikenttäKysymys(teksti))
  },

  haeLisäkysymyksenKysymysteksti: () => {
    return lisakysymys.lisäkysymysListaTekstikenttäKysymys()
  },
}
