import { teeJaodotaLomakkeenTallennusta } from './lomakkeenMuokkaus'

export const haeOsionNimi = () =>
  cy.get('[data-test-id=arvosanat-moduuli-header-label]:visible')

export const haePoistaOsioNappi = () =>
  cy.get(
    '[data-test-id=arvosanat-moduuli-header-remove-component-button]:visible'
  )

export const haeVahvistaPoistaOsioNappi = () =>
  cy.get(
    '[data-test-id=arvosanat-moduuli-header-remove-component-button-confirm]:visible'
  )

export const haeLeikkaaOsioNappi = () =>
  cy.get('[data-test-id=arvosanat-moduuli-header-cut-component-button]:visible')

export const poistaArvosanat = (lomakkeenId: number) =>
  haePoistaOsioNappi()
    .click()
    .then(() =>
      teeJaodotaLomakkeenTallennusta(lomakkeenId, () =>
        haeVahvistaPoistaOsioNappi().click()
      )
    )
