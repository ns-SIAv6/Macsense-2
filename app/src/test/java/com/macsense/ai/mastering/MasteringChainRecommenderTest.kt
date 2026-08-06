package com.macsense.ai.mastering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MasteringChainRecommenderTest {

    @Test
    fun testRecommendChain_muddyLowMids() {
        val analysis = MasteringAnalyzer.AnalysisResult(
            integratedLufs = -12.0,
            peakDbtp = -0.5,
            lowMidRatio = 0.35, // very muddy (target is 0.15)
            highRatio = 0.08,
            detectedIssues = emptyList()
        )
        val target = TargetProfile.POP
        val proposal = MasteringChainRecommender.recommendChain(analysis, target)

        assertTrue(proposal.eqLowGain < 0f)
        assertTrue(proposal.eqMidGain < 0f)
        assertTrue(proposal.explanation.contains("low-mids"))
    }

    @Test
    fun testRecommendChain_harshHighShelf() {
        val analysis = MasteringAnalyzer.AnalysisResult(
            integratedLufs = -12.0,
            peakDbtp = -0.5,
            lowMidRatio = 0.15,
            highRatio = 0.25, // harsh highs (target is 0.08)
            detectedIssues = emptyList()
        )
        val target = TargetProfile.POP
        val proposal = MasteringChainRecommender.recommendChain(analysis, target)

        assertTrue(proposal.eqHighGain < 0f)
        assertTrue(proposal.explanation.contains("harsh high-shelf"))
    }

    @Test
    fun testRecommendChain_insufficientLoudness() {
        val analysis = MasteringAnalyzer.AnalysisResult(
            integratedLufs = -20.0, // quiet
            peakDbtp = -6.0,
            lowMidRatio = 0.15,
            highRatio = 0.08,
            detectedIssues = emptyList()
        )
        val target = TargetProfile.POP // target lufs is -10.0
        val proposal = MasteringChainRecommender.recommendChain(analysis, target)

        // It needs a gain boost of 10 dB, so limiter threshold should be -10.0f
        assertEquals(-10.0f, proposal.limiterThreshold, 0.1f)
    }
}
