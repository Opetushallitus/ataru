export const setDropdownValue = (dataTestIdPrefix: string, value: string) =>
  cy
    .get(`[data-test-id=${dataTestIdPrefix}-button]:visible`)
    .click()
    .then(() =>
      cy
        .get(`[data-test-id=${dataTestIdPrefix}-option-${value}]:visible`)
        .click({ force: true })
    )
    .then(() =>
      cy
        .get(`[data-test-id=${dataTestIdPrefix}-list]:visible`)
        .should('not.exist')
    )
