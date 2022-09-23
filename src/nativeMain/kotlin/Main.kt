import gitversion.Application
import kotlinx.cinterop.toKStringFromUtf8
import platform.posix.exit
import platform.posix.getenv

fun main(args: Array<String>) {
    try {
        Application(out = ::println, env = { getenv(it)?.toKStringFromUtf8() }).execute(args)
    } catch (ex: Application.ExecutionError) {
        println(ex.message)
        exit(ex.reason.status)
    }
}


