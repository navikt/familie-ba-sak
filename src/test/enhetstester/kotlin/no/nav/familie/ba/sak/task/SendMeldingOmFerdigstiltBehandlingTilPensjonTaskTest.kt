package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.ekstern.pensjon.kafka.FerdigstiltBehandlingHendelse
import no.nav.familie.ba.sak.ekstern.pensjon.kafka.PensjonKafkaProducer
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.dto.SendMeldingOmFerdigstiltBehandlingTilPensjonDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class SendMeldingOmFerdigstiltBehandlingTilPensjonTaskTest {
    private val vedtakService = mockk<VedtakService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val pensjonKafkaProducer = mockk<PensjonKafkaProducer>()
    private val sendMeldingOmFerdigstiltBehandlingTilPensjonTask =
        SendMeldingOmFerdigstiltBehandlingTilPensjonTask(
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
            pensjonKafkaProducer = pensjonKafkaProducer,
        )

    @Nested
    inner class OpprettTask {
        @Test
        fun `skal opprette task med riktig type, payload og properties`() {
            // Arrange
            val personIdent = "12345678901"
            val behandlingsId = 123L

            val forventetDto = SendMeldingOmFerdigstiltBehandlingTilPensjonDTO(behandlingsId, personIdent)

            // Act
            val task = SendMeldingOmFerdigstiltBehandlingTilPensjonTask.opprettTask(personIdent, behandlingsId)

            // Assert
            assertThat(task.type).isEqualTo(SendMeldingOmFerdigstiltBehandlingTilPensjonTask.TASK_STEP_TYPE)
            assertThat(task.payload).isEqualTo(jsonMapper.writeValueAsString(forventetDto))
            assertThat(task.metadata["personIdent"]).isEqualTo(personIdent)
            assertThat(task.metadata["behandlingId"]).isEqualTo(behandlingsId.toString())
        }
    }

    @Nested
    inner class DoTask {
        @Test
        fun `skal sende hendelse til pensjon med vedtakstidspunkt og behandlingens endringstidspunkt`() {
            // Arrange
            val personIdent = "12345678901"
            val behandlingsId = 123L
            val vedtaktidspunkt = LocalDateTime.of(2026, 10, 8, 12, 0)
            val endringstidspunkt = LocalDate.of(2026, 10, 8)

            val task = SendMeldingOmFerdigstiltBehandlingTilPensjonTask.opprettTask(personIdent, behandlingsId)

            val hendelseSlot = slot<FerdigstiltBehandlingHendelse>()

            every { vedtakService.hentAktivForBehandlingThrows(behandlingsId) } returns lagVedtak(vedtaksdato = vedtaktidspunkt)
            every { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingsId) } returns endringstidspunkt
            every { pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingTilPensjon(capture(hendelseSlot)) } returns Unit

            val forventetHendelse = FerdigstiltBehandlingHendelse(personIdent, vedtaktidspunkt, endringstidspunkt)

            // Act
            sendMeldingOmFerdigstiltBehandlingTilPensjonTask.doTask(task)

            // Assert
            assertThat(hendelseSlot.captured).isEqualTo(forventetHendelse)
            verify(exactly = 1) { vedtakService.hentAktivForBehandlingThrows(behandlingsId) }
            verify(exactly = 1) { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingsId) }
            verify(exactly = 1) {
                pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingTilPensjon(forventetHendelse)
            }
        }

        @Test
        fun `skal kaste feil dersom vedtaket mangler vedtaksdato`() {
            // Arrange
            val behandlingsId = 123L
            val task = SendMeldingOmFerdigstiltBehandlingTilPensjonTask.opprettTask("12345678901", behandlingsId)

            every { vedtakService.hentAktivForBehandlingThrows(behandlingsId) } returns lagVedtak(vedtaksdato = null)

            // Act & Assert
            val feil = assertThrows<Feil> { sendMeldingOmFerdigstiltBehandlingTilPensjonTask.doTask(task) }

            assertThat(feil.message).isEqualTo("Fant ikke vedtaksdato for vedtak for behandling=$behandlingsId")
            verify(exactly = 0) { pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingTilPensjon(any()) }
        }
    }
}
