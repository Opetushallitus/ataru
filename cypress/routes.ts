export const virkailija = {
  getFormPreviewUrl: (formKey: string) =>
    `/lomake-editori/api/preview/form/${formKey}?lang=fi`,

  getHakemuspalveluLoginUrl: () => `/lomake-editori/auth/cas?ticket=DEVELOPER`,

  getPostFormUrl: () => `/lomake-editori/api/forms`,

  getDeleteFormUrl: () => `/lomake-editori/api/cypress/form`,

  getPutFormUrl: (formId: number) => `/lomake-editori/api/forms/${formId}`,
}

export const hakija = {
  getHakemuspalveluUrl: (formKey: string) => `/hakemus/${formKey}`,

  getFormUrl: (formKey: string) => `/hakemus/api/form/${formKey}?role=hakija`,
}
