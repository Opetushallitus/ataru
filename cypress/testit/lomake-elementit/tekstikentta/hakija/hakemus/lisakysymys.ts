import * as tekstinSyotto from '../../../../../tekstinSyotto'

export const lisakysymys = {
  kysymysKenttä: () => cy.get('[data-test-id=tekstikenttä-input]'),
  lisäkysymyksetTekstikenttä: () =>
    cy.get(
      '[data-test-id=tekstikenttä-lisäkysymykset] [data-test-id=tekstikenttä-input]'
    ),

  syötäTekstikenttäänVastaus: (teksti: string) => {
    return tekstinSyotto.syotaTekstiTarkistamatta(
      lisakysymys.kysymysKenttä(),
      teksti
    )
  },

  syötäLisäkysymykseenVastaus: (teksti: string) => {
    return tekstinSyotto.syotaTekstiTarkistamatta(
      lisakysymys.lisäkysymyksetTekstikenttä(),
      teksti
    )
  },

  haeLisäkysymyksenVastaus: () => {
    return lisakysymys.lisäkysymyksetTekstikenttä()
  },
}
