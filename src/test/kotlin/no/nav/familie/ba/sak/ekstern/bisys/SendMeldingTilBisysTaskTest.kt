package no.nav.familie.ba.sak.ekstern.bisys

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.statistikk.producer.DefaultKafkaProducer
import no.nav.familie.ba.sak.statistikk.producer.DefaultKafkaProducer.Companion.OPPHOER_BARNETRYGD_BISYS_TOPIC
import no.nav.familie.ba.sak.task.EndretType
import no.nav.familie.ba.sak.task.SendMeldingTilBisysTask
import no.nav.familie.eksterne.kontrakter.bisys.OpphørBarnetrygdBisysMelding
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.ListenableFuture
import java.math.BigDecimal
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
            SendMeldingTilBisysTask(behandlingService, kafkaProducer, tilkjentYtelseRepository, mockk())
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
            SendMeldingTilBisysTask(behandlingService, kafkaProducer, tilkjentYtelseRepository, mockk())
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

    @Test
    fun `finnEndretPerioder() skal return riktig perioder som er endret`() {
        val behandlingRepositoryMock = mockk<BehandlingRepository>()
        val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()

        val sendMeldingTilBisysTask =
            SendMeldingTilBisysTask(mockk(), mockk(), tilkjentYtelseRepository, behandlingRepositoryMock)

        val forrigeBehandling = lagBehandling(defaultFagsak(), førsteSteg = StegType.BEHANDLING_AVSLUTTET)
        val nyBehandling = lagBehandling(forrigeBehandling.fagsak)

        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val barn3 = lagPerson(type = PersonType.BARN)

        every { behandlingRepositoryMock.finnIverksatteBehandlinger(any()) } returns listOf(
            forrigeBehandling,
            nyBehandling
        )
        every { tilkjentYtelseRepository.findByBehandling(forrigeBehandling.id) } returns lagInitiellTilkjentYtelse().also {
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1), tom = YearMonth.of(2037, 12), prosent = BigDecimal(100),
                    person = barn1
                )
            )
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1), tom = YearMonth.of(2037, 12), prosent = BigDecimal(100),
                    person = barn2
                )
            )
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2019, 1), tom = YearMonth.of(2036, 12), prosent = BigDecimal(100),
                    person = barn3
                )
            )
        }
        every { tilkjentYtelseRepository.findByBehandling(nyBehandling.id) } returns lagInitiellTilkjentYtelse().also {
            // Barn1 opphør fra 04/2022
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1), tom = YearMonth.of(2022, 3), prosent = BigDecimal(100),
                    person = barn1
                )
            )

            // Barn2 redusert fra 02/2026
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1), tom = YearMonth.of(2026, 1), prosent = BigDecimal(100),
                    person = barn2
                )
            )
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2026, 2), tom = YearMonth.of(2037, 12), prosent = BigDecimal(50),
                    person = barn2
                )
            )

            // Barn3 redusert fra 04/2019 og opphørt fra 10/2019
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2019, 1), tom = YearMonth.of(2019, 4), prosent = BigDecimal(100),
                    person = barn3
                )
            )
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2019, 5), tom = YearMonth.of(2019, 9), prosent = BigDecimal(50),
                    person = barn3
                )
            )
        }

        val endretPerioder = sendMeldingTilBisysTask.finnEndretPerioder(nyBehandling)
        val barn1Perioder = endretPerioder[barn1.personIdent.ident]
        val barn2Perioder = endretPerioder[barn2.personIdent.ident]
        val barn3Perioder = endretPerioder[barn3.personIdent.ident]

        assertThat(barn1Perioder).hasSize(1)
        assertThat(barn1Perioder!![0].type).isEqualTo(EndretType.OPPHØRT)
        assertThat(barn1Perioder!![0].fom).isEqualTo(YearMonth.of(2022, 4))
        assertThat(barn1Perioder!![0].tom).isEqualTo(YearMonth.of(2037, 12))

        assertThat(barn2Perioder).hasSize(1)
        assertThat(barn2Perioder!![0].type).isEqualTo(EndretType.REDUSERT)
        assertThat(barn2Perioder!![0].fom).isEqualTo(YearMonth.of(2026, 2))
        assertThat(barn2Perioder!![0].tom).isEqualTo(YearMonth.of(2037, 12))

        assertThat(barn3Perioder).hasSize(2)

        val barn3PeriodeOpphør = barn3Perioder!!.first { it.type == EndretType.OPPHØRT }
        assertThat(barn3PeriodeOpphør.type).isEqualTo(EndretType.OPPHØRT)
        assertThat(barn3PeriodeOpphør.fom).isEqualTo(YearMonth.of(2019, 10))
        assertThat(barn3PeriodeOpphør.tom).isEqualTo(YearMonth.of(2036, 12))

        val barn3PeriodeReduser = barn3Perioder!!.first { it.type == EndretType.REDUSERT }
        assertThat(barn3PeriodeReduser.type).isEqualTo(EndretType.REDUSERT)
        assertThat(barn3PeriodeReduser.fom).isEqualTo(YearMonth.of(2019, 5))
        assertThat(barn3PeriodeReduser.tom).isEqualTo(YearMonth.of(2019, 9))
    }
}
