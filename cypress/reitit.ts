export const virkailija = {
  haeLomakkeenEsikatseluOsoite: (formKey: string) =>
    `/lomake-editori/api/preview/form/${formKey}?lang=fi`,

  haeHakemuksenMuokkauksenKirjautumisenOsoite: () =>
    `/lomake-editori/auth/cas?ticket=DEVELOPER`,

  haeUudenLomakkeenLahettamisenOsoite: () => `/lomake-editori/api/forms`,

  haeLomakkeenMuuttamisenOsoite: (formId: number) =>
    `/lomake-editori/api/forms/${formId}`,
}

export const hakija = {
  haeHakijanNakymanOsoite: (formKey: string) => `/hakemus/${formKey}`,

  haeLomakkeenHaunOsoite: (formKey: string) =>
    `/hakemus/api/form/${formKey}?role=hakija`,
}

export const cypress = {
  haeLomakkeenPoistamisenOsoite: () => `/lomake-editori/api/cypress/form`,
}
