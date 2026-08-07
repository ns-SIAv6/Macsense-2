package com.macsense.ai.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportJob(
    val id: String,
    val projectId: String,
    val format: ExportFormat,
    val outputFileName: String,
    val sourceTakeId: String,
    val timeConstraint: TimeConstraint? = null,
    val tempoModulation: TempoModulation? = null,
    val includeStems: Set<String>? = null,
    val excludeStems: Set<String> = emptySet(),
    val soundDna: String? = null
) {
    val notificationTitle: String get() = "Rendering ${format.displayName}…"
    val notificationText: String get() = outputFileName
}

data class ExportBatchStatus(
    val totalJobs: Int, val completedJobs: Int, val failedJobs: Int,
    val currentJob: ExportJob?, val isComplete: Boolean
) {
    val progressPercent: Int get() = if (totalJobs == 0) 0 else ((completedJobs + failedJobs) * 100 / totalJobs)
    companion object { val EMPTY = ExportBatchStatus(0, 0, 0, null, true) }
}
