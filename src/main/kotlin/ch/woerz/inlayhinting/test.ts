

type Tuple<L, T, A extends T[] = []> = L extends A['length'] ? A : Tuple<L, T, [...A, T]>

type A = Tuple<3, number>;
//   ^?

type U = boolean;

type Test = U | string;

type T = number | Test;
//   ^?
