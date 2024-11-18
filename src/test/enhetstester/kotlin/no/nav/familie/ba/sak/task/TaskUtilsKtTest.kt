package no.nav.familie.ba.sak.task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskUtilsKtTest {
    @Nested
    inner class FinnNesteTriggerTidIHverdagerForTaskTest {
        @ParameterizedTest
        @CsvSource(
            // Fridag
            "2020-05-17T00:00:00, 2020-05-18T06:00:00, Fridag 17. mai midnatt. Venter til morgenen neste virkedag kl. 06.",
            "2020-05-17T05:59:59, 2020-05-18T06:00:00, Fridag 17. mai rett før kl. 06. Venter til morgenen neste virkedag kl. 06.",
            "2020-05-17T06:00:00, 2020-05-18T06:00:00, Fridag 17. mai kl. 06. Venter til morgenen neste virkedag kl. 06.",
            "2020-05-17T06:00:01, 2020-05-18T06:00:00, Fridag 17. mai rett etter kl. 06. Venter til morgenen neste virkedag kl. 06.",
            "2020-05-17T20:59:59, 2020-05-18T06:00:00, Fridag 17. mai rett før kl. 21. Venter til morgenen neste virkedag kl. 06.",
            "2020-05-17T21:00:00, 2020-05-18T06:00:00, Fridag 17. mai kl. 21. Venter til morgenen neste virkedag kl. 06.",
            "2020-05-17T21:00:01, 2020-05-18T06:00:00, Fridag 17. mai rett etter kl. 21. Venter til morgenen neste virkedag kl. 06.",
            "2020-05-17T23:59:59, 2020-05-18T06:00:00, Fridag 17. mai rett før midnatt. Venter til morgenen neste virkedag kl. 06.",
            "2020-05-17T12:00:00, 2020-05-18T06:00:00, Fridag 17. mai kl 12. Venter til morgenen neste virkedag kl. 06.",
            "2020-05-17T15:00:00, 2020-05-18T06:00:00, Fridag 17. mai kl 15. Venter til morgenen neste virkedag kl. 06.",
            "2020-05-17T18:00:00, 2020-05-18T06:00:00, Fridag 17. mai kl 18. Venter til morgenen neste virkedag kl. 06.",
            "2020-12-25T00:00:00, 2020-12-28T06:00:00, Fridag 25. desember midnatt. Venter til morgenen neste virkedag kl. 06.",
            "2020-12-25T05:59:59, 2020-12-28T06:00:00, Fridag 25. desember rett før kl. 06. Venter til morgenen neste virkedag kl. 06.",
            "2020-12-25T06:00:00, 2020-12-28T06:00:00, Fridag 25. desember kl. 06. Venter til morgenen neste virkedag kl. 06.",
            "2020-12-25T06:00:01, 2020-12-28T06:00:00, Fridag 25. desember rett etter kl. 06. Venter til morgenen neste virkedag kl. 06.",
            "2020-12-25T20:59:59, 2020-12-28T06:00:00, Fridag 25. desember rett før kl. 21. Venter til morgenen neste virkedag kl. 06.",
            "2020-12-25T21:00:00, 2020-12-28T06:00:00, Fridag 25. desember kl. 21. Venter til morgenen neste virkedag kl. 06.",
            "2020-12-25T21:00:01, 2020-12-28T06:00:00, Fridag 25. desember rett etter kl. 21. Venter til morgenen neste virkedag kl. 06.",
            "2020-12-25T23:59:59, 2020-12-28T06:00:00, Fridag 25. desember rett før midnatt. Venter til morgenen neste virkedag kl. 06.",
            "2020-12-25T12:00:00, 2020-12-28T06:00:00, Fridag 25. desember kl 12. Venter til morgenen neste virkedag kl. 06.",
            "2020-12-25T15:00:00, 2020-12-28T06:00:00, Fridag 25. desember kl 15. Venter til morgenen neste virkedag kl. 06.",
            "2020-12-25T18:00:00, 2020-12-28T06:00:00, Fridag 25. desember kl 18. Venter til morgenen neste virkedag kl. 06.",
            // Mandag
            "2020-06-08T00:00:00, 2020-06-08T06:00:00, Mandag midnatt. Venter til morgenen kl. 06.",
            "2020-06-08T05:59:59, 2020-06-08T06:00:00, Mandag kl. 03. Venter til morgenen kl. 06.",
            "2020-06-08T05:59:59, 2020-06-08T06:00:00, Mandag rett før kl. 06. Venter til morgenen kl. 06.",
            "2020-06-08T06:00:00, 2020-06-08T06:00:00, Mandag kl. 06. Kjører med en gang.",
            "2020-06-08T06:00:01, 2020-06-08T06:00:01, Mandag rett etter kl. 06. Kjører med en gang.",
            "2020-06-08T20:59:59, 2020-06-08T20:59:59, Mandag rett før kl. 21. Kjører med en gang.",
            "2020-06-08T21:00:00, 2020-06-08T21:00:00, Mandag kl. 21. Kjører med en gang",
            "2020-06-08T21:00:01, 2020-06-09T06:00:00, Mandag rett etter kl. 21. Venter til morgenen neste virkedag kl. 06.",
            "2020-06-08T12:00:00, 2020-06-08T12:00:00, Mandag kl. 12. Kjører med en gang.",
            "2020-06-08T15:00:00, 2020-06-08T15:00:00, Mandag kl. 15. Kjører med en gang.",
            "2020-06-08T18:00:00, 2020-06-08T18:00:00, Mandag kl. 18. Kjører med en gang.",
            // Fredag
            "2020-06-12T00:00:00, 2020-06-12T06:00:00, Fredag midnatt. Venter til morgenen kl 06.",
            "2020-06-12T03:00:00, 2020-06-12T06:00:00, Fredag kl. 03. Venter til morgenen kl 06.",
            "2020-06-12T05:59:59, 2020-06-12T06:00:00, Fredag rett før kl. 06. Venter til morgenen kl 06.",
            "2020-06-12T06:00:00, 2020-06-12T06:00:00, Fredag kl. 06. Kjører med en gang.",
            "2020-06-12T06:00:01, 2020-06-12T06:00:01, Fredag rett etter kl. 06. Kjører med en gang.",
            "2020-06-12T12:00:00, 2020-06-12T12:00:00, Fredag kl. 12. Kjører med en gang.",
            "2020-06-12T15:00:00, 2020-06-12T15:00:00, Fredag kl. 15. Kjører med en gang.",
            "2020-06-12T18:00:00, 2020-06-12T18:00:00, Fredag kl. 18. Kjører med en gang.",
            "2020-06-12T20:59:59, 2020-06-12T20:59:59, Fredag rett før kl. 21. Kjører med en gang",
            "2020-06-12T21:00:00, 2020-06-12T21:00:00, Fredag kl. 21. Kjører med en gang",
            "2020-06-12T21:00:01, 2020-06-15T06:00:00, Fredag rett etter kl. 21. Venter til morgenen neste virkedag kl. 06",
            "2021-05-14T21:00:01, 2021-05-18T06:00:00, Fredag rett etter kl. 21 14. mai, mandag er 17. mai og fridag. Venter til 18. mai klokken 06",
            // Lørdag
            "2020-06-13T00:00:00, 2020-06-15T06:00:00, Lørdag midnatt. Venter til morgenen neste virkedag kl 06.",
            "2020-06-13T03:00:00, 2020-06-15T06:00:00, Lørdag kl. 03. Venter til morgenen neste virkedag kl 06.",
            "2020-06-13T05:59:59, 2020-06-15T06:00:00, Lørdag rett før kl. 06. Venter til morgenen neste virkedag kl 06.",
            "2020-06-13T06:00:00, 2020-06-15T06:00:00, Lørdag kl. 06. Venter til morgenen neste virkedag kl 06.",
            "2020-06-13T06:00:01, 2020-06-15T06:00:00, Lørdag rett etter kl. 06. Venter til morgenen neste virkedag kl 06.",
            "2020-06-13T12:00:00, 2020-06-15T06:00:00, Lørdag kl. 12. Venter til morgenen neste virkedag kl 06.",
            "2020-06-13T15:00:00, 2020-06-15T06:00:00, Lørdag kl. 15. Venter til morgenen neste virkedag kl 06.",
            "2020-06-13T18:00:00, 2020-06-15T06:00:00, Lørdag kl. 18. Venter til morgenen neste virkedag kl 06.",
            "2020-06-13T20:59:59, 2020-06-15T06:00:00, Lørdag rett før kl. 21. Venter til morgenen neste virkedag kl 06.",
            "2020-06-13T21:00:00, 2020-06-15T06:00:00, Lørdag kl. 21. Venter til morgenen neste virkedag kl. 06",
            "2020-06-13T21:00:01, 2020-06-15T06:00:00, Lørdag rett etter kl. 21. Venter til morgenen neste virkedag kl. 06",
            // Søndag
            "2020-06-14T00:00:00, 2020-06-15T06:00:00, Søndag midnatt. Venter til morgenen neste virkedag kl 06.",
            "2020-06-14T03:00:00, 2020-06-15T06:00:00, Søndag kl. 03. Venter til morgenen neste virkedag kl 06.",
            "2020-06-14T05:59:59, 2020-06-15T06:00:00, Søndag rett før kl. 06. Venter til morgenen neste virkedag kl 06.",
            "2020-06-14T06:00:00, 2020-06-15T06:00:00, Søndag kl. 06. Venter til morgenen neste virkedag kl 06.",
            "2020-06-14T06:00:01, 2020-06-15T06:00:00, Søndag rett etter kl. 06. Venter til morgenen neste virkedag kl 06.",
            "2020-06-14T12:00:00, 2020-06-15T06:00:00, Søndag kl. 12. Venter til morgenen neste virkedag kl 06.",
            "2020-06-14T15:00:00, 2020-06-15T06:00:00, Søndag kl. 15. Venter til morgenen neste virkedag kl 06.",
            "2020-06-14T18:00:00, 2020-06-15T06:00:00, Søndag kl. 18. Venter til morgenen neste virkedag kl 06.",
            "2020-06-14T20:59:59, 2020-06-15T06:00:00, Søndag rett før kl. 21. Venter til morgenen neste virkedag kl 06.",
            "2020-06-14T21:00:00, 2020-06-15T06:00:00, Søndag kl. 21. Venter til morgenen neste virkedag kl. 06",
            "2020-06-14T21:00:01, 2020-06-15T06:00:00, Søndag rett etter kl. 21. Venter til morgenen neste virkedag kl. 06",
            // Mikset
            "2020-04-08T21:00:01, 2020-04-14T06:00:00, Onsdag rett etter kl. 21 8. April. Venter til morgenen kl. 06.",
            "2020-06-09T13:37:00, 2020-06-09T13:37:00, Innenfor dagtid",
            "2020-06-09T21:37:00, 2020-06-10T06:00:00, Hverdag utenfor dagtid. Venter til dagen etter kl. 06",
            "2020-06-12T19:37:00, 2020-06-12T19:37:00, Innenfor dagtid på en fredag",
            "2020-06-12T21:37:00, 2020-06-15T06:00:00, Utenfor dagtid på en fredag. Venter til mandag morgen",
            "2020-06-13T09:37:00, 2020-06-15T06:00:00, Lørdag morgen. Venter til mandag morgen",
            "2020-06-13T19:37:00, 2020-06-15T06:00:00, Lørdag kveld. Venter til mandag morgen",
            "2020-06-14T09:37:00, 2020-06-15T06:00:00, Søndag morgen. Venter til mandag morgen",
            "2020-06-14T19:37:00, 2020-06-15T06:00:00, Søndag kveld. Venter til mandag morgen",
            "2020-06-14T21:37:00, 2020-06-15T06:00:00, Søndag etter 21. Venter til mandag morgen",
            "2020-05-17T15:37:00, 2020-05-18T06:00:00, Innenfor dagtid 17 mai. Venter til morgenen etter",
            "2020-05-17T05:37:00, 2020-05-18T06:00:00, Før dagtid 17 mai. Venter til morgenen etter",
            "2020-05-17T22:37:00, 2020-05-18T06:00:00, Etter dagtid 17 mai. Venter til morgenen etter",
            "2021-05-14T21:30:00, 2021-05-18T06:00:00, 14 mai er fredag. 17 mai er mandag og fridag. Venter til 18 mai klokken 6",
        )
        fun `skal returnere neste arbeidsdag `(
            input: LocalDateTime,
            expected: LocalDateTime,
            kommentar: String,
        ) {
            assertEquals(expected, finnNesteTriggerTidIHverdagerForTask(input))
        }

        @Test
        fun `skal returnere samme dag kl 6 for hverdag når triggertid er rett før kl 6, med forsikelse på 1 sek `() {
            // Arrange
            val triggerTid = LocalDateTime.of(LocalDate.of(2024, 11, 15), LocalTime.of(5, 59, 59))
            val forsinkelse = Duration.ofSeconds(1)
            // Act
            val nyTriggertid = finnNesteTriggerTidIHverdagerForTask(triggerTid = triggerTid, forsinkelse = forsinkelse)
            // Assert
            assertThat(nyTriggertid).isEqualTo(LocalDateTime.of(LocalDate.of(2024, 11, 15), LocalTime.of(6, 0)))
        }

        @Test
        fun `skal returnere samme dag kl 06,00,01 for hverdag når triggretid er 06,00,00, med forsikelse på 1 sek `() {
            // Arrange
            val triggerTid = LocalDateTime.of(LocalDate.of(2024, 11, 15), LocalTime.of(6, 0, 0))
            val forsinkelse = Duration.ofSeconds(1)
            // Act
            val nyTriggertid = finnNesteTriggerTidIHverdagerForTask(triggerTid = triggerTid, forsinkelse = forsinkelse)
            // Assert
            assertThat(nyTriggertid).isEqualTo(LocalDateTime.of(LocalDate.of(2024, 11, 15), LocalTime.of(6, 0, 1)))
        }

        @Test
        fun `skal returnere samme dag kl 21 når triggertid er 1 sek før kl 21 med forsinkelse på 1 sek `() {
            // Arrange
            val triggerTid = LocalDateTime.of(LocalDate.of(2024, 11, 15), LocalTime.of(20, 59, 59))
            val forsinkelse = Duration.ofSeconds(1)
            // Act
            val nyTriggertid = finnNesteTriggerTidIHverdagerForTask(triggerTid = triggerTid, forsinkelse = forsinkelse)
            // Assert
            assertThat(nyTriggertid).isEqualTo(LocalDateTime.of(LocalDate.of(2024, 11, 15), LocalTime.of(21, 0, 0)))
        }

        @Test
        fun `skal returnere neste virkedag kl 06 når triggertid er kl 21 med forsinkelse på 1 sek`() {
            // Arrange
            val triggerTid = LocalDateTime.of(LocalDate.of(2024, 11, 15), LocalTime.of(21, 0, 0))
            val forsinkelse = Duration.ofSeconds(1)
            // Act
            val nyTriggertid = finnNesteTriggerTidIHverdagerForTask(triggerTid = triggerTid, forsinkelse = forsinkelse)
            // Assert
            assertThat(nyTriggertid).isEqualTo(LocalDateTime.of(LocalDate.of(2024, 11, 18), LocalTime.of(6, 0, 0)))
        }
    }
}
