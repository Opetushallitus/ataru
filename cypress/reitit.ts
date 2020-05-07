export const virkailija = {
  haeLomakkeenEsikatseluOsoite: (lomakkeenAvain: string) =>
    `/lomake-editori/api/preview/form/${lomakkeenAvain}?lang=fi`,

  haeVirkailijanNakymaanKirjautumisenOsoite: () =>
    `/lomake-editori/auth/cas?ticket=DEVELOPER`,

  haeUudenLomakkeenLahettamisenOsoite: () => `/lomake-editori/api/forms`,

  haeLomakkeenMuuttamisenOsoite: (lomakkeenId: number) =>
    `/lomake-editori/api/forms/${lomakkeenId}`,
}

export const hakija = {
  haeHakijanNakymanOsoite: (lomakkeenAvain: string) =>
    `/hakemus/${lomakkeenAvain}`,

  haeLomakkeenHaunOsoite: (lomakkeenAvain: string) =>
    `/hakemus/api/form/${lomakkeenAvain}?role=hakija`,
}

export const cypress = {
  haeLomakkeenPoistamisenOsoite: () => `/lomake-editori/api/cypress/form`,
}
