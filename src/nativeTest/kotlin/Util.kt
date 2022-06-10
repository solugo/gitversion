import kotlinx.cinterop.*
import platform.posix.*

fun withTemporaryGit(block: (String) -> Unit) {
    val current = memScoped {
        val size: size_t = 10000U
        allocArray<ByteVar>(size.toInt()).also { getcwd(it, size) }.toKStringFromUtf8()
    }
    val parent = "$current/build/git"

    mkdir(parent, S_IRWXU)

    val path = mkdtemp("$parent/git-test-XXXXXX".cstr)?.toKStringFromUtf8() ?: error(
        "Could not create temprary directory"
    )

    try {
        println("Creating temporary git repository at $path")

        chdir(path)

        system("git init -q")
        system("git config user.email test@solugo.de")
        system("git config user.name 'Test User'")

        block(path)
    } finally {
        chdir(current)
    }
}

fun commit(message: String) {
    system("git add .")
    system("git commit -q --allow-empty -m '$message'")
}