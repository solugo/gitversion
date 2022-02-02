import git.git_error_code
import git.git_error_last
import git.git_oid
import git.git_oid_tostr
import kotlinx.cinterop.*

fun git_error_code.handleGitError() {
    this.also { value ->
        if (value < 0) {
            git_error_last()?.also {
                throw RuntimeException(it.pointed.run { "$value/$klass: ${message?.toKString()}" })
            }
        }
    }
}

fun git_oid.toKStringFromUtf8() = memScoped {
    allocArray<ByteVar>(41).also { string ->
        git_oid_tostr(string, 41, ptr)
    }.toKStringFromUtf8()
}

val MatchResult.groupString; get() = groupValues.takeUnless { it.size == 1 }?.asSequence()?.drop(1)?.joinToString(prefix = "[", postfix = "]") ?: ""
