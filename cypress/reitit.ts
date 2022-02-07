export const virkailija = {
  haeLomakkeenEsikatseluOsoite: (lomakkeenAvain: string) =>
    `/lomake-editori/api/preview/form/${lomakkeenAvain}?lang=fi`,

  haeVirkailijanNakymaanKirjautumisenOsoite: (ticket?: string) =>
    `/lomake-editori/auth/cas?ticket=${ticket || 'DEVELOPER'}`,

  haeUudenLomakkeenLahettamisenOsoite: () => `/lomake-editori/api/forms`,

  haeLomakkeenMuuttamisenOsoite: (lomakkeenId: number) =>
    `/lomake-editori/api/forms/${lomakkeenId}`,

  haeLomakkeenHakemuksetVirkailijanNakymassaOsoite: (lomakkeenAvain: string) =>
    `/lomake-editori/applications/${lomakkeenAvain}?ensisijaisesti=false`,
}

export const hakija = {
  haeHakijanNakymanOsoite: (lomakkeenAvain: string) =>
    `/hakemus/${lomakkeenAvain}`,

  haeLomakkeenHaunOsoite: (lomakkeenAvain: string) =>
    `/hakemus/api/form/${lomakkeenAvain}?role=hakija`,

  haeHakemuksenLahettamisenOsoite: () => `/hakemus/api/application`,

  haeHakemuksenMuokkauksenOsoite: (salainenKoodi: string) =>
    `/hakemus?modify=${salainenKoodi}`,
}

export const cypress = {
  haeLomakkeenPoistamisenOsoite: () => `/lomake-editori/api/cypress/form`,
}
