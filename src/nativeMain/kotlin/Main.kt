import gitversion.Application
import gitversion.errOut
import gitversion.stdOut
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKStringFromUtf8
import platform.posix.exit
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    try {
        Application(out = ::stdOut, err = ::errOut, env = { getenv(it)?.toKStringFromUtf8() }).execute(args)
    } catch (ex: Application.ExecutionError) {
        println(ex.message)
        exit(ex.reason.status)
    }
}
