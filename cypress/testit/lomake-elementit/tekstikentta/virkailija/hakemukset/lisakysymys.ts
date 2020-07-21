export const lisakysymys = {
  tekstikentänLisäkysymyksetTekstikenttäVastaus: () =>
    cy.get(
      '[data-test-id=tekstikenttä-lisäkysymykset] [data-test-id=tekstikenttä-vastaus]'
    ),

  haeLisäkysymyksenVastaus: () =>
    lisakysymys.tekstikentänLisäkysymyksetTekstikenttäVastaus(),
}
