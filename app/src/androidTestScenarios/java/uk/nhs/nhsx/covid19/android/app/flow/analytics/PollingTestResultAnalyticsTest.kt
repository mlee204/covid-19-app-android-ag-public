package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.PollingTestResult
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.report.notReported

class PollingTestResultAnalyticsTest : AnalyticsTest() {

    private var selfDiagnosis = SelfDiagnosis(this)
    private var pollingTestResult = PollingTestResult(this)

    // hasTestedPositiveBackgroundTick - Polling
    // >0 if the app is aware that the user has received/entered a positive PCR test
    // this currently happens during an isolation and for the 14 days after isolation
    @Test
    fun receivePositivePCRTestResultAfterSelfDiagnosis() = notReported {
        receivePositiveTestResultAfterSelfDiagnosis(
            LAB_RESULT,
            Metrics::receivedPositiveTestResultViaPolling,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick
        )
    }

    // hasTestedPositiveBackgroundTick - Polling
    // >0 if the app is aware that the user has received/entered a positive assisted LFD test
    // this currently happens during an isolation and for the 14 days after isolation
    @Test
    fun receivePositiveAssistedLFDTestResultAfterSelfDiagnosis() = notReported {
        receivePositiveTestResultAfterSelfDiagnosis(
            RAPID_RESULT,
            Metrics::receivedPositiveLFDTestResultViaPolling,
            Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
            Metrics::hasTestedLFDPositiveBackgroundTick
        )
    }

    // hasTestedPositiveBackgroundTick - Polling
    // >0 if the app is aware that the user has received/entered a positive unassisted LFD test
    // this currently happens during an isolation and for the 14 days after isolation
    @Test
    fun receivePositiveUnassistedLFDTestResultAfterSelfDiagnosis() = notReported {
        receivePositiveTestResultAfterSelfDiagnosis(
            RAPID_SELF_REPORTED,
            Metrics::receivedPositiveLFDTestResultViaPolling,
            Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
            Metrics::hasTestedLFDPositiveBackgroundTick
        )
    }

    private fun receivePositiveTestResultAfterSelfDiagnosis(
        testKitType: VirologyTestKitType,
        receivedPositiveTestResultViaPollingMetric: MetricsProperty,
        isIsolatingForTestedPositiveBackgroundTickMetric: MetricsProperty,
        hasTestedPositiveBackgroundTickMetric: MetricsProperty
    ) {
        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        // Starting state: App running normally, not in isolation
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms and order test on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 10th Jan
        selfDiagnosis.selfDiagnosePositiveAndOrderTest()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertEquals(1, Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        pollingTestResult.receiveAndAcknowledgePositiveTestResult(
            testKitType,
            this::advanceToNextBackgroundTaskExecution
        )

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            // Still in isolation, for both self-diagnosis and positive test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, receivedPositiveTestResultViaPollingMetric)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
        }

        // Dates: 5th-10th Jan -> Analytics packets for: 4th-9th Jan
        assertOnFieldsForDateRange(5..10) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
        }

        // Dates: 11th-24th Jan -> Analytics packets for: 10th-23rd Jan
        assertOnFieldsForDateRange(11..24) {
            // Isolation is over, but isolation reason still stored for 14 days
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
        }

        // Current date: 25th Jan -> Analytics packet for: 24th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun receiveNegativePCRTestResultAfterSelfDiagnosisAndEndIsolation() = notReported {
        receiveNegativeTestResultAfterSelfDiagnosisAndEndIsolation(
            LAB_RESULT,
            Metrics::receivedNegativeTestResultViaPolling
        )
    }

    @Test
    fun receiveNegativeAssistedLFDTestResultAfterSelfDiagnosisAndEndIsolation() = notReported {
        receiveNegativeTestResultAfterSelfDiagnosisAndEndIsolation(
            RAPID_RESULT,
            Metrics::receivedNegativeLFDTestResultViaPolling
        )
    }

    @Test
    fun receiveNegativeUnassistedLFDTestResultAfterSelfDiagnosisAndEndIsolation() = notReported {
        receiveNegativeTestResultAfterSelfDiagnosisAndEndIsolation(
            RAPID_SELF_REPORTED,
            Metrics::receivedNegativeLFDTestResultViaPolling
        )
    }

    private fun receiveNegativeTestResultAfterSelfDiagnosisAndEndIsolation(
        testKitType: VirologyTestKitType,
        receivedNegativeTestResultViaPollingMetric: MetricsProperty
    ) {
        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        // Starting state: App running normally, not in isolation
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms and order test on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 10th Jan
        selfDiagnosis.selfDiagnosePositiveAndOrderTest()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertEquals(1, Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        // Receive negative test result via polling
        pollingTestResult.receiveAndAcknowledgeNegativeTestResult(
            testKitType,
            this::advanceToNextBackgroundTaskExecution
        )

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            // Isolation ends part way through analytics window due to negative test result
            assertEquals(1, Metrics::receivedNegativeTestResult)
            assertEquals(1, receivedNegativeTestResultViaPollingMetric)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertLessThanTotalBackgroundTasks(Metrics::isIsolatingBackgroundTick)
            assertLessThanTotalBackgroundTasks(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        // Original reason for isolation stored until 17th
        // Dates: 5th-17th Jan -> Analytics packets for: 4th-16th Jan
        assertOnFieldsForDateRange(5..17) {
            // Isolation is over, but isolation reason still stored for 14 days
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        // Current date: 18th Jan -> Analytics packet for: 17th Jan
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun receiveVoidPCRTestResultAfterSelfDiagnosis() = notReported {
        receiveVoidTestResultAfterSelfDiagnosis(
            LAB_RESULT,
            Metrics::receivedVoidTestResultViaPolling
        )
    }

    @Test
    fun receiveVoidAssistedLFDTestResultAfterSelfDiagnosis() = notReported {
        receiveVoidTestResultAfterSelfDiagnosis(
            RAPID_RESULT,
            Metrics::receivedVoidLFDTestResultViaPolling
        )
    }

    @Test
    fun receiveVoidUnassistedLFDTestResultAfterSelfDiagnosis() = notReported {
        receiveVoidTestResultAfterSelfDiagnosis(
            RAPID_SELF_REPORTED,
            Metrics::receivedVoidLFDTestResultViaPolling
        )
    }

    private fun receiveVoidTestResultAfterSelfDiagnosis(
        testKitType: VirologyTestKitType,
        receivedVoidTestResultViaPollingMetric: MetricsProperty
    ) {
        selfDiagnosis.selfDiagnosePositiveAndOrderTest()

        assertOnFields {
            assertEquals(1, Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        pollingTestResult.receiveAndAcknowledgeVoidTestResult(
            testKitType,
            this::advanceToNextBackgroundTaskExecution
        )

        assertOnFields {
            assertEquals(1, Metrics::receivedVoidTestResult)
            assertEquals(1, receivedVoidTestResultViaPollingMetric)
            ignore(
                Metrics::hasSelfDiagnosedBackgroundTick,
                Metrics::isIsolatingBackgroundTick,
                Metrics::isIsolatingForSelfDiagnosedBackgroundTick
            )
        }
    }
}
