import {FromOther} from "./another";
//       ^?/*<# (alias) type FromOther = { a: string; } import FromOther #>*/

type Hello = number;

type TooLong = "ABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJA_ABHIERWEG"
//   ^?/*<# type TooLong = "ABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJA... #>*/


type HelloJetbrains = Hello | string | FromOther;
//    ^?/*<# type HelloJetbrains = string | number | FromOther #>*/

//
const fun = () => 'Hi';
//    ^?/*<# const fun: () => string #>*/

const param = (b: Hello) => 'Hi';
//             ^?/*<# (parameter) b: number #>*/

type Dup = number;
//   ^?/*<# TS2300: Duplicate identifier 'Dup'. #>*/
type Dup = number;