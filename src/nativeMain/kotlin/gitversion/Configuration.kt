package gitversion

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional

class Configuration(args: Array<String>) {

    companion object {
        private const val defaultVersionPattern = "(\\d+)(?:[.](\\d+)(?:[.](\\d+))?)?"
        private const val defaultTagPattern = "v(.+)"
        private const val defaultPatchPattern = ".+"
        private const val defaultPath = "."
    }

    val parser = ArgParser("gitversion")
    val versionPattern by parser.option(ArgType.String, fullName = "version_pattern").default(defaultVersionPattern)
    val tagPattern by parser.option(type = ArgType.String, fullName = "tag_pattern").default(defaultTagPattern)
    val majorPattern by parser.option(type = ArgType.String, fullName = "major_pattern")
    val minorPattern by parser.option(type = ArgType.String, fullName = "minor_pattern")
    val patchPattern by parser.option(type = ArgType.String, fullName = "patch_pattern").default(defaultPatchPattern)
    val verbose by parser.option(type = ArgType.Boolean, shortName = "v", fullName = "verbose").default(false)
    val debug by parser.option(type = ArgType.Boolean, fullName = "debug").default(false)
    val directory by parser.option(type = ArgType.String, shortName = "d", fullName = "directory")
    val path by parser.argument(type = ArgType.String, fullName = "path").optional().default(defaultPath)
    val pipeline by parser.option(type = ArgType.Boolean, fullName = "pipeline").default(true)

    init {
        parser.parse(args)
    }


}
