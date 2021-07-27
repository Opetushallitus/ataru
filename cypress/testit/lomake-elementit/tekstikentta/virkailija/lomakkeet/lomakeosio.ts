import * as lomakkeenMuokkaus from '../../../../../lomakkeenMuokkaus'

export const lomakeosio = {
  haeLisaaLinkki: () => cy.get('[data-test-id=component-toolbar-lomakeosio]'),

  lisaaLomakeosio: (formId: number) =>
    lomakkeenMuokkaus.teeJaodotaLomakkeenTallennusta(formId, () => {
      lomakkeenMuokkaus.komponentinLisays.avaaValikko()
      return lomakeosio.haeLisaaLinkki().click()
    }),
}
