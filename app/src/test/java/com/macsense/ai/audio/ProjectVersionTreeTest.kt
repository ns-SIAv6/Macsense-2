package com.macsense.ai.audio

import com.macsense.ai.data.local.VersionNodeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectVersionTreeTest {

    private fun node(id: String, parent: String?, ts: Long) =
        VersionNodeEntity(id = id, projectId = "p1", parentId = parent, timestamp = ts)

    @Test
    fun `fork creates child and path traces ancestry`() {
        val tree = ProjectVersionTree()
        tree.add(node("root", null, 1))
        tree.fork("root", "a", "p1", 2)
        tree.fork("a", "a2", "p1", 3)

        assertEquals(listOf("root", "a", "a2"), tree.pathTo("a2").map { it.id })
        assertEquals(listOf("a"), tree.childrenOf("root").map { it.id })
    }

    @Test
    fun `leaves are the A-B comparison candidates`() {
        val tree = ProjectVersionTree()
        tree.add(node("root", null, 1))
        tree.fork("root", "mixA", "p1", 2)
        tree.fork("root", "mixB", "p1", 3)

        assertEquals(setOf("mixA", "mixB"), tree.leaves().map { it.id }.toSet())
    }

    @Test
    fun `common ancestor identifies divergence point`() {
        val tree = ProjectVersionTree()
        tree.add(node("root", null, 1))
        tree.fork("root", "v1", "p1", 2)
        tree.fork("v1", "mixA", "p1", 3)
        tree.fork("v1", "mixB", "p1", 4)

        assertEquals("v1", tree.commonAncestor("mixA", "mixB")?.id)
        assertEquals("root", tree.commonAncestor("root", "mixB")?.id)
        assertNull(tree.commonAncestor("mixA", "ghost"))
    }

    @Test
    fun `fork from unknown parent fails loudly`() {
        val tree = ProjectVersionTree()
        tree.add(node("root", null, 1))
        try {
            tree.fork("nope", "x", "p1", 2)
            assertTrue("expected IllegalArgumentException", false)
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun `rebuilds from persisted rows in any insertion-safe order`() {
        val rows = listOf(node("root", null, 1), node("a", "root", 2), node("b", "a", 3))
        val tree = ProjectVersionTree(rows)
        assertEquals(3, tree.nodes.size)
        assertEquals(listOf("root", "a", "b"), tree.pathTo("b").map { it.id })
    }
}
