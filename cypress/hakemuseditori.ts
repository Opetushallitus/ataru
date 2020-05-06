import * as responses from './responses'
import * as routes from './routes'
import * as config from './config'
import * as wait from './wait'

export const getAddFormButton = () =>
  cy.get('[data-test-id=add-form-button]:visible')

export const addForm = () => {
  cy.server()
  cy.route('POST', routes.virkailija.getPostFormUrl()).as('postForms')
  getAddFormButton().click()
  return cy.wait('@postForms').then((response) => {
    return {
      formKey: responses.postFormResponse.getFormKey(response),
      formId: responses.postFormResponse.getFormId(response),
    }
  })
}

export const getFormNameInput = () =>
  cy.get('[data-test-id=form-name-input]:visible')

export const setFormName = (name: string, formId: number) =>
  wait.waitFor(
    () => cy.route('PUT', routes.virkailija.getPutFormUrl(formId)),
    () =>
      getFormNameInput().clear().type(name, { delay: config.textInputDelay })
  )

export const getPreviewLink = () =>
  cy.get('[data-test-id=application-preview-link-fi]:visible')

export const hakukohteet = {
  getHeaderLabel: () =>
    cy.get('[data-test-id=hakukohteet-header-label]:visible'),
}

export const henkilotiedot = {
  getHeaderLabel: () =>
    cy.get('[data-test-id=henkilotietomoduuli-header-label]:visible'),

  getSelectComponent: () =>
    cy.get('[data-test-id=henkilotietomoduuli-select]:visible'),

  getFieldsLabel: () =>
    cy.get('[data-test-id=henkilotietomoduuli-fields-label]:visible'),

  selectOption: (label: string, formId: number) =>
    wait.waitFor(
      () => cy.route('PUT', routes.virkailija.getPutFormUrl(formId)),
      () => henkilotiedot.getSelectComponent().select(label)
    ),
}
