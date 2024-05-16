import assertk.assertThat
import assertk.assertions.*
import gitversion.Application
import gitversion.readLines
import okio.Path.Companion.toPath
import platform.posix.system
import kotlin.test.Test
import kotlin.test.fail

class ApplicationTest {

    @Test
    fun `process default`() = withTemporaryGit {
        commit("commit 1")
        commit("commit 2")
        system("git tag v0.1.0")
        commit("commit 3")
        commit("commit 4")
        system("git tag v1.0.0")
        commit("commit 5")

        process().apply {
            assertThat(out).isEqualTo("1.0.1")
        }
    }

    @Test
    fun `process with rc file`() = withTemporaryGit {
        commit("commit 1")
        commit("commit 2")
        system("git tag v0.1.0")
        commit("commit 3")
        commit("commit 4")
        system("git tag v1.0.0")
        commit("commit 5")

        system("echo dummy >> changed_file.txt")
        system("echo dirty_suffix=dirty >> .gitversionrc")
        system("echo #dirty_suffix=other >> .gitversionrc")

        process().apply {
            assertThat(out).isEqualTo("1.0.2-dirty")
        }
    }

    @Test
    fun `process with tag patterns`() = withTemporaryGit {
        commit("commit 1")
        system("git tag test1-v0.1.0")
        commit("commit 2")
        system("git tag test2-v0.2.0")
        commit("commit 3")
        system("git tag test1-v1.0.0")
        commit("commit 4")
        system("git tag test2-v2.0.0")
        commit("commit 5")

        process("-tp", "test1-v(.+)").apply {
            assertThat(out).isEqualTo("1.0.2")
        }
        process("-tp", "test2-v(.+)").apply {
            assertThat(out).isEqualTo("2.0.1")
        }
    }

