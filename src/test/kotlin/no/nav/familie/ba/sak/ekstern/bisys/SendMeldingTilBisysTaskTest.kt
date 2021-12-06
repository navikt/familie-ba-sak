package no.nav.familie.ba.sak.ekstern.bisys

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.statistikk.producer.DefaultKafkaProducer
import no.nav.familie.ba.sak.statistikk.producer.DefaultKafkaProducer.Companion.OPPHOER_BARNETRYGD_BISYS_TOPIC
import no.nav.familie.ba.sak.task.SendMeldingTilBisysTask
import no.nav.familie.eksterne.kontrakter.bisys.OpphørBarnetrygdBisysMelding
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.ListenableFuture
import java.time.LocalDate
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendMeldingTilBisysTaskTest {

    data class Mocks(
        val behandlingService: BehandlingService,
        val kafkaProducer: DefaultKafkaProducer,
        val tilkjentYtelseRepository: TilkjentYtelseRepository,
        val kafkaResult: ListenableFuture<SendResult<String, String>>,
    )

    fun setupMocks(): Mocks {
        val behandlingServiceMock = mockk<BehandlingService>()
        val tilkjentYtelseRepositoryMock = mockk<TilkjentYtelseRepository>()
        val kafkaProducer = DefaultKafkaProducer(mockk())
        val listenableFutureMock = mockk<ListenableFuture<SendResult<String, String>>>()
        every { listenableFutureMock.addCallback(any(), any()) } just runs
        kafkaProducer.kafkaTemplate = mockk()
        kafkaProducer.kafkaAivenTemplate = mockk()
        return Mocks(behandlingServiceMock, kafkaProducer, tilkjentYtelseRepositoryMock, listenableFutureMock)
    }

    @Test
    fun `Skal send melding til Bisys hvis iverksett behandlingen har resultat OPPHØRT`() {
        val (behandlingService, kafkaProducer, tilkjentYtelseRepository, kafkaResult) = setupMocks()
        val sendMeldingTilBisysTask =
            SendMeldingTilBisysTask(behandlingService, kafkaProducer, tilkjentYtelseRepository)
        val behandling = lagBehandling(resultat = BehandlingResultat.OPPHØRT)
        val opphørFom = YearMonth.of(1990, 1)
        every { behandlingService.hent(behandling.id) } returns behandling
        every { tilkjentYtelseRepository.findByBehandlingOptional(behandling.id) } returns TilkjentYtelse(
            behandling = behandling,
            opphørFom = opphørFom,
            endretDato = LocalDate.of(1991, 1, 1),
            opprettetDato = LocalDate.of(1992, 1, 1)
        )
        val meldingSlot = slot<String>()
        every {
            kafkaProducer.kafkaAivenTemplate.send(
                OPPHOER_BARNETRYGD_BISYS_TOPIC,
                behandling.id.toString(),
                capture(meldingSlot)
            )
        } returns kafkaResult

        sendMeldingTilBisysTask.doTask(SendMeldingTilBisysTask.opprettTask(behandling.id))

        verify(exactly = 1) { kafkaProducer.kafkaAivenTemplate.send(any(), any(), any()) }
        val jsonMedling = objectMapper.readValue(meldingSlot.captured, OpphørBarnetrygdBisysMelding::class.java)
        assertThat(jsonMedling.opphørFom).isEqualTo(opphørFom)
        assertThat(jsonMedling.personident).isEqualTo(behandling.fagsak.hentAktivIdent().ident)
    }

    @Test
    fun `Skal ikke send melding til Bisys hvis iverksett behandlingen ikke har resultat OPPHØRT`() {
        val (behandlingService, kafkaProducer, tilkjentYtelseRepository, kafkaResult) = setupMocks()
        val sendMeldingTilBisysTask =
            SendMeldingTilBisysTask(behandlingService, kafkaProducer, tilkjentYtelseRepository)
        val behandling = lagBehandling(resultat = BehandlingResultat.INNVILGET)
        every { behandlingService.hent(behandling.id) } returns behandling
        every { tilkjentYtelseRepository.findByBehandlingOptional(behandling.id) } returns TilkjentYtelse(
            behandling = behandling,
            endretDato = LocalDate.of(1991, 1, 1),
            opprettetDato = LocalDate.of(1992, 1, 1)
        )
        every {
            kafkaProducer.kafkaAivenTemplate.send(
                OPPHOER_BARNETRYGD_BISYS_TOPIC, any(), any()
            )
        } returns kafkaResult

        sendMeldingTilBisysTask.doTask(SendMeldingTilBisysTask.opprettTask(behandling.id))

        verify(exactly = 0) { kafkaProducer.kafkaAivenTemplate.send(any(), any(), any()) }
    }
}
