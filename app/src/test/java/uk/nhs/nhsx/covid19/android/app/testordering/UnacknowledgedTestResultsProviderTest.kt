package uk.nhs.nhsx.covid19.android.app.testordering

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED

class UnacknowledgedTestResultsProviderTest {

    private val unacknowledgedTestResultsStorage = mockk<UnacknowledgedTestResultsStorage>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-10-07T00:05:00.00Z"), ZoneOffset.UTC)
    private val moshi = Moshi.Builder().add(InstantAdapter()).build()

    private val testSubject = UnacknowledgedTestResultsProvider(
        unacknowledgedTestResultsStorage,
        fixedClock,
        moshi
    )

    @Test
    fun `add PCR test result`() {
        testSubject.add(SINGLE_RECEIVED_LAB_RESULT_TEST_RESULT)

        verify { unacknowledgedTestResultsStorage.value = SINGLE_LAB_RESULT_TEST_RESULT_JSON }
    }

    @Test
    fun `add assisted LFD test result`() {
        testSubject.add(SINGLE_RECEIVED_RAPID_RESULT_TEST_RESULT)

        verify { unacknowledgedTestResultsStorage.value = SINGLE_RAPID_RESULT_TEST_RESULT_JSON }
    }

    @Test
    fun `add unassisted LFD test result`() {
        testSubject.add(SINGLE_RECEIVED_RAPID_SELF_REPORTED_TEST_RESULT)

        verify { unacknowledgedTestResultsStorage.value = SINGLE_RAPID_SELF_REPORTED_TEST_RESULT_JSON }
    }

    @Test
    fun `remove test result`() {
        every { unacknowledgedTestResultsStorage.value } returns SINGLE_LAB_RESULT_TEST_RESULT_JSON

        testSubject.remove(SINGLE_RECEIVED_LAB_RESULT_TEST_RESULT)

        verify { unacknowledgedTestResultsStorage.value = EMPTY_JSON }
    }

    @Test
    fun `clear no test results`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        testSubject.clearBefore(LocalDate.of(1970, 1, 1))

