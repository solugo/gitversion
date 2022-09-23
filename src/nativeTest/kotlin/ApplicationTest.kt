import gitversion.Application
import platform.posix.system
import kotlin.test.Test
import kotlin.test.assertEquals
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

        assertEquals("1.0.1", process())
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

        assertEquals("1.0.2", process("-tp", "test1-v(.+)"))
        assertEquals("2.0.1", process("-tp", "test2-v(.+)"))
    }

    @Test
    fun `process with major message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        assertEquals("2.0.1", process("--major_pattern", "release.+"))
    }

    @Test
    fun `process with minor message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        assertEquals("0.2.1", process("--minor_pattern", "release.+"))
    }

    @Test
    fun `process with patch message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        assertEquals("0.0.2", process("--patch_pattern", "release.+"))
    }

    @Test
    fun `process with directory`() = withTemporaryGit {
        commit("commit 1")
        system("mkdir component")
        system("echo 'value' > component/file.txt")
        commit("commit 2")
        commit("commit 3")

        assertEquals("0.0.1", process("-d", "component"))
    }

    @Test
    fun `process with component`() = withTemporaryGit {
        commit("commit 1")
        system("git tag v0.1.0")
        commit("commit 2")
        system("git tag component-v1.0.0")
        system("mkdir component")
        system("echo 'value' > component/file.txt")
        commit("commit 3")

        assertEquals("1.0.1", process("-c", "component"))
    }

    @Test
    fun `process with component and directory and tag prefix`() = withTemporaryGit {
        commit("commit 1")
        system("git tag v0.1.0")
        commit("commit 2")
        system("mkdir component")
        system("echo 'value' > component/file.txt")
        commit("commit 3")
        system("git tag component-x1.0.0")
        system("mkdir component/sub")
        system("echo 'value' > component/sub/file.txt")
        commit("commit 4")

        assertEquals("1.0.1", process("-c", "component", "-d", "sub", "-tp", "x(.+)"))
    }

    @Test
    fun `process with uncommited changes and default suffix`() = withTemporaryGit {
        commit("commit 1")
        system("echo 'value' > file.txt")

        assertEquals("0.0.2-SNAPSHOT", process())
    }

    @Test
    fun `process with uncommited changes and custom suffix`() = withTemporaryGit {
        commit("commit 1")
        system("echo 'value' > file.txt")

        assertEquals("0.0.2-CUSTOM", process("--dirty_suffix", "CUSTOM"))
    }

    @Test
    fun `process with uncommited changes and without suffix`() = withTemporaryGit {
        commit("commit 1")
        system("echo 'value' > file.txt")

        assertEquals("0.0.2", process("--dirty_suffix", ""))
    }

    @Test
    fun `process with uncommited changes and deactivated dirty handling`() = withTemporaryGit {
        commit("commit 1")
        system("echo 'value' > file.txt")

        assertEquals("0.0.1", process("--dirty_ignore"))
    }

    @Test
    fun `process with major override`() = withTemporaryGit {
        commit("commit 1")

        assertEquals("2.0.1", process("--major_override", "2"))
    }

    @Test
    fun `process with minor override`() = withTemporaryGit {
        commit("commit 1")

        assertEquals("0.2.1", process("--minor_override", "2"))
    }

    @Test
    fun `process with patch override`() = withTemporaryGit {
        commit("commit 1")

        assertEquals("0.0.2", process("--patch_override", "2"))
    }

    @Test
    fun `process with suffix override`() = withTemporaryGit {
        commit("commit 1")

        assertEquals("0.0.1-CUSTOM", process("--suffix_override", "CUSTOM"))
    }

    @Test
    fun `failure on missing repository`() = withTemporaryGit(create = false) {
        try {
            process()
            fail("No exception thrown")
        } catch (ex: Application.ExecutionError) {
            assertEquals(ex.reason, Application.ExecutionError.Reason.NO_REPOSITORY)
        }
    }

    @Test
    fun `failure on empty history`() = withTemporaryGit {
        try {
            process()
            fail("No exception thrown")
        } catch (ex: Application.ExecutionError) {
            assertEquals(ex.reason, Application.ExecutionError.Reason.NO_HISTORY)
        }
    }

}