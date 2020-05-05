import * as routes from '../routes'

declare global {
  // eslint-disable-next-line no-redeclare
  namespace Cypress {
    interface Chainable {
      loginToVirkailija: () => Chainable<Window>

      deleteForm: (formKey: string) => Chainable<Response>

      loadHakija: (formKey: string) => Chainable<WaitXHR>
    }
  }
}

Cypress.Commands.add('loginToVirkailija', () =>
  cy.visit(routes.virkailija.getHakemuspalveluLoginUrl())
)

Cypress.Commands.add('deleteForm', (formKey: string) =>
  cy.request('DELETE', routes.virkailija.getDeleteFormUrl(), { formKey })
)

Cypress.Commands.add('loadHakija', (formKey: string) => {
  cy.server()
  cy.route('GET', routes.hakija.getFormUrl(formKey)).as('getForm')
  cy.visit(routes.hakija.getHakemuspalveluUrl(formKey))
  return cy.wait('@getForm')
})
