import { AssertionError } from 'chai'
import * as Option from 'fp-ts/lib/Option'

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
export function unsafeFoldOption<T>(o: Option.Option<T>): T {
  return Option.fold<T, T>(
    () => {
      throw new AssertionError('Option was None')
    },
    (val) => val
  )(o)
}
