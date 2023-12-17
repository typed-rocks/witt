val input = "{   a: string; }"

val res = input.replace(Regex("\\{( {2,})"), "\\{ ")

println(res)