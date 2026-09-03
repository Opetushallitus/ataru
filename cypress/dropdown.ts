export const setDropdownValue = (dataTestIdPrefix: string, value: string) =>
  cy
    .get(`[data-test-id=${dataTestIdPrefix}-button]`)
    .click()
    .then(() =>
      cy
        .get(`[data-value="${value}"]`)
        .click({ force: true })
    )

export const asetaHakijanNakymanPudotusvalikonArvo = (
  dataTestId: string,
  value: string
) => cy.get(`[data-test-id=${dataTestId}]`).select(value)