        verify { unacknowledgedTestResultsStorage.value = MULTIPLE_TEST_RESULTS_JSON.replace("\n", "") }
    }

    @Test
    fun `clear some test results`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        testSubject.clearBefore(LocalDate.of(1971, 1, 2))

        verify {
            unacknowledgedTestResultsStorage.value =
                """[{"diagnosisKeySubmissionToken":"token3","testEndDate":"1972-01-01T00:00:00Z","testResult":"NEGATIVE","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":true}]""".trimIndent()
        }
    }

    @Test
    fun `clear all test results`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        testSubject.clearBefore(LocalDate.of(1972, 1, 2))

        verify { unacknowledgedTestResultsStorage.value = EMPTY_JSON }
    }

    @Test
    fun `hasPositiveTestResultAfter returns true if positive after the date`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        val result = testSubject.hasPositiveTestResultAfter(Instant.parse("1969-12-31T00:00:00Z"))

        assertTrue(result)
    }

    @Test
    fun `hasPositiveTestResultAfter returns false if positive on the date`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        val result = testSubject.hasPositiveTestResultAfter(Instant.parse("1970-01-01T00:00:00Z"))

        assertFalse(result)
    }

    @Test
    fun `hasPositiveTestResultAfter returns false if positive before the date`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        val result = testSubject.hasPositiveTestResultAfter(Instant.parse("1971-01-01T00:00:00Z"))

        assertFalse(result)
    }

    @Test
    fun `hasPositiveTestResultAfter returns false if no positive test`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_NONE_POSITIVE_JSON

        val result = testSubject.hasPositiveTestResultAfter(Instant.parse("1970-01-01T00:00:00Z"))

        assertFalse(result)
    }

    @Test
    fun `hasPositiveTestResultAfterOrEqual returns true if positive after the date`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        val result = testSubject.hasPositiveTestResultAfterOrEqual(Instant.parse("1969-12-31T00:00:00Z"))

        assertTrue(result)
    }

    @Test
    fun `hasPositiveTestResultAfterOrEqual returns true if positive on the date`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        val result = testSubject.hasPositiveTestResultAfterOrEqual(Instant.parse("1970-01-01T00:00:00Z"))

        assertTrue(result)
    }

    @Test
    fun `hasPositiveTestResultAfterOrEqual returns false if positive before the date`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        val result = testSubject.hasPositiveTestResultAfterOrEqual(Instant.parse("1971-01-01T00:00:00Z"))

        assertFalse(result)
    }

    @Test
    fun `hasPositiveTestResultAfterOrEqual returns false if no positive test`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_NONE_POSITIVE_JSON

        val result = testSubject.hasPositiveTestResultAfterOrEqual(Instant.parse("1970-01-01T00:00:00Z"))

        assertFalse(result)
    }

    @Test
    fun `with storage`() {
        every { unacknowledgedTestResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        val receivedTestResults = testSubject.testResults

        val expectedResult = listOf(
            ReceivedTestResult(
                "token1",
                Instant.parse("1970-01-01T00:00:00Z"),
                POSITIVE,
                RAPID_SELF_REPORTED,
                diagnosisKeySubmissionSupported = false
            ),
            ReceivedTestResult(
                "token2",
                Instant.parse("1971-01-01T00:00:00Z"),
                VOID,
                RAPID_RESULT,
                diagnosisKeySubmissionSupported = true
            ),
            ReceivedTestResult(
                "token3",
                Instant.parse("1972-01-01T00:00:00Z"),
                NEGATIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true
            )
        )

        assertEquals(expectedResult, receivedTestResults)
    }

    @Test
    fun `with empty storage`() {
        every { unacknowledgedTestResultsStorage.value } returns null

        val receivedTestResults = testSubject.testResults

        assertEquals(emptyList(), receivedTestResults)
    }

    @Test
    fun `with corrupt storage`() {
        every { unacknowledgedTestResultsStorage.value } returns "sdsfljghsfgyldfjg"

        val receivedTestResults = testSubject.testResults

        assertEquals(emptyList(), receivedTestResults)
    }

    companion object {
        val SINGLE_LAB_RESULT_TEST_RESULT_JSON =
            """
            [{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":false}]
            """.trimIndent()

        val SINGLE_RAPID_RESULT_TEST_RESULT_JSON =
            """
            [{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"RAPID_RESULT","diagnosisKeySubmissionSupported":false}]
            """.trimIndent()

        val SINGLE_RAPID_SELF_REPORTED_TEST_RESULT_JSON =
            """
            [{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"RAPID_SELF_REPORTED","diagnosisKeySubmissionSupported":false}]
            """.trimIndent()

        val MULTIPLE_TEST_RESULTS_JSON =
            """
            [
            {"diagnosisKeySubmissionToken":"token1","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"RAPID_SELF_REPORTED","diagnosisKeySubmissionSupported":false},
            {"diagnosisKeySubmissionToken":"token2","testEndDate":"1971-01-01T00:00:00Z","testResult":"VOID","testKitType":"RAPID_RESULT","diagnosisKeySubmissionSupported":true},
            {"diagnosisKeySubmissionToken":"token3","testEndDate":"1972-01-01T00:00:00Z","testResult":"NEGATIVE","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":true}
            ]
            """.trimIndent()

        val MULTIPLE_TEST_RESULTS_NONE_POSITIVE_JSON =
            """
            [
            {"diagnosisKeySubmissionToken":"token2","testEndDate":"1971-01-01T00:00:00Z","testResult":"VOID","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":true},
            {"diagnosisKeySubmissionToken":"token3","testEndDate":"1972-01-01T00:00:00Z","testResult":"NEGATIVE","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":true}
            ]
            """.trimIndent()

        val EMPTY_JSON =
            """
            []
            """.trimIndent()

        val SINGLE_RECEIVED_LAB_RESULT_TEST_RESULT = ReceivedTestResult(
            "token",
            Instant.ofEpochMilli(0),
            POSITIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = false
        )

        val SINGLE_RECEIVED_RAPID_RESULT_TEST_RESULT = ReceivedTestResult(
            "token",
            Instant.ofEpochMilli(0),
            POSITIVE,
            RAPID_RESULT,
            diagnosisKeySubmissionSupported = false
        )

        val SINGLE_RECEIVED_RAPID_SELF_REPORTED_TEST_RESULT = ReceivedTestResult(
            "token",
            Instant.ofEpochMilli(0),
            POSITIVE,
            RAPID_SELF_REPORTED,
            diagnosisKeySubmissionSupported = false
        )
    }
}
