package gitversion

val MatchResult.groupString; get() = groupValues.takeUnless { it.size == 1 }?.asSequence()?.drop(1)?.joinToString(prefix = "[", postfix = "]") ?: ""
