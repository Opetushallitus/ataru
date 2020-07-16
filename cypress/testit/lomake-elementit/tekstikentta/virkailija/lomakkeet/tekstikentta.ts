import * as asetukset from '../../../../../asetukset'
import * as tekstinSyotto from '../../../../../tekstinSyotto'
import * as lomakkeenMuokkaus from '../../../../../lomakkeenMuokkaus'

export const tekstikentta = {
  haeLisaaLinkki: () => cy.get('[data-test-id=component-toolbar-tekstikenttä]'),

  kysymysKenttä: () => cy.get('[data-test-id=tekstikenttä-kysymys]'),

  lisäkysymysValinta: () =>
    cy.get('[data-test-id=tekstikenttä-valinta-lisäkysymys]'),

  kenttäänVainNumeroitaValinta: () =>
    cy.get('[data-test-id=tekstikenttä-valinta-kenttään-vain-numeroita]'),

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
    tekstikentta.lisäkysymysLista().find('[data-test-id=tekstikenttä-kysymys]'),

  lisäkysymysListaLisäkysymyksenEhtoOperaattori: () =>
    tekstikentta
      .lisäkysymysLista()
      .find(
        '[data-test-id=tekstikenttä-lisäkysymys-arvon-perusteella-ehto-operaattori]'
      ),

  lisäkysymysListaLisäkysymyksenEhtoVertailuarvo: () =>
    tekstikentta
      .lisäkysymysLista()
      .find(
        '[data-test-id=tekstikenttä-lisäkysymys-arvon-perusteella-ehto-vertailuarvo]'
      ),

  lisäkysymysListaLisäkysymyksenEhtoLisäkysymykset: () =>
    tekstikentta
      .lisäkysymysLista()
      .find('[data-test-id=followup-question-followups]'),

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

  valitseKenttäänVainNumeroita: () => {
    return tekstikentta.kenttäänVainNumeroitaValinta().click()
  },

  valitseLisäkysymysArvonPerusteella: () => {
    return tekstikentta.lisäkysymysArvonPerusteellaValinta().click()
  },

  asetaLisäkysymysArvonPerusteellaEhto: (
    operaattori: string,
    vertailuarvo: number
  ) => {
    tekstikentta
      .lisäkysymysListaLisäkysymyksenEhtoOperaattori()
      .select(operaattori)
    tekstikentta
      .lisäkysymysListaLisäkysymyksenEhtoVertailuarvo()
      .type(`${vertailuarvo}`)
  },

  avaaLisäkysymysArvonPerusteellaEhdonLisäkysymykset: () => {
    return tekstikentta
      .lisäkysymysListaLisäkysymyksenEhtoLisäkysymykset()
      .click()
  },
}
