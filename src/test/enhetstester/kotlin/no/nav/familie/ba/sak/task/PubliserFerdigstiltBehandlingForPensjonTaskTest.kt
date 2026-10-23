package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagFerdigstiltBehandlingForPensjon
import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjonRepository
import no.nav.familie.ba.sak.ekstern.pensjon.kafka.FerdigstiltBehandlingForPensjonPayload
import no.nav.familie.ba.sak.ekstern.pensjon.kafka.PensjonKafkaProducer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional

class PubliserFerdigstiltBehandlingForPensjonTaskTest {
    private val repository = mockk<FerdigstiltBehandlingForPensjonRepository>()
    private val pensjonKafkaProducer = mockk<PensjonKafkaProducer>()
    private val publiserTask = PubliserFerdigstiltBehandlingForPensjonTask(repository, pensjonKafkaProducer)

    @Nested
    inner class OpprettTask {
        @Test
        fun `skal opprette task med sekvensnummer som payload`() {
            // Arrange
            val sekvensnummer = 42L

            // Act
            val task = PubliserFerdigstiltBehandlingForPensjonTask.opprettTask(sekvensnummer)

            // Assert
            assertThat(task.type).isEqualTo(PubliserFerdigstiltBehandlingForPensjonTask.TASK_STEP_TYPE)
            assertThat(task.payload).isEqualTo("42")
        }
    }

    @Nested
    inner class DoTask {
        @Test
        fun `skal publisere lagret ferdigstilt behandling med samme sekvensnummer ved nye forsøk`() {
            // Arrange
            val ferdigstiltBehandling = lagFerdigstiltBehandlingForPensjon(sekvensnummer = 42L)
            val task = PubliserFerdigstiltBehandlingForPensjonTask.opprettTask(ferdigstiltBehandling.sekvensnummer)
            val forventetPayload =
                FerdigstiltBehandlingForPensjonPayload(
                    ident = ferdigstiltBehandling.ident,
                    vedtaktidspunkt = ferdigstiltBehandling.vedtaktidspunkt,
                    endringstidspunkt = ferdigstiltBehandling.endringstidspunkt,
                    sekvensnummer = 42L,
                )
            every { repository.findById(ferdigstiltBehandling.sekvensnummer) } returns Optional.of(ferdigstiltBehandling)
            every { pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingForPensjon(any()) } returns Unit

            publiserTask.doTask(task)

            // Act
            publiserTask.doTask(task)

            // Assert
            verify(exactly = 2) { pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingForPensjon(forventetPayload) }
            verify(exactly = 0) { repository.save(any()) }
        }

        @Test
        fun `skal kaste feil dersom ferdigstilt behandling ikke finnes`() {
            // Arrange
            val task = PubliserFerdigstiltBehandlingForPensjonTask.opprettTask(42L)
            every { repository.findById(42L) } returns Optional.empty()

            // Act & Assert
            val feil = assertThrows<Feil> { publiserTask.doTask(task) }

            assertThat(feil.message).isEqualTo("Fant ikke ferdigstilt behandling for pensjon med sekvensnummer=42")
            verify(exactly = 0) { pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingForPensjon(any()) }
        }

        @Test
        fun `skal la Kafka-feil feile tasken slik at den kan forsøkes på nytt`() {
            // Arrange
            val ferdigstiltBehandling = lagFerdigstiltBehandlingForPensjon()
            val task = PubliserFerdigstiltBehandlingForPensjonTask.opprettTask(ferdigstiltBehandling.sekvensnummer)
            val kafkaFeil = IllegalStateException("Kafka nede")
            every { repository.findById(ferdigstiltBehandling.sekvensnummer) } returns Optional.of(ferdigstiltBehandling)
            every { pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingForPensjon(any()) } throws kafkaFeil

            // Act & Assert
            assertThat(assertThrows<IllegalStateException> { publiserTask.doTask(task) }).isSameAs(kafkaFeil)
        }
    }
}
