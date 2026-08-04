package com.macsense.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.macsense.ai.data.repository.MacSenseRepository

/**
 * Supplies [DawViewModel] with a real [MacSenseRepository] (backed by [AppContainer]'s Room
 * database) instead of the `null` constructor default used in lightweight unit tests. This is
 * the first real production wiring of the Phase 5 sound-genetics persistence path: without it,
 * `breed_sounds`/`resurrect_sound` Ari commands silently no-op because [DawViewModel] has no
 * repository to read/write archive entries through.
 *
 * [genomeProjectId] scopes genome storage the same way [MacSenseRepository.upsertSoundGenome]
 * expects a project id; callers that already track a real project id (e.g. once multi-project
 * support lands) should pass it here instead of relying on the default.
 */
class DawViewModelFactory(
    private val repository: MacSenseRepository,
    private val genomeProjectId: String = "default-project"
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DawViewModel::class.java)) {
            "DawViewModelFactory can only create DawViewModel, got $modelClass"
        }
        return DawViewModel(
            repository = repository,
            genomeProjectId = genomeProjectId
        ) as T
    }
}
