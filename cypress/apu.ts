import * as asetukset from './asetukset'

import Chainable = Cypress.Chainable

export const syotaTeksti = (
  elementti: Chainable<unknown>,
  teksti: string
): Chainable<unknown> =>
  elementti.clear().type(teksti, { delay: asetukset.tekstikentanSyotonViive })
