import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import me.archinamon.fileio.File
import me.archinamon.fileio.appendText
import platform.posix.getenv

fun main(args: Array<String>) {

    val parser = ArgParser("gitversion")
    val versionPattern by parser.option(ArgType.String, fullName = "version_pattern").default("(\\d+)(?:[.](\\d+)(?:[.](\\d+))?)?")
    val tagPattern by parser.option(type = ArgType.String, fullName = "tag_pattern").default("v(.+)")
    val majorPattern by parser.option(type = ArgType.String, fullName = "major_pattern")
    val minorPattern by parser.option(type = ArgType.String, fullName = "minor_pattern")
    val patchPattern by parser.option(type = ArgType.String, fullName = "patch_pattern").default(".+")
    val verbose by parser.option(type = ArgType.Boolean, shortName = "v", fullName = "verbose").default(false)
    val path by parser.argument(type = ArgType.String, fullName = "path").optional().default(".")
    val pipeline by parser.argument(type = ArgType.Boolean, fullName = "pipeline").optional().default(true)

    parser.parse(args)

    val versionRegex = versionPattern.toRegex(RegexOption.DOT_MATCHES_ALL)
    val tagRegex = tagPattern.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)
    val majorRegex = majorPattern?.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)
    val minorRegex = minorPattern?.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)
    val patchRegex = patchPattern.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)

    val git = Git(path)
    val tags = hashMapOf<String, MutableList<Git.Tag>>()
    val changes = arrayListOf<VersionChange>()

    git.consumeTags { tag ->
        tags.getOrPut(tag.commit, ::arrayListOf).add(tag)
        true
    }

    git.consumeCommits { commit ->
        tags[commit.id]?.forEach { tag ->
            tagRegex?.matchEntire(tag.name)?.takeIf { it.groupValues.size > 1 }?.also { match ->
                match.groupValues.getOrNull(1)?.let(versionRegex::matchEntire)?.also { versionMatch ->
                    changes.add(
                        VersionChange(tag) {
                            major += versionMatch.groupValues.getOrNull(1)?.toInt() ?: 0
                            minor += versionMatch.groupValues.getOrNull(2)?.toInt() ?: 0
                            patch += versionMatch.groupValues.getOrNull(3)?.toInt() ?: 0
                        }
                    )

                    return@consumeCommits false
                }
            }

        }

        majorRegex?.matchEntire(commit.message)?.also {
            changes.add(
                VersionChange(commit) {
                    major += 1
                }
            )
            return@consumeCommits true
        }

        minorRegex?.matchEntire(commit.message)?.also {
            changes.add(
                VersionChange(commit) {
                    minor += 1
                }
            )
            return@consumeCommits true
        }

        patchRegex?.matchEntire(commit.message)?.also {
            changes.add(
                VersionChange(commit) {
                    patch += 1
                }
            )
            return@consumeCommits true
        }

        changes.add(
            VersionChange(commit, null)
        )

        return@consumeCommits true
    }

    val version = Version()
    changes.reversed().forEach { change ->
        change.modification?.invoke(version)

        if (verbose) println("$version because of ${change.reason}")
    }

    if (pipeline) {
        getenv("GITHUB_ENV")?.toKStringFromUtf8()?.also {
            writeLinesToFile(it, "VERSION=$version", "GITHUB_RUN_NUMBER=$version")
        }
    }

    println(version)

}

private fun writeLinesToFile(file: String, vararg lines: String) {
    File(file).appendText(lines.joinToString(separator = "\n", postfix = "\n"))
}

data class VersionChange(val reason: Any, val modification: (Version.() -> Unit)?)

data class Version(var major: Int = 0, var minor: Int = 0, var patch: Int = 0) {
    override fun toString(): String = "$major.$minor.$patch"
}
