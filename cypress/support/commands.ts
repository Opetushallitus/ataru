import * as reitit from '../reitit'

declare global {
  // eslint-disable-next-line no-redeclare, @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      kirjauduVirkailijanNakymaan: (ticket?: string) => Chainable<Window>

      poistaLomake: (lomakkeenAvain: string) => Chainable<Response>

      avaaLomakeHakijanNakymassa: (
        lomakkeenAvain: string
      ) => Chainable<Cypress.WaitXHR>
      avaaLomakkeenHakemuksetVirkailijanNakymassa: (
        lomakkeenAvain: string
      ) => Chainable<Cypress.WaitXHR>

      avaaHaunHakemuksetVirkailijanNakymassa: (
        hakuOid: string
      ) => Chainable<Cypress.WaitXHR>
    }
  }
}

Cypress.Commands.add('kirjauduVirkailijanNakymaan', (ticket?: string) =>
  cy.visit(reitit.virkailija.haeVirkailijanNakymaanKirjautumisenOsoite(ticket))
)

Cypress.Commands.add('poistaLomake', (lomakkeenAvain: string) =>
  cy.request('DELETE', reitit.cypress.haeLomakkeenPoistamisenOsoite(), {
    formKey: lomakkeenAvain,
  })
)

Cypress.Commands.add('avaaLomakeHakijanNakymassa', (lomakkeenAvain: string) => {
  cy.server()
  cy.route('GET', reitit.hakija.haeLomakkeenHaunOsoite(lomakkeenAvain)).as(
    'getForm'
  )
  cy.visit(reitit.hakija.haeHakijanNakymanOsoite(lomakkeenAvain))
  return cy.wait('@getForm')
})

Cypress.Commands.add(
  'avaaLomakkeenHakemuksetVirkailijanNakymassa',
  (lomakkeenAvain: string) =>
    cy.visit(
      reitit.virkailija.haeLomakkeenHakemuksetVirkailijanNakymassaOsoite(
        lomakkeenAvain
      )
    )
)

Cypress.Commands.add(
  'avaaHaunHakemuksetVirkailijanNakymassa',
  (hakuOid: string) =>
    cy.visit(reitit.virkailija.haeHaunHakemusListausOsoite(hakuOid))
)
