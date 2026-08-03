package com.example.data.model

import androidx.compose.ui.graphics.Color
import kotlin.math.log10
import kotlin.math.min

/**
 * 4D Tensor (Mass, Radiance, Entropy, Curvature) plus 5-integer Chromosome.
 * Formula for breeding:
 * T_child,i = clamp(T_A,i * w_i + T_B,i * (1-w_i) + delta_mut, 0.0, 1.0)
 */
data class SoundGenome(
    val id: String,
    val name: String,
    val soundType: SoundType,
    // 4D Tensor components [0.0..1.0]
    val mass: Float,
    val radiance: Float,
    val entropy: Float,
    val curvature: Float,
    // 5-integer chromosome
    val chrom1: Int, // Frequency / Pitch Base (Hz or semitone shift)
    val chrom2: Int, // Envelope Decay (ms)
    val chrom3: Int, // Timbre / Harmonic Content (1..100)
    val chrom4: Int, // Resonance / Q factor (1..100)
    val chrom5: Int, // Distortion / Drive (0..100)
    val generation: Int = 1,
    val parentAId: String? = null,
    val parentBId: String? = null,
    val scarMagnitude: Float = 0.0f,
    val isExtinct: Boolean = false,
    val deathTimestamp: Long? = null,
    val deathReason: String? = null,
    val epitaph: String? = null,
    val isFavorite: Boolean = false
) {
    /**
     * Breed two sound genomes according to the Master Codex formula
     */
    companion object {
        fun breed(
            parentA: SoundGenome,
            parentB: SoundGenome,
            weightA: Float = 0.5f,
            mutationFactor: Float = 0.08f,
            newId: String = "genome_${System.currentTimeMillis()}",
            newName: String = "${parentA.name.take(3)}-${parentB.name.take(3)} Hybrid"
        ): SoundGenome {
            val weightB = 1.0f - weightA

            // Mutation random delta [-mutationFactor, +mutationFactor]
            fun mutate(value: Float): Float {
                val delta = (Math.random().toFloat() * 2f - 1f) * mutationFactor
                return (value * weightA + value * weightB + delta).coerceIn(0.05f, 0.98f)
            }

            val childMass = ((parentA.mass * weightA + parentB.mass * weightB) + (Math.random().toFloat() * 2f - 1f) * mutationFactor).coerceIn(0.05f, 1.0f)
            val childRadiance = ((parentA.radiance * weightA + parentB.radiance * weightB) + (Math.random().toFloat() * 2f - 1f) * mutationFactor).coerceIn(0.05f, 1.0f)
            val childEntropy = ((parentA.entropy * weightA + parentB.entropy * weightB) + (Math.random().toFloat() * 2f - 1f) * mutationFactor).coerceIn(0.05f, 1.0f)
            val childCurvature = ((parentA.curvature * weightA + parentB.curvature * weightB) + (Math.random().toFloat() * 2f - 1f) * mutationFactor).coerceIn(0.05f, 1.0f)

            val childChrom1 = ((parentA.chrom1 * weightA + parentB.chrom1 * weightB) + (Math.random() * 20 - 10)).toInt().coerceIn(20, 2000)
            val childChrom2 = ((parentA.chrom2 * weightA + parentB.chrom2 * weightB) + (Math.random() * 50 - 25)).toInt().coerceIn(10, 3000)
            val childChrom3 = ((parentA.chrom3 * weightA + parentB.chrom3 * weightB) + (Math.random() * 10 - 5)).toInt().coerceIn(1, 100)
            val childChrom4 = ((parentA.chrom4 * weightA + parentB.chrom4 * weightB) + (Math.random() * 10 - 5)).toInt().coerceIn(1, 100)
            val childChrom5 = ((parentA.chrom5 * weightA + parentB.chrom5 * weightB) + (Math.random() * 10 - 5)).toInt().coerceIn(0, 100)

            val nextGen = maxOf(parentA.generation, parentB.generation) + 1

            return SoundGenome(
                id = newId,
                name = newName,
                soundType = if (Math.random() > 0.5) parentA.soundType else parentB.soundType,
                mass = childMass,
                radiance = childRadiance,
                entropy = childEntropy,
                curvature = childCurvature,
                chrom1 = childChrom1,
                chrom2 = childChrom2,
                chrom3 = childChrom3,
                chrom4 = childChrom4,
                chrom5 = childChrom5,
                generation = nextGen,
                parentAId = parentA.id,
                parentBId = parentB.id
            )
        }

        /**
         * Lazarus Resurrection formula:
         * scar = min(0.35, 0.05 + 0.08 * log10(1 + t_extinct_hours))
         */
        fun resurrect(extinctGenome: SoundGenome): SoundGenome {
            val tExtinctHours = if (extinctGenome.deathTimestamp != null) {
                ((System.currentTimeMillis() - extinctGenome.deathTimestamp) / (1000 * 3600)).toFloat().coerceAtLeast(0.1f)
            } else 1.0f

            val scar = minOf(0.35f, 0.05f + 0.08f * log10(1f + tExtinctHours))
            val newEntropy = (extinctGenome.entropy + scar).coerceAtMost(0.99f)

            return extinctGenome.copy(
                isExtinct = false,
                scarMagnitude = scar,
                entropy = newEntropy,
                name = "Resurrected ${extinctGenome.name} ( Scar ${(scar * 100).toInt()}% )"
            )
        }

        /**
         * Heterozygosity calculation (Wright's formula) for genetic diversity
         */
        fun calculateHeterozygosity(genomes: List<SoundGenome>): Float {
            if (genomes.isEmpty()) return 1.0f
            val avgMass = genomes.map { it.mass }.average().toFloat()
            val variance = genomes.map { (it.mass - avgMass) * (it.mass - avgMass) }.average().toFloat()
            return (variance * 4.0f).coerceIn(0.1f, 0.98f)
        }
    }
}

enum class SoundType(val label: String, val color: Color, val hexString: String) {
    SUB_808("808 Sub", Color(0xFFFF0055), "#FF0055"),
    KICK("Kick", Color(0xFFFF5500), "#FF5500"),
    SNARE("Snare", Color(0xFF00E5FF), "#00E5FF"),
    HIHAT("Hi-Hat", Color(0xFFFFEE00), "#FFEE00"),
    SYNTH("Synth Lead", Color(0xFFA000FF), "#A000FF"),
    PAD("Ambient Pad", Color(0xFF00FF87), "#00FF87"),
    FX("Laser FX", Color(0xFFFF00D6), "#FF00D6")
}
