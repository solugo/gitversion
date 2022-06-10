import platform.posix.system
import kotlin.test.Test
import kotlin.test.assertEquals

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

        assertEquals("1.0.2", process("-tp", "test1.*"), "version for test1")
        assertEquals("2.0.1", process("-tp", "test2.*"), "version for test2")
    }

    @Test
    fun `process with major message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        assertEquals("2.0.1", process("--major_pattern", "release"), "version for test1")
    }

    @Test
    fun `process with minor message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        assertEquals("0.2.1", process("--minor_pattern", "release"), "version for test1")
    }

    @Test
    fun `process with patch message pattern`() = withTemporaryGit {
        commit("commit 1")
        commit("release 2")
        commit("commit 3")
        commit("release 4")
        commit("commit 5")

        assertEquals("0.0.2", process("--patch_pattern", "release"), "version for test1")
    }

    @Test
    fun `process with directory`() = withTemporaryGit {
        commit("commit 1")
        system("mkdir component")
        system("echo 'value' > component/file.txt")
        commit("commit 2")
        commit("commit 3")

        assertEquals("0.0.1", process("-d", "component"), "version for test1")
    }

}