package gitversion

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
private val STDOUT = platform.posix.fdopen(1, "w")

@OptIn(ExperimentalForeignApi::class)
private val ERROUT = platform.posix.fdopen(2, "w")

@OptIn(ExperimentalForeignApi::class)
fun stdOut(message: Any?) {
    platform.posix.fprintf(STDOUT, "$message\n")
}

@OptIn(ExperimentalForeignApi::class)
fun errOut(message: Any?) {
    platform.posix.fprintf(ERROUT, "$message\n")
}
