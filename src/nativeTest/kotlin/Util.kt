import gitversion.Application
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.chdir
import platform.posix.system
import kotlin.random.Random

private val root = FileSystem.SYSTEM.canonicalize(".".toPath())

fun randomString(length: Int = 20) = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".let { chars ->
    (0 until length).map { chars[Random.nextInt(0, chars.length)] }.joinToString("")
}

fun withTemporaryGit(create: Boolean = true, block: (String) -> Unit) {
    val parent = root.resolve("build/git")
    val path = parent.resolve("git-test-${randomString()}")

    try {
        println("Creating temporary git repository at $path")

        FileSystem.SYSTEM.createDirectories(path, false)

        chdir(path.toString()) == 0 || error("Could not change director to $path")

        if (create) {
            system("git init -q .")
            system("git config user.email test@solugo.de")
            system("git config user.name 'Test User'")
        }

        block(path.toString())
    } finally {
        chdir(root.toString())
    }
}

fun createDirectory(path: String) {
    FileSystem.SYSTEM.createDirectories("./$path".toPath(true), false)
}

fun commit(message: String) {
    system("git add .")
    system("git commit -q --allow-empty -m \"$message\"")
}

fun process(vararg args: String, env: Map<String, String> = emptyMap()): String {
    val out = StringBuilder()
    Application(env = env::get, out = out::appendLine).execute(emptyArray<String>() + args)
    return out.toString().trim()
}