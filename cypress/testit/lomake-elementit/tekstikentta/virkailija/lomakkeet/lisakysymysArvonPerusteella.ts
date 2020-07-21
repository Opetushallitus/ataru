import * as tekstinSyotto from '../../../../../tekstinSyotto'

export const lisakysymysArvonPerusteella = {
  lisäkysymysArvonPerusteellaValinta: () =>
    cy.get('[data-test-id=tekstikenttä-valinta-lisäkysymys-arvon-perusteella]'),

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
    lisakysymysArvonPerusteella
      .lisäkysymysLista()
      .find('[data-test-id=tekstikenttä-kysymys]'),

  lisäkysymysListaLisäkysymyksenEhtoOperaattori: () =>
    lisakysymysArvonPerusteella
      .lisäkysymysLista()
      .find(
        '[data-test-id=tekstikenttä-lisäkysymys-arvon-perusteella-ehto-operaattori]'
      ),

  lisäkysymysListaLisäkysymyksenEhtoVertailuarvo: () =>
    lisakysymysArvonPerusteella
      .lisäkysymysLista()
      .find(
        '[data-test-id=tekstikenttä-lisäkysymys-arvon-perusteella-ehto-vertailuarvo]'
      ),

  lisäkysymysListaLisäkysymyksenEhtoLisäkysymykset: () =>
    lisakysymysArvonPerusteella
      .lisäkysymysLista()
      .find('[data-test-id=followup-question-followups]'),

  avaaLisäkysymyksenValinta: () => {
    return lisakysymysArvonPerusteella
      .lisäkysymyksenValintaToolbar()
      .trigger('mouseover')
  },

  valitseLisäkysymysTekstikenttä: () => {
    return lisakysymysArvonPerusteella
      .lisäkysymyksenValintaToolbarTekstikenttä()
      .click()
  },

  asetaLisäkysymysTekstikenttäKysymys: (teksti: string) => {
    return tekstinSyotto.syotaTekstiTarkistamatta(
      lisakysymysArvonPerusteella.lisäkysymysListaTekstikenttäKysymys(),
      teksti
    )
  },

  lisääLisäkysymys: (teksti: string) => {
    return lisakysymysArvonPerusteella
      .avaaLisäkysymyksenValinta()
      .then(() => lisakysymysArvonPerusteella.valitseLisäkysymysTekstikenttä())
      .then(() =>
        lisakysymysArvonPerusteella.asetaLisäkysymysTekstikenttäKysymys(teksti)
      )
  },

  haeLisäkysymyksenKysymysteksti: () => {
    return lisakysymysArvonPerusteella.lisäkysymysListaTekstikenttäKysymys()
  },

  valitseLisäkysymysArvonPerusteella: () => {
    return lisakysymysArvonPerusteella
      .lisäkysymysArvonPerusteellaValinta()
      .click()
  },

  asetaLisäkysymysArvonPerusteellaEhto: (
    operaattori: string,
    vertailuarvo: number
  ) => {
    lisakysymysArvonPerusteella
      .lisäkysymysListaLisäkysymyksenEhtoOperaattori()
      .select(operaattori)
    lisakysymysArvonPerusteella
      .lisäkysymysListaLisäkysymyksenEhtoVertailuarvo()
      .type(`${vertailuarvo}`)
  },

  avaaLisäkysymysArvonPerusteellaEhdonLisäkysymykset: () => {
    return lisakysymysArvonPerusteella
      .lisäkysymysListaLisäkysymyksenEhtoLisäkysymykset()
      .click()
  },
}
