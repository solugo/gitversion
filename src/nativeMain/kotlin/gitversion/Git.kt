
package gitversion

import cnames.structs.git_commit
import cnames.structs.git_diff
import cnames.structs.git_object
import cnames.structs.git_repository
import cnames.structs.git_revwalk
import cnames.structs.git_tree
import git.*
import kotlinx.cinterop.*
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.UInt
import kotlin.also
import kotlin.arrayOf
import kotlin.checkNotNull
import kotlin.collections.Set
import kotlin.collections.buildSet
import kotlin.error
import kotlin.let
import kotlin.run
import kotlin.text.Regex
import kotlin.toUInt

@OptIn(ExperimentalForeignApi::class)
class Git(path: String) {
    private val repositoryPointer = nativeHeap.allocPointerTo<git_repository>()

    init {
        git_libgit2_init().handleGitError()
        git_repository_open(repositoryPointer.ptr, path).handleGitError()
    }

    companion object {
        fun git_error_code.handleGitError() {
            this.also { value ->
                if (value < 0) {
                    git_error_last()?.also {
                        error(it.pointed.run { "$value/$klass: ${message?.toKString()}" })
                    }
                }
            }
        }

        fun git_oid.toKStringFromUtf8() = memScoped {
            allocArray<ByteVar>(41).also { string ->
                git_oid_tostr(string, 41u, ptr)
            }.toKStringFromUtf8()
        }
    }

    fun consumeTags(regex: Regex?, callback: (Tag) -> Boolean) {
        data class Parameters(
            val repository: CPointer<git_repository>,
            val regex: Regex?,
            val callback: (Tag) -> Boolean
        )

        val repository = checkNotNull(repositoryPointer.value)
        val parameters = Parameters(repository, regex, callback)

        memScoped {
            val foreach: git_tag_foreach_cb = staticCFunction { ref, oid, payload ->
                memScoped {
                    val params = checkNotNull(payload).asStableRef<Parameters>().get()

                    val sourcePointer = allocPointerTo<git_object>().let {
                        git_object_lookup(it.ptr, params.repository, oid, GIT_OBJECT_ANY)
                        it.value ?: return@staticCFunction 1
                    }

                    val targetPointer = allocPointerTo<git_object>().let {
                        git_object_peel(it.ptr, sourcePointer, GIT_OBJECT_COMMIT).handleGitError()
                        it.value ?: return@staticCFunction 1
                    }

                    val name = ref?.toKStringFromUtf8()?.removePrefix("refs/tags/")
                    val commit = git_object_id(targetPointer)?.pointed?.toKStringFromUtf8()

                    if (name != null && commit != null) {
                        val tag = Tag(name, commit, params.regex?.containsMatchIn(name) != false)
                        if (params.callback(tag)) 0 else 1
                    } else {
                        0
                    }
                }
            }

            git_tag_foreach(repositoryPointer.value, foreach, StableRef.create(parameters).asCPointer())
        }
    }

