package gitversion

import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

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


fun Path.readLines(): List<String>? = when {
    FileSystem.SYSTEM.exists(this@readLines) -> buildList {
        FileSystem.SYSTEM.read(this@readLines) {
            while (true) {
                val line = readUtf8Line() ?: break
                add(line)
            }
        }
    }

    else -> null
}

fun Path.writeLines(lines: List<String>) {
    FileSystem.SYSTEM.appendingSink(this, mustExist = false).buffer().use {
        lines.forEach { line ->
            it.writeUtf8(line)
            it.writeUtf8("\n")
        }
    }
}