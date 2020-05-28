export const setDropdownValue = (dataTestIdPrefix: string, value: string) =>
  cy
    .get(`[data-test-id=${dataTestIdPrefix}-button]`)
    .click()
    .then(() =>
      cy
        .get(`[data-test-id=${dataTestIdPrefix}-option-${value}]`)
        .click({ force: true })
    )
