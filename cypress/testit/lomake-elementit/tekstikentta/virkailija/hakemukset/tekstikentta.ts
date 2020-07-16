export const tekstikentta = {
  tekstikentänLisäkysymyksetTekstikenttäVastaus: () =>
    cy.get(
      '[data-test-id=tekstikenttä-lisäkysymykset] [data-test-id=tekstikenttä-vastaus]'
    ),

  haeLisäkysymyksenVastaus: () =>
    tekstikentta.tekstikentänLisäkysymyksetTekstikenttäVastaus(),
}
