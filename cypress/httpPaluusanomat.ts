import * as Option from 'fp-ts/lib/Option'

import WaitXHR = Cypress.WaitXHR

export const lomakkeenLahetyksenPaluusanoma = {
  haeLomakkeenAvain: ({
    response: { body },
  }: WaitXHR): Option.Option<string> =>
    typeof body === 'object' && 'key' in body
      ? Option.some(body.key)
      : Option.none,

  haeLomakkeenId: ({
    response: { body },
  }: Cypress.WaitXHR): Option.Option<number> =>
    typeof body === 'object' && 'id' in body
      ? Option.some(body.id)
      : Option.none,
}
