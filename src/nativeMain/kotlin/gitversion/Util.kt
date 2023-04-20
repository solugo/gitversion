package gitversion

fun String?.str(prefix: String? = null): String = this?.let { "${prefix.str()}$it"} ?: ""
