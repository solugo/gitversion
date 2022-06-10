import gitversion.Application
import kotlinx.cinterop.toKStringFromUtf8
import platform.posix.getenv

fun main(args: Array<String>) {
    Application(out = ::println, env = { getenv(it)?.toKStringFromUtf8() }).execute(args)
}


