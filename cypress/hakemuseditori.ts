import * as responses from './responses'
import * as routes from './routes'
import * as config from './config'

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

export const setFormName = (name: string, formId: number) => {
  cy.server()
  cy.route('PUT', routes.virkailija.getPutFormUrl(formId)).as('putForm')
  getFormNameInput().clear().type(name, { delay: config.textInputDelay })
  cy.wait('@putForm')
}

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

  selectOption: (label: string, formId: number) => {
    cy.server()
    cy.route('PUT', routes.virkailija.getPutFormUrl(formId)).as('putForm')
    henkilotiedot.getSelectComponent().select(label)
    cy.wait('@putForm')
  },
}
