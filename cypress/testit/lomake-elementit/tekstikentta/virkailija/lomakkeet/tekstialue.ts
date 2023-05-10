import * as asetukset from '../../../../../asetukset'
import * as lomakkeenMuokkaus from '../../../../../lomakkeenMuokkaus'
import * as tekstinSyotto from '../../../../../tekstinSyotto'

export const tekstialue = {
  haeLisaaLinkki: () => cy.get('[data-test-id=component-toolbar-tekstialue]'),

  kysymysKenttä: () => cy.get('[data-test-id=tekstikenttä-kysymys]'),

  maksimiMerkkimaara: () => cy.get('[data-test-id=tekstialue-max-merkkimaara]'),

  haeTekstialue: () => cy.get('textarea'),

  lisaaTekstialue: (formId: number) =>
    lomakkeenMuokkaus.teeJaodotaLomakkeenTallennusta(formId, () => {
      lomakkeenMuokkaus.komponentinLisays.avaaValikko()
      return tekstialue.haeLisaaLinkki().click()
    }),

  asetaKysymys: (teksti: string) => {
    return tekstialue
      .kysymysKenttä()
      .type(teksti, { delay: asetukset.tekstikentanSyotonViive })
  },

  asetaMaxMerkkimaara: (teksti: string) => {
    return tekstialue
      .maksimiMerkkimaara()
      .type(teksti, { delay: asetukset.tekstikentanSyotonViive })
  },

  syötäTekstialueenVastaus: (teksti: string) => {
    return tekstinSyotto.syotaTekstiTarkistamatta(
      tekstialue.haeTekstialue(),
      teksti
    )
  },
}
