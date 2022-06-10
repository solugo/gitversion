package gitversion

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional

class Configuration(args: Array<String>) {

    val parser = ArgParser("gitversion")

    val versionPattern by parser.option(
        description = "version regex pattern",
        type = ArgType.String,
        shortName = "vp",
        fullName = "version_pattern",
    ).default("(\\d+)(?:[.](\\d+)(?:[.](\\d+))?)?")

    val tagPattern by parser.option(
        description = "tag regex pattern",
        type = ArgType.String,
        shortName = "tp",
        fullName = "tag_pattern",
    ).default("v(.+)")

    val majorPattern by parser.option(
        description = "message major version regex pattern",
        type = ArgType.String,
        fullName = "major_pattern",
    )

    val minorPattern by parser.option(
        description = "message minor version regex pattern",
        type = ArgType.String,
        fullName = "minor_pattern",
    )

    val patchPattern by parser.option(
        description = "message patch version regex pattern",
        type = ArgType.String,
        fullName = "patch_pattern",
    ).default(".+")

    val verbose1 by parser.option(
        description = "verbosity level 1",
        type = ArgType.Boolean,
        shortName = "v",
    ).default(false)

    val verbose2 by parser.option(
        description = "verbosity level 2",
        type = ArgType.Boolean,
        shortName = "vv",
    ).default(false)

    val stacktrace by parser.option(
        description = "display stacktrace on error",
        type = ArgType.Boolean,
        shortName = "s",
        fullName = "stacktrace",
    ).default(false)

    val directory by parser.option(
        description = "only consider commits with changes in the given directory",
        type = ArgType.String,
        shortName = "d",
        fullName = "directory",
    )

    val path by parser.argument(
        description = "path to repository",
        type = ArgType.String,
        fullName = "path",
    ).optional().default(".")

    val pipeline by parser.option(
        description = "apply build pipeline changes",
        type = ArgType.String,
        fullName = "pipeline",
    ).default("auto")

    init {
        parser.parse(args)
    }


}
