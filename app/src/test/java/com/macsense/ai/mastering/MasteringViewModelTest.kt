package com.macsense.ai.mastering

import com.macsense.ai.ui.viewmodel.MasteringViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class MasteringViewModelTest {

    @Test
    fun testSelectProfileUpdatesTargets() {
        val vm = MasteringViewModel()
        vm.selectProfile("hiphop")
        assertEquals("Hip Hop / Trap", vm.selectedProfile.value.name)
        assertEquals(-9.0, vm.targetLufs.value, 0.001)
        assertEquals(-0.5, vm.ceilingDbtp.value, 0.001)
    }

    @Test
    fun testImportReferenceTrack() {
        val vm = MasteringViewModel()
        val sampleRate = 44100
        val size = sampleRate
        val left = DoubleArray(size)
        val right = DoubleArray(size)
        var phase = 0.0
        for (i in 0 until size) {
            val sample = sin(phase) * 0.4
            left[i] = sample
            right[i] = sample
            phase += 2.0 * Math.PI * 440.0 / sampleRate
        }
        val channels = arrayOf(left, right)

        vm.importReferenceTrack(channels, sampleRate, "Imported Jazz")

        val selected = vm.selectedProfile.value
        assertEquals("Imported Jazz", selected.name)
        assertTrue(vm.targetProfiles.value.contains(selected))
    }

    @Test
    fun testAnalyzeAndAriProposalWorkflow() {
        val vm = MasteringViewModel()
        val sampleRate = 44100
        val size = sampleRate
        val left = DoubleArray(size)
        val right = DoubleArray(size)
        var phase = 0.0
        for (i in 0 until size) {
            val sample = sin(phase) * 0.3
            left[i] = sample
            right[i] = sample
            phase += 2.0 * Math.PI * 300.0 / sampleRate // low-mid frequency
        }
        val channels = arrayOf(left, right)

        // Select electronic standard
        vm.selectProfile("electronic")

        // 1. Analyze Mix
        vm.analyzeMix(channels, sampleRate)
        val analysis = vm.analysisResult.value
        assertNotNull(analysis)
        assertTrue(analysis!!.detectedIssues.isNotEmpty())

        // 2. Request Ari Proposal
        vm.requestAriMasteringProposal()
        val proposal = vm.pendingProposal.value
        assertNotNull(proposal)

        // 3. Reject Proposal
        vm.rejectProposal()
        assertNull(vm.pendingProposal.value)

        // Request again
        vm.requestAriMasteringProposal()
        assertNotNull(vm.pendingProposal.value)

        // 4. Confirm Proposal
        vm.confirmProposal()
        assertNull(vm.pendingProposal.value)

        // Assert that vm settings were updated by the proposal
        assertEquals(proposal!!.eqLowGain, vm.eqLowGain.value)
        assertEquals(proposal.eqMidGain, vm.eqMidGain.value)
        assertEquals(proposal.eqHighGain, vm.eqHighGain.value)
        assertEquals(proposal.limiterThreshold, vm.limiterThreshold.value)
        assertEquals(proposal.compressorThreshold, vm.compressorThreshold.value)
    }

    @Test
    fun testABComparisonProcessing() {
        val vm = MasteringViewModel()
        val sampleRate = 44100
        val size = sampleRate
        val left = DoubleArray(size)
        val right = DoubleArray(size)
        var phase = 0.0
        for (i in 0 until size) {
            val sample = sin(phase) * 0.4
            left[i] = sample
            right[i] = sample
            phase += 2.0 * Math.PI * 440.0 / sampleRate
        }
        val channels = arrayOf(left, right)

        // Set A/B mode to Original (A)
        vm.setPlayB(false)
        val outA = vm.processComparison(channels, sampleRate)
        assertTrue(channels === outA)

        // Set A/B mode to Mastered (B)
        vm.setPlayB(true)
        vm.setEqGains(2f, 0f, 1f)
        val outB = vm.processComparison(channels, sampleRate)
        assertFalse(channels === outB)
    }
}
