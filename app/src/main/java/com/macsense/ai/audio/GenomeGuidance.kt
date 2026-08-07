package com.macsense.ai.audio

/**
 * P5 flagship (issue #37): genome-guided generation.
 *
 * Exposes a [SoundGenome] as four user-facing slider parameters — brightness, density,
 * rhythmic complexity and harmonic tension — and maps edited slider values back into a
 * genome. The mapping is a lossless affine round-trip over the genome's 0..1 trait space,
 * so tweaking a slider is literally editing the trait the DSP analyzers measure
 * (SpectrumAnalyzer → brightness, transient profile → density, DynamicsAnalyzer → rhythmic
 * complexity, harmonicity → harmonic tension inverted: tenser = less harmonic).
 */
data class GenomeGuidanceParams(
    val brightness: Double,
    val density: Double,
    val rhythmicComplexity: Double,
    val harmonicTension: Double,
) {
    init {
        listOf(brightness, density, rhythmicComplexity, harmonicTension).forEach {
            require(it in 0.0..1.0) { "Guidance parameters must be in 0..1, got $it" }
        }
    }
}

object GenomeGuidance {

    fun fromGenome(genome: SoundGenome): GenomeGuidanceParams = GenomeGuidanceParams(
        brightness = genome.brightness.coerceIn(0.0, 1.0),
        density = genome.transient.coerceIn(0.0, 1.0),
        rhythmicComplexity = genome.dynamics.coerceIn(0.0, 1.0),
        harmonicTension = (1.0 - genome.harmonicity).coerceIn(0.0, 1.0),
    )

    /** Applies slider values onto a genome, preserving identity/ancestry fields. */
    fun applyTo(genome: SoundGenome, params: GenomeGuidanceParams): SoundGenome = genome.copy(
        brightness = params.brightness,
        transient = params.density,
        dynamics = params.rhythmicComplexity,
        harmonicity = 1.0 - params.harmonicTension,
    )
}
