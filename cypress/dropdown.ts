export const setDropdownValue = (dataTestIdPrefix: string, value: string) =>
  cy
    .get(`[data-test-id=${dataTestIdPrefix}]`)
    .click()
    .then(() =>
      cy
        .get(`[data-value="${value}"]`)
        .click({ force: true })
    )
