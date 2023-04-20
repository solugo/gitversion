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

    val component by parser.option(
        description = "component name affecting directory and tag prefix",
        type = ArgType.String,
        shortName = "c",
        fullName = "component",
    )

    val path by parser.argument(
        description = "path to repository",
        type = ArgType.String,
        fullName = "path",
    ).optional().default(".")

    val pipeline by parser.option(
        description = "apply build pipeline changes",
        type = ArgType.Choice(listOf("auto", "none", "azure", "github", "gitlab"), { it }),
        fullName = "pipeline",
    ).default("auto")

    val pipelineGitlabDotenv by parser.option(
        description = "gitlab dotenv file (see https://docs.gitlab.com/ee/ci/environments/index.html#set-dynamic-environment-urls-after-a-job-finishes)",
        type = ArgType.String,
        fullName = "pipeline-gitlab-dotenv",
    ).default("build.env")

    val dirtyIgnore by parser.option(
        description = "ignore version change on dirty working tree",
        type = ArgType.Boolean,
        fullName = "dirty_ignore",
    ).default(false)

    val dirtySuffix by parser.option(
        description = "apply version suffix on dirty working tree",
        type = ArgType.String,
        fullName = "dirty_suffix",
    ).default("SNAPSHOT")

    val majorOverride by parser.option(
        description = "override version major value",
        type = ArgType.Int,
        fullName = "major_override",
    )

    val minorOverride by parser.option(
        description = "override version minor value",
        type = ArgType.Int,
        fullName = "minor_override",
    )

    val patchOverride by parser.option(
        description = "override version patch value",
        type = ArgType.Int,
        fullName = "patch_override",
    )

    val suffixOverride by parser.option(
        description = "override version suffix value",
        type = ArgType.String,
        fullName = "suffix_override",
    )

    val appendHash by parser.option(
        description = "append hash to version",
        type = ArgType.Boolean,
        fullName = "append_hash",
    ).default(false)

    init {
        parser.parse(args)
    }


}
