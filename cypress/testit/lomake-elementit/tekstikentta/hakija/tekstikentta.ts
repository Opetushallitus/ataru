import * as tekstinSyotto from '../../../../tekstinSyotto'

export const tekstikentta = {
  kysymysKenttä: () => cy.get('[data-test-id=tekstikenttä-input]'),
  lisäkysymyksetTekstikenttä: () =>
    cy.get(
      '[data-test-id=tekstikenttä-lisäkysymykset] [data-test-id=tekstikenttä-input]'
    ),

  syötäTekstikenttäänVastaus: (teksti: string) => {
    return tekstinSyotto.syotaTekstiTarkistamatta(
      tekstikentta.kysymysKenttä(),
      teksti
    )
  },

  syötäLisäkysymykseenVastaus: (teksti: string) => {
    return tekstinSyotto.syotaTekstiTarkistamatta(
      tekstikentta.lisäkysymyksetTekstikenttä(),
      teksti
    )
  },

  haeLisäkysymyksenVastaus: () => {
    return tekstikentta.lisäkysymyksetTekstikenttä()
  },
}
