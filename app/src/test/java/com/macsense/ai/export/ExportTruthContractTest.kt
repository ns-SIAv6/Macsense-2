package com.macsense.ai.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportTruthContractTest {
    @Test
    fun `all batch jobs have a real WAV renderer`() {
        val jobs = ExportFactory.createBatch(
            projectId = "project",
            projectName = "Truth Check",
            sourceTakeId = "take",
        )
        assertTrue(jobs.isNotEmpty())
        jobs.forEach { job ->
            assertEquals("wav", job.format.fileExtension)
            assertTrue(job.outputFileName.endsWith(".wav"))
        }
    }

    @Test
    fun `unsupported encoded and stem formats are never put in batch`() {
        val formats = ExportFactory.createBatch("p", "t", "s").map { it.format }.toSet()
        val unavailable = setOf(
            ExportFormat.INSTRUMENTAL,
            ExportFormat.ACAPELLA,
            ExportFormat.STEMS_ZIP,
            ExportFormat.AAC_320,
            ExportFormat.MP3_320,
        )
        assertTrue(formats.intersect(unavailable).isEmpty())
    }

    @Test
    fun `verified batch filenames match their declared format`() {
        ExportFactory.createBatch("p", "t", "s").forEach { job ->
            assertTrue(
                "${job.outputFileName} must use .${job.format.fileExtension}",
                job.outputFileName.endsWith(".${job.format.fileExtension}", ignoreCase = true),
            )
        }
    }
}