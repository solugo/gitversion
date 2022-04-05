package gitversion

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional

class Configuration(args: Array<String>) {

    val parser = ArgParser("gitversion")
    val versionPattern by parser.option(ArgType.String, fullName = "version_pattern").default("(\\d+)(?:[.](\\d+)(?:[.](\\d+))?)?")
    val tagPattern by parser.option(type = ArgType.String, fullName = "tag_pattern").default("v(.+)")
    val majorPattern by parser.option(type = ArgType.String, fullName = "major_pattern")
    val minorPattern by parser.option(type = ArgType.String, fullName = "minor_pattern")
    val patchPattern by parser.option(type = ArgType.String, fullName = "patch_pattern").default(".+")
    val verbose by parser.option(type = ArgType.Boolean, shortName = "v", fullName = "verbose").default(false)
    val path by parser.argument(type = ArgType.String, fullName = "path").optional().default(".")
    val pipeline by parser.argument(type = ArgType.Boolean, fullName = "pipeline").optional().default(true)

    init {
        parser.parse(args)
    }


}
