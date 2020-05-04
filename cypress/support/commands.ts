import * as routes from '../routes'

declare global {
  // eslint-disable-next-line no-redeclare
  namespace Cypress {
    interface Chainable {
      loginToVirkailija: () => Chainable<Window>

      deleteForm: (formKey: string) => Chainable<Response>
    }
  }
}

Cypress.Commands.add('loginToVirkailija', () =>
  cy.visit(routes.virkailija.getHakemuspalveluLoginUrl())
)

Cypress.Commands.add('deleteForm', (formKey: string) =>
  cy.request('DELETE', routes.virkailija.getDeleteFormUrl(), { formKey })
)