    @Test
    fun `process with major message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        process("--major_pattern", "release.+").apply {
            assertThat(out).isEqualTo("2.0.1")
        }
    }

    @Test
    fun `process with minor message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        process("--minor_pattern", "release.+").apply {
            assertThat(out).isEqualTo("0.2.1")
        }
    }

    @Test
    fun `process with patch message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        process("--patch_pattern", "release.+").apply {
            assertThat(out).isEqualTo("0.0.2")
        }
    }

    @Test
    fun `process with directory`() = withTemporaryGit {
        commit("commit 1")
        createDirectory("component")
        system("echo \"value\" > component/file.txt")
        commit("commit 2")
        commit("commit 3")

        process("-d", "component").apply {
            assertThat(out).isEqualTo("0.0.1")
        }
    }

    @Test
    fun `process with component`() = withTemporaryGit {
        commit("commit 1")
        system("git tag v0.1.0")
        commit("commit 2")
        system("git tag component-v1.0.0")
        createDirectory("component")
        system("echo \"value\" > component/file.txt")
        commit("commit 3")

        process("-c", "component").apply {
            assertThat(out).isEqualTo("1.0.1")
        }
    }

    @Test
    fun `process with component and directory and tag prefix`() = withTemporaryGit {
        commit("commit 1")
        system("git tag v0.1.0")
        commit("commit 2")
        createDirectory("component")
        system("echo \"value\" > component/file.txt")
        commit("commit 3")
        system("git tag component-x1.0.0")
        createDirectory("component/sub")
        system("echo \"value\" > component/sub/file.txt")
        commit("commit 4")

        process("-c", "component", "-d", "sub", "-tp", "x(.+)").apply {
            assertThat(out).isEqualTo("1.0.1")
        }
    }

    @Test
    fun `process with uncommited changes and default suffix`() = withTemporaryGit {
        commit("commit 1")
        system("echo \"value\" > file.txt")

        process().apply {
            assertThat(out).isEqualTo("0.0.2-SNAPSHOT")
        }
    }

    @Test
    fun `process with uncommited changes and custom suffix`() = withTemporaryGit {
        commit("commit 1")
        system("echo \"value\" > file.txt")

        process("--dirty_suffix", "CUSTOM").apply {
            assertThat(out).isEqualTo("0.0.2-CUSTOM")
        }
    }

    @Test
    fun `process with uncommited changes and without suffix`() = withTemporaryGit {
        commit("commit 1")
        system("echo \"value\" > file.txt")

        process("--dirty_suffix", "").apply {
            assertThat(out).isEqualTo("0.0.2")
        }
    }

    @Test
    fun `process with uncommited changes and deactivated dirty handling`() = withTemporaryGit {
        commit("commit 1")
        system("echo \"value\" > file.txt")

        process("--dirty_ignore").apply {
            assertThat(out).isEqualTo("0.0.1")
        }
    }

    @Test
    fun `process with major override`() = withTemporaryGit {
        commit("commit 1")

        process("--major_override", "2").apply {
            assertThat(out).isEqualTo("2.0.1")
        }
    }

    @Test
    fun `process with minor override`() = withTemporaryGit {
        commit("commit 1")
        process("--minor_override", "2").apply {
            assertThat(out).isEqualTo("0.2.1")
        }
    }

    @Test
    fun `process with patch override`() = withTemporaryGit {
        commit("commit 1")

        process("--patch_override", "2").apply {
            assertThat(out).isEqualTo("0.0.2")
        }
    }

    @Test
    fun `process with suffix override`() = withTemporaryGit {
        commit("commit 1")

        process("--suffix_override", "CUSTOM").apply {
            assertThat(out).isEqualTo("0.0.1-CUSTOM")
        }
    }

    @Test
    fun `process with appended hash`() = withTemporaryGit {
        commit("commit 1")
        commit("commit 2")
        system("git tag v0.1.0")
        commit("commit 3")
        commit("commit 4")
        system("git tag v1.0.0")
        commit("commit 5")

        process("--append_hash").apply {
            assertThat(out).matches("1.0.1[+][0-9a-f]{8}".toRegex())
        }
    }

    @Test
    fun `failure on missing repository`() = withTemporaryGit(create = false) {
        try {
            process()
            fail("No exception thrown")
        } catch (ex: Application.ExecutionError) {
            assertThat(ex.reason).isEqualTo(Application.ExecutionError.Reason.NO_REPOSITORY)
        }
    }

    @Test
    fun `failure on empty history`() = withTemporaryGit {
        try {
            process()
            fail("No exception thrown")
        } catch (ex: Application.ExecutionError) {
            assertThat(ex.reason).isEqualTo(Application.ExecutionError.Reason.NO_HISTORY)
        }
    }

    @Test
    fun `provide azure environment`() = withTemporaryGit {
        commit("commit 1")

        process("--pipeline", "azure", env = mapOf("BUILD_BUILDID" to "custom")).apply {
            assertThat(err).apply {
                contains("##vso[build.updatebuildnumber]0.0.1")
                contains("##vso[task.setvariable variable=VERSION]0.0.1")
            }
        }
    }

    @Test
    fun `provide github environment`() = withTemporaryGit {
        commit("commit 1")

        val env = mapOf(
            "GITHUB_ACTION" to "ACTION_ID",
            "GITHUB_ENV" to "github.env",
            "GITHUB_OUTPUT" to "github.out",
        )

        process("--pipeline", "github", env = env).apply {
            assertThat(err).contains("::notice title=GitVersion::Calculated version is 0.0.1")
            assertThat("github.env".toPath().readLines()).isNotNull().containsOnly("VERSION=0.0.1")
            assertThat("github.out".toPath().readLines()).isNotNull().containsOnly("VERSION=0.0.1")
        }
    }

    @Test
    fun `provide gitlab environment with default config`() = withTemporaryGit {
        commit("commit 1")

        process("--pipeline", "gitlab", env = mapOf("GITLAB_CI" to "true")).apply {
            assertThat("build.env".toPath().readLines()).isNotNull().containsOnly("VERSION=0.0.1")
        }
    }

    @Test
    fun `provide gitlab environment with custom config`() = withTemporaryGit {
        commit("commit 1")

        val env = mapOf(
            "GITLAB_CI" to "true",
        )

        process("--pipeline", "gitlab", "--pipeline-gitlab-dotenv", "my.env", env = env).apply {
            assertThat("my.env".toPath().readLines()).isNotNull().containsOnly("VERSION=0.0.1")
        }
    }


}