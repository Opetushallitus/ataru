import Chainable = Cypress.Chainable
import WaitXHR = Cypress.WaitXHR

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
export function odotaHttpPyyntoa<T>(
  route: () => Chainable<unknown>,
  f: () => Chainable<T>
): Chainable<{ result: T; xhr: WaitXHR }> {
  cy.server()
  route().as('waitFor')
  return f().then((t) =>
    cy.wait('@waitFor').then((xhr) => ({ result: t, xhr }))
  )
}
