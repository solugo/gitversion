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
import kotlin.String
import kotlin.also

class Git(path: String) {
    private val repositoryReference: CPointerVar<git_repository> = nativeHeap.allocPointerTo()

    init {
        git_libgit2_init().handleGitError()
        git_repository_open(repositoryReference.ptr, path).handleGitError()
    }

    companion object {
        fun git_error_code.handleGitError() {
            this.also { value ->
                if (value < 0) {
                    git_error_last()?.also {
                        throw RuntimeException(it.pointed.run { "$value/$klass: ${message?.toKString()}" })
                    }
                }
            }
        }

        fun git_oid.toKStringFromUtf8() = memScoped {
            allocArray<ByteVar>(41).also { string ->
                git_oid_tostr(string, 41, ptr)
            }.toKStringFromUtf8()
        }
    }

    fun consumeTags(callback: (Tag) -> Boolean) {
        data class Parameters(val repository: CPointer<git_repository>, val callback: (Tag) -> Boolean)

        val repository = checkNotNull(repositoryReference.pointed)
        val parameters = Parameters(repository.ptr, callback)

        memScoped {
            val foreach: git_tag_foreach_cb =
                staticCFunction { ref: CPointer<ByteVar>?, oid: CPointer<git_oid>?, payload: COpaquePointer? ->
                    memScoped {
                        val params = checkNotNull(payload).asStableRef<Parameters>().get()

                        val source = allocPointerTo<git_object>().let {
                            git_object_lookup(it.ptr, params.repository, oid, GIT_OBJECT_ANY)
                            it.pointed ?: return@staticCFunction 1
                        }

                        val target = allocPointerTo<git_object>().let {
                            git_object_peel(it.ptr, source.ptr, GIT_OBJECT_COMMIT).handleGitError()
                            it.pointed ?: return@staticCFunction 1
                        }

                        val name = ref?.toKStringFromUtf8()?.removePrefix("refs/tags/")
                        val commit = git_object_id(target.ptr)?.pointed?.toKStringFromUtf8()

                        if (name != null && commit != null) {
                            if (params.callback(Tag(name, commit))) 0 else 1
                        } else {
                            0
                        }
                    }
                }

            git_tag_foreach(repositoryReference.pointed?.ptr, foreach, StableRef.create(parameters).asCPointer())
        }
    }

    fun consumeCommits(
        directory: String? = null,
        callback: (Commit) -> Boolean,
    ) {
        val repository = checkNotNull(repositoryReference.pointed)

        data class Parameters(var matches: Boolean = true)

        memScoped {
            val head = alloc<git_oid>().also {
                git_reference_name_to_id(it.ptr, repository.ptr, "HEAD")
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
            val diffCallback: git_diff_notify_cb =
                staticCFunction { _: CPointer<git_diff>?, _: CPointer<git_diff_delta>?, _: CPointer<ByteVar>?, payload: COpaquePointer? ->
                    checkNotNull(payload).asStableRef<Parameters>().get().matches = true
                    1
                }



            git_revwalk_new(walkPointer.ptr, repository.ptr)

            if (directory != null) {
                git_diff_options_init(diffOptions.ptr, GIT_DIFF_OPTIONS_VERSION)
                diffOptions.payload = StableRef.create(parameters).asCPointer()
                diffOptions.notify_cb = diffCallback
                diffOptions.pathspec.count = 1U
                diffOptions.pathspec.strings = arrayOf("$directory/**/*").toCStringArray(memScope)
            }

            walkPointer.pointed?.also { walk ->
                git_revwalk_sorting(walk.ptr, GIT_SORT_TIME)
                git_revwalk_push(walk.ptr, head.ptr)

                while (true) {
                    git_revwalk_next(walkOid.ptr, walk.ptr) == 0 || break
                    git_commit_lookup(walkCommitPointer.ptr, repository.ptr, walkOid.ptr)

                    val oid = walkOid.toKStringFromUtf8()
                    val message = git_commit_message(walkCommitPointer.pointed?.ptr)?.toKStringFromUtf8()

                    parameters.matches = directory == null

                    if (!parameters.matches) {
                        git_commit_parent(parentCommitPointer.ptr, walkCommitPointer.value, 0)
                        git_commit_tree(walkTreePointer.ptr, walkCommitPointer.pointed?.ptr)
                        git_commit_tree(parentTreePointer.ptr, parentCommitPointer.pointed?.ptr)

                        if (parentTreePointer.pointed !== null) {
                            git_diff_tree_to_tree(
                                diffPointer.ptr,
                                repository.ptr,
                                parentTreePointer.value,
                                walkTreePointer.value,
                                diffOptions.ptr
                            )
                        }
                    }

                    if (!callback(Commit(oid, message?.substringBefore("\n") ?: "", parameters.matches))) break
                }
            }
        }
    }

    fun close() {
        nativeHeap.free(repositoryReference)
    }

    data class Tag(
        val name: String,
        val commit: String,
    )

    data class Commit(
        val id: String,
        val message: String,
        val matches: Boolean,
    )


}