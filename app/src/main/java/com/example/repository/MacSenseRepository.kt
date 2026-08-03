package com.example.repository

import com.example.data.local.BreedingHistoryEntity
import com.example.data.local.LyricEntity
import com.example.data.local.MacSenseDao
import com.example.data.local.MidiMappingEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.SoundGenomeEntity
import com.example.data.local.TrackEntity
import com.example.data.local.VersionNodeEntity
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MacSenseRepository(private val dao: MacSenseDao) {

    val activeGenomes: Flow<List<SoundGenome>> = dao.getAllActiveGenomes().map { entities ->
        entities.map { it.toModel() }
    }

    val extinctGenomes: Flow<List<SoundGenome>> = dao.getExtinctGenomes().map { entities ->
        entities.map { it.toModel() }
    }

    val breedingHistory: Flow<List<BreedingHistoryEntity>> = dao.getBreedingHistory()

    val midiMappings: Flow<List<MidiMappingEntity>> = dao.getMidiMappings()

    val currentProject: Flow<Project?> = dao.getProjectById("project_master_01").map { entity ->
        entity?.let {
            Project(
                id = it.id,
                title = it.title,
                genre = it.genre,
                bpm = it.bpm,
                keySignature = it.keySignature,
                targetLufs = it.targetLufs,
                createdTimestamp = it.createdTimestamp,
                modifiedTimestamp = it.modifiedTimestamp
            )
        }
    }

    val tracks: Flow<List<TrackItem>> = dao.getTracksForProject("project_master_01").map { entities ->
        entities.map { entity ->
            TrackItem(
                id = entity.id,
                name = entity.name,
                soundType = try { SoundType.valueOf(entity.soundType) } catch (_: Exception) { SoundType.SYNTH },
                volume = entity.volume,
                pan = entity.pan,
                isMuted = entity.isMuted,
                isSolo = entity.isSolo,
                genomeId = entity.genomeId
            )
        }
    }

    val lyrics: Flow<List<LyricSpan>> = dao.getLyricsForProject("project_master_01").map { entities ->
        entities.map { entity ->
            LyricSpan(
                id = entity.id,
                sectionName = entity.sectionName,
                lineIndex = entity.lineIndex,
                text = entity.text,
                startTimeMs = entity.startTimeMs,
                endTimeMs = entity.endTimeMs,
                cadenceScore = entity.cadenceScore,
                rhymeScheme = entity.rhymeScheme
            )
        }
    }

    val versionNodes: Flow<List<VersionNode>> = dao.getVersionNodes("project_master_01").map { entities ->
        entities.map { entity ->
            VersionNode(
                id = entity.id,
                parentId = entity.parentId,
                commitMessage = entity.commitMessage,
                author = entity.author,
                timestamp = entity.timestamp,
                isCurrent = entity.isCurrent
            )
        }
    }

    suspend fun breedAndSave(
        parentA: SoundGenome,
        parentB: SoundGenome,
        weightA: Float = 0.5f,
        mutationFactor: Float = 0.08f
    ): SoundGenome {
        val child = SoundGenome.breed(parentA, parentB, weightA, mutationFactor)
        dao.insertGenome(child.toEntity())

        // Save breeding history record in Room
        val historyRecord = BreedingHistoryEntity(
            id = "bh_${System.currentTimeMillis()}",
            parentAId = parentA.id,
            parentAName = parentA.name,
            parentBId = parentB.id,
            parentBName = parentB.name,
            childId = child.id,
            childName = child.name,
            breedWeight = weightA,
            mutationFactor = mutationFactor,
            generation = child.generation,
            timestamp = System.currentTimeMillis()
        )
        dao.insertBreedingHistory(historyRecord)

        return child
    }

    suspend fun saveMidiMapping(
        controllerName: String,
        ccNumber: Int,
        parameterTarget: String,
        minValue: Float = 0f,
        maxValue: Float = 1f,
        channel: Int = 1
    ) {
        val mapping = MidiMappingEntity(
            id = "mapping_cc_${ccNumber}_${parameterTarget}",
            controllerName = controllerName,
            ccNumber = ccNumber,
            parameterTarget = parameterTarget,
            minValue = minValue,
            maxValue = maxValue,
            channel = channel
        )
        dao.insertMidiMapping(mapping)
    }


    suspend fun resurrectAndSave(extinctGenome: SoundGenome): SoundGenome {
        val resurrected = SoundGenome.resurrect(extinctGenome)
        dao.insertGenome(resurrected.toEntity())
        return resurrected
    }

    suspend fun markExtinct(genome: SoundGenome, reason: String, epitaph: String) {
        dao.markGenomeExtinct(genome.id, System.currentTimeMillis(), reason, epitaph)
    }

    suspend fun updateLyric(id: String, newText: String) {
        dao.updateLyricText(id, newText)
    }

    suspend fun addVersionCommit(commitMsg: String, author: String = "ARi Co-Producer") {
        val node = VersionNodeEntity(
            id = "v_${System.currentTimeMillis()}",
            projectId = "project_master_01",
            parentId = "v_root",
            commitMessage = commitMsg,
            author = author,
            timestamp = System.currentTimeMillis(),
            isCurrent = true
        )
        dao.insertVersionNode(node)
    }

    /**
     * Pre-populate database with default Master Codex sound genomes & sample project data if empty
     */
    suspend fun seedInitialDataIfNeeded() {
        // Pre-populated default genomes
        val initialGenomes = listOf(
            SoundGenome(
                id = "genome_808_sub_heavy",
                name = "Obsidian 808 Sub",
                soundType = SoundType.SUB_808,
                mass = 0.92f,
                radiance = 0.35f,
                entropy = 0.12f,
                curvature = 0.78f,
                chrom1 = 45, // Sub freq 45Hz
                chrom2 = 1200, // Decay
                chrom3 = 85,
                chrom4 = 90,
                chrom5 = 45
            ),
            SoundGenome(
                id = "genome_kick_punch",
                name = "Cyber Transient Kick",
                soundType = SoundType.KICK,
                mass = 0.88f,
                radiance = 0.72f,
                entropy = 0.18f,
                curvature = 0.85f,
                chrom1 = 90,
                chrom2 = 250,
                chrom3 = 92,
                chrom4 = 60,
                chrom5 = 20
            ),
            SoundGenome(
                id = "genome_snare_crisp",
                name = "Lazer Crack Snare",
                soundType = SoundType.SNARE,
                mass = 0.45f,
                radiance = 0.95f,
                entropy = 0.42f,
                curvature = 0.55f,
                chrom1 = 280,
                chrom2 = 350,
                chrom3 = 70,
                chrom4 = 80,
                chrom5 = 30
            ),
            SoundGenome(
                id = "genome_hihat_metallic",
                name = "Quantum Roll HiHat",
                soundType = SoundType.HIHAT,
                mass = 0.22f,
                radiance = 0.98f,
                entropy = 0.25f,
                curvature = 0.30f,
                chrom1 = 8000,
                chrom2 = 120,
                chrom3 = 90,
                chrom4 = 40,
                chrom5 = 10
            ),
            SoundGenome(
                id = "genome_synth_lead",
                name = "Hyperdrive Saw Lead",
                soundType = SoundType.SYNTH,
                mass = 0.65f,
                radiance = 0.88f,
                entropy = 0.35f,
                curvature = 0.92f,
                chrom1 = 440,
                chrom2 = 800,
                chrom3 = 95,
                chrom4 = 85,
                chrom5 = 60
            ),
            SoundGenome(
                id = "genome_pad_ambient",
                name = "Ethereal Nebula Pad",
                soundType = SoundType.PAD,
                mass = 0.55f,
                radiance = 0.90f,
                entropy = 0.15f,
                curvature = 0.40f,
                chrom1 = 220,
                chrom2 = 2500,
                chrom3 = 60,
                chrom4 = 95,
                chrom5 = 5
            ),
            // Extinct genome for Lazarus Graveyard Vault
            SoundGenome(
                id = "genome_extinct_vortex",
                name = "Phase Collapsed Vortex",
                soundType = SoundType.FX,
                mass = 0.99f,
                radiance = 0.10f,
                entropy = 0.95f,
                curvature = 0.99f,
                chrom1 = 120,
                chrom2 = 1800,
                chrom3 = 99,
                chrom4 = 99,
                chrom5 = 90,
                isExtinct = true,
                deathTimestamp = System.currentTimeMillis() - (3600000 * 48), // 48 hrs ago
                deathReason = "Phase Cancellation & Resonance Overload",
                epitaph = "Lost in the infinite feedback loop at 99% Entropy"
            )
        )

        dao.insertGenomes(initialGenomes.map { it.toEntity() })

        // Seed project
        val project = ProjectEntity(
            id = "project_master_01",
            title = "Cyber Symphony No. 1",
            genre = "Futuristic Trap / Cyberpunk",
            bpm = 140,
            keySignature = "C Minor",
            targetLufs = -14.0f,
            createdTimestamp = System.currentTimeMillis(),
            modifiedTimestamp = System.currentTimeMillis()
        )
        dao.insertProject(project)

        // Seed tracks
        val tracks = listOf(
            TrackEntity("t1", "project_master_01", "808 Sub Bass", "SUB_808", 0.9f, 0.0f, false, false, "genome_808_sub_heavy", "[]"),
            TrackEntity("t2", "project_master_01", "Main Punch Kick", "KICK", 0.85f, 0.0f, false, false, "genome_kick_punch", "[]"),
            TrackEntity("t3", "project_master_01", "Crack Snare", "SNARE", 0.8f, 0.1f, false, false, "genome_snare_crisp", "[]"),
            TrackEntity("t4", "project_master_01", "Trap Hi-Hat Rolls", "HIHAT", 0.75f, -0.2f, false, false, "genome_hihat_metallic", "[]"),
            TrackEntity("t5", "project_master_01", "Hyperdrive Synth Lead", "SYNTH", 0.7f, 0.3f, false, false, "genome_synth_lead", "[]")
        )
        dao.insertTracks(tracks)

        // Seed lyrics
        val lyrics = listOf(
            LyricEntity("l1", "project_master_01", "Intro", 0, "Stepping through the neon grid, frequencies align", 0, 2000, 0.92f, "AABB"),
            LyricEntity("l2", "project_master_01", "Intro", 1, "Sub-bass pulsing in my veins, digital design", 2000, 4000, 0.89f, "AABB"),
            LyricEntity("l3", "project_master_01", "Verse 1", 2, "Bred a new 808 with ninety percent Mass", 4000, 6000, 0.95f, "AABB"),
            LyricEntity("l4", "project_master_01", "Verse 1", 3, "Shatter every limit, breaking through the glass", 6000, 8000, 0.91f, "AABB")
        )
        for (l in lyrics) dao.insertLyric(l)

        // Seed initial version node
        dao.insertVersionNode(
            VersionNodeEntity("v_root", "project_master_01", null, "Initial Master Codex Setup", "ARi System", System.currentTimeMillis(), true)
        )
    }

    private fun SoundGenome.toEntity(): SoundGenomeEntity = SoundGenomeEntity(
        id = id,
        name = name,
        soundType = soundType.name,
        mass = mass,
        radiance = radiance,
        entropy = entropy,
        curvature = curvature,
        chrom1 = chrom1,
        chrom2 = chrom2,
        chrom3 = chrom3,
        chrom4 = chrom4,
        chrom5 = chrom5,
        generation = generation,
        parentAId = parentAId,
        parentBId = parentBId,
        scarMagnitude = scarMagnitude,
        isExtinct = isExtinct,
        deathTimestamp = deathTimestamp,
        deathReason = deathReason,
        epitaph = epitaph,
        isFavorite = isFavorite
    )

    private fun SoundGenomeEntity.toModel(): SoundGenome = SoundGenome(
        id = id,
        name = name,
        soundType = try { SoundType.valueOf(soundType) } catch (_: Exception) { SoundType.SYNTH },
        mass = mass,
        radiance = radiance,
        entropy = entropy,
        curvature = curvature,
        chrom1 = chrom1,
        chrom2 = chrom2,
        chrom3 = chrom3,
        chrom4 = chrom4,
        chrom5 = chrom5,
        generation = generation,
        parentAId = parentAId,
        parentBId = parentBId,
        scarMagnitude = scarMagnitude,
        isExtinct = isExtinct,
        deathTimestamp = deathTimestamp,
        deathReason = deathReason,
        epitaph = epitaph,
        isFavorite = isFavorite
    )
}
