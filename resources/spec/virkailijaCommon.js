const formComponents = () => {
  return (
    testFrame()
      .find('.editor-form__component-wrapper')
      // exclude followup question components
      .not(
        '.editor-form__followup-question-overlay .editor-form__component-wrapper'
      )
      // exclude hakukohteet
      .not(
        (i, node) => $(node).find("header:contains('Hakukohteet')").length > 0
      )
      // exclude henkilötiedot
      .not(
        (i, node) => $(node).find("header:contains('Henkilötiedot')").length > 0
      )
      // exclude properties
      .not(
        (i, node) =>
          $(node).find("header:contains('Yleiset asetukset')").length > 0
      )
  )
}
