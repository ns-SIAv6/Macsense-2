package com.macsense.ai.audio

import com.macsense.ai.data.local.VersionNodeEntity

/**
 * Phase 4 (issue #39): A/B version branching — the [SoundLineage] model applied to project
 * state. Pure tree logic over persisted [VersionNodeEntity] rows; the ViewModel decides when
 * to fork and the repository persists the nodes.
 */
class ProjectVersionTree(nodes: List<VersionNodeEntity> = emptyList()) {

    private val nodesById = LinkedHashMap<String, VersionNodeEntity>()

    init {
        nodes.forEach { nodesById[it.id] = it }
    }

    val nodes: List<VersionNodeEntity> get() = nodesById.values.toList()

    fun node(id: String): VersionNodeEntity? = nodesById[id]

    /** Adds a node. Parent (when non-null) must already be in the tree. */
    fun add(node: VersionNodeEntity): VersionNodeEntity {
        require(node.parentId == null || nodesById.containsKey(node.parentId)) {
            "Parent ${node.parentId} not found for version node ${node.id}"
        }
        require(!nodesById.containsKey(node.id)) { "Duplicate version node id ${node.id}" }
        nodesById[node.id] = node
        return node
    }

    /** Fork: create a child of [parentId] — the "try a different arrangement" branch point. */
    fun fork(parentId: String, newId: String, projectId: String, timestamp: Long): VersionNodeEntity {
        require(nodesById.containsKey(parentId)) { "Cannot fork unknown version $parentId" }
        return add(VersionNodeEntity(id = newId, projectId = projectId, parentId = parentId, timestamp = timestamp))
    }

    fun childrenOf(id: String): List<VersionNodeEntity> =
        nodesById.values.filter { it.parentId == id }.sortedBy { it.timestamp }

    /** Path from root to [id], inclusive — the ancestry of a version. */
    fun pathTo(id: String): List<VersionNodeEntity> {
        val path = ArrayList<VersionNodeEntity>()
        var cur = nodesById[id]
        val seen = HashSet<String>()
        while (cur != null) {
            if (!seen.add(cur.id)) break // cycle guard
            path.add(cur)
            cur = cur.parentId?.let { nodesById[it] }
        }
        return path.reversed()
    }

    /** Leaves = comparable candidate versions for A/B (no children yet). */
    fun leaves(): List<VersionNodeEntity> {
        val parentIds = nodesById.values.mapNotNull { it.parentId }.toSet()
        return nodesById.values.filter { it.id !in parentIds }.sortedBy { it.timestamp }
    }

    /** Nearest common ancestor of two versions — where an A/B pair diverged. */
    fun commonAncestor(aId: String, bId: String): VersionNodeEntity? {
        val aPath = pathTo(aId).map { it.id }.toSet()
        return pathTo(bId).lastOrNull { it.id in aPath }
    }
}