    fun consumeCommits(
        directory: String? = null,
        callback: (Commit) -> Boolean,
    ) {
        data class Parameters(var matches: Boolean = true)

        memScoped {
            val head = alloc<git_oid>().also {
                git_reference_name_to_id(it.ptr, repositoryPointer.value, "HEAD")
            }

            val parameters = Parameters()
            val walkOid = alloc<git_oid>()
            val walkCommitPointer = allocPointerTo<git_commit>()
            val parentCommitPointer = allocPointerTo<git_commit>()
            val walkPointer = allocPointerTo<git_revwalk>()
            val walkTreePointer = allocPointerTo<git_tree>()
            val parentTreePointer = allocPointerTo<git_tree>()
            val diffPointer = allocPointerTo<git_diff>()
            val diffOptions = alloc<git_diff_options>()
            val diffCallback: git_diff_notify_cb = staticCFunction { _, _, _, payload ->
                checkNotNull(payload).asStableRef<Parameters>().get().matches = true
                1
            }

            git_revwalk_new(walkPointer.ptr, repositoryPointer.value)

            if (directory != null) {
                git_diff_options_init(diffOptions.ptr, GIT_DIFF_OPTIONS_VERSION.toUInt())
                diffOptions.payload = StableRef.create(parameters).asCPointer()
                diffOptions.notify_cb = diffCallback
                diffOptions.pathspec.count = 1U
                diffOptions.pathspec.strings = arrayOf("$directory/*").toCStringArray(this)
            }

            walkPointer.value?.also { walk ->
                git_revwalk_sorting(walk, GIT_SORT_TOPOLOGICAL)
                git_revwalk_push(walk, head.ptr)

                while (true) {
                    git_revwalk_next(walkOid.ptr, walk) == 0 || break
                    git_commit_lookup(walkCommitPointer.ptr, repositoryPointer.value, walkOid.ptr)

                    val oid = walkOid.toKStringFromUtf8()
                    val message = git_commit_message(walkCommitPointer.value)?.toKStringFromUtf8()

                    parameters.matches = directory == null

                    if (!parameters.matches) {
                        git_commit_parent(parentCommitPointer.ptr, walkCommitPointer.value, 0u)
                        git_commit_tree(walkTreePointer.ptr, walkCommitPointer.value)
                        git_commit_tree(parentTreePointer.ptr, parentCommitPointer.value)

                        if (parentTreePointer.value !== null) {
                            git_diff_tree_to_tree(
                                diffPointer.ptr,
                                repositoryPointer.value,
                                parentTreePointer.value,
                                walkTreePointer.value,
                                diffOptions.ptr
                            )
                        }
                    }

                    val commit = Commit(oid, message?.substringBefore("\n") ?: "", parameters.matches)
                    if (!callback(commit)) break
                }
            }
        }
    }

    fun consumeModifications(
        callback: (Modification) -> Boolean,
    ) {
        memScoped {
            data class Parameters(var callback: (Modification) -> Boolean)

            val statusCallback: git_status_cb = staticCFunction { path, status, payload ->
                fun UInt.hasBitSet(value: UInt) = (this and value) == value

                val modification = Modification(
                    file = path?.toKStringFromUtf8(),
                    status = buildSet {
                        if (status.hasBitSet(GIT_STATUS_IGNORED)) return@buildSet
                        if (status.hasBitSet(GIT_STATUS_INDEX_NEW)) add(ModificationStatus.NEW)
                        if (status.hasBitSet(GIT_STATUS_INDEX_MODIFIED)) add(ModificationStatus.MODIFIED)
                        if (status.hasBitSet(GIT_STATUS_INDEX_RENAMED)) add(ModificationStatus.RENAMED)
                        if (status.hasBitSet(GIT_STATUS_INDEX_DELETED)) add(ModificationStatus.DELETED)
                        if (status.hasBitSet(GIT_STATUS_INDEX_TYPECHANGE)) add(ModificationStatus.TYPECHANGE)
                        if (status.hasBitSet(GIT_STATUS_WT_NEW)) add(ModificationStatus.NEW)
                        if (status.hasBitSet(GIT_STATUS_WT_MODIFIED)) add(ModificationStatus.MODIFIED)
                        if (status.hasBitSet(GIT_STATUS_WT_RENAMED)) add(ModificationStatus.RENAMED)
                        if (status.hasBitSet(GIT_STATUS_WT_DELETED)) add(ModificationStatus.DELETED)
                        if (status.hasBitSet(GIT_STATUS_WT_TYPECHANGE)) add(ModificationStatus.TYPECHANGE)
                    }
                )

                if (modification.status.isEmpty()) return@staticCFunction 0

                if (checkNotNull(payload).asStableRef<Parameters>().get().callback(modification)) 1 else 0
            }

            val parameters = Parameters(callback)

            git_status_foreach(repositoryPointer.value, statusCallback, StableRef.create(parameters).asCPointer())
        }
    }

    fun close() {
        nativeHeap.free(repositoryPointer)
    }

    data class Tag(
        val name: String,
        val commit: String,
        val matches: Boolean,
    )

    data class Modification(
        val file: String?,
        val status: Set<ModificationStatus>,
    )

    enum class ModificationStatus {
        NEW, MODIFIED, DELETED, RENAMED, TYPECHANGE
    }

    data class Commit(
        val id: String,
        val message: String,
        val matches: Boolean,
    )


}