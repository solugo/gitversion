import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.matches
import gitversion.Application
import me.archinamon.fileio.File
import me.archinamon.fileio.readText
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

        assertThat(process().out).isEqualTo("1.0.1")
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

        assertThat(process().out).isEqualTo("1.0.2-dirty")
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

        assertThat(process("-tp", "test1-v(.+)").out).isEqualTo("1.0.2")
        assertThat(process("-tp", "test2-v(.+)").out).isEqualTo("2.0.1")
    }

    @Test
    fun `process with major message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        assertThat(process("--major_pattern", "release.+").out).isEqualTo("2.0.1")
    }

    @Test
    fun `process with minor message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        assertThat(process("--minor_pattern", "release.+").out).isEqualTo("0.2.1")
    }

    @Test
    fun `process with patch message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        assertThat(process("--patch_pattern", "release.+").out).isEqualTo("0.0.2")
    }

    @Test
    fun `process with directory`() = withTemporaryGit {
        commit("commit 1")
        createDirectory("component")
        system("echo \"value\" > component/file.txt")
        commit("commit 2")
        commit("commit 3")

        assertThat(process("-d", "component").out).isEqualTo("0.0.1")
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

        assertThat(process("-c", "component").out).isEqualTo("1.0.1")
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

        assertThat(process("-c", "component", "-d", "sub", "-tp", "x(.+)").out).isEqualTo("1.0.1")
    }

    @Test
    fun `process with uncommited changes and default suffix`() = withTemporaryGit {
        commit("commit 1")
        system("echo \"value\" > file.txt")

        assertThat(process().out).isEqualTo("0.0.2-SNAPSHOT")
    }

    @Test
    fun `process with uncommited changes and custom suffix`() = withTemporaryGit {
        commit("commit 1")
        system("echo \"value\" > file.txt")

        assertThat(process("--dirty_suffix", "CUSTOM").out).isEqualTo("0.0.2-CUSTOM")
    }

    @Test
    fun `process with uncommited changes and without suffix`() = withTemporaryGit {
        commit("commit 1")
        system("echo \"value\" > file.txt")

        assertThat(process("--dirty_suffix", "").out).isEqualTo("0.0.2")
    }

    @Test
    fun `process with uncommited changes and deactivated dirty handling`() = withTemporaryGit {
        commit("commit 1")
        system("echo \"value\" > file.txt")

        assertThat(process("--dirty_ignore").out).isEqualTo("0.0.1")
    }

    @Test
    fun `process with major override`() = withTemporaryGit {
        commit("commit 1")

        assertThat(process("--major_override", "2").out).isEqualTo("2.0.1")
    }

    @Test
    fun `process with minor override`() = withTemporaryGit {
        commit("commit 1")

        assertThat(process("--minor_override", "2").out).isEqualTo("0.2.1")
    }

    @Test
    fun `process with patch override`() = withTemporaryGit {
        commit("commit 1")

        assertThat(process("--patch_override", "2").out).isEqualTo("0.0.2")
    }

    @Test
    fun `process with suffix override`() = withTemporaryGit {
        commit("commit 1")

        assertThat(process("--suffix_override", "CUSTOM").out).isEqualTo("0.0.1-CUSTOM")
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

        assertThat(process("--append_hash").out).matches("1.0.1[+][0-9a-f]{8}".toRegex())
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
        assertThat(process("--pipeline", "azure", env = mapOf("BUILD_BUILDID" to "custom")).err).apply {
            contains("##vso[build.updatebuildnumber]0.0.1")
            contains("##vso[task.setvariable variable=VERSION]0.0.1")
        }
    }

    @Test
    fun `provide github environment`() = withTemporaryGit {
        commit("commit 1")
        val env = mapOf("GITHUB_ENV" to "github.env", "GITHUB_OUTPUT" to "github.out")
        assertThat(process("--pipeline", "github", env = env).err).apply {
            contains("::notice title=GitVersion::Calculated version is 0.0.1")
        }

        assertThat(File("github.env").readText(), "VERSION=0.0.1\n")
        assertThat(File("github.out").readText(), "VERSION=0.0.1\n")
    }

    @Test
    fun `provide gitlab environment with default config`() = withTemporaryGit {
        commit("commit 1")
        process("--pipeline", "gitlab", env = mapOf("GITLAB_CI" to "true"))

        assertThat(File("build.env").readText(), "VERSION=0.0.1\n")
    }

    @Test
    fun `provide gitlab environment with custom config`() = withTemporaryGit {
        commit("commit 1")
        process("--pipeline", "gitlab", "--pipeline-gitlab-dotenv", "my.env", env = mapOf("GITLAB_CI" to "true"))

        assertThat(File("my.env").readText(), "VERSION=0.0.1\n")
    }


}