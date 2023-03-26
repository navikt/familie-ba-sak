package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.YearMonth

internal class StartSatsendringTest {

    private val fagsakRepository: FagsakRepository = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService =
        mockk()
    private val satskjøringRepository: SatskjøringRepository = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val personidentService: PersonidentService = mockk()
    private val autovedtakSatsendringService: AutovedtakSatsendringService = mockk()
    private val satsendringService: SatsendringService = mockk()

    private lateinit var startSatsendring: StartSatsendring

    @BeforeEach
    fun setUp() {
        val satsSlot = slot<Satskjøring>()
        every { satskjøringRepository.save(capture(satsSlot)) } answers { satsSlot.captured }
        every { behandlingRepository.findByFagsakAndAktivAndOpen(any()) } returns null
        val taskRepository: TaskRepositoryWrapper = mockk()
        val taskSlot = slot<Task>()
        every { taskRepository.save(capture(taskSlot)) } answers { taskSlot.captured }
        val opprettTaskService = OpprettTaskService(taskRepository, satskjøringRepository)
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_SJEKK_UTBETALING, true) } returns false
        every { satsendringService.erFagsakOppdatertMedSisteSatser(any()) } returns true

        startSatsendring = spyk(
            StartSatsendring(
                fagsakRepository = fagsakRepository,
                behandlingRepository = behandlingRepository,
                opprettTaskService = opprettTaskService,
                andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
                satskjøringRepository = satskjøringRepository,
                featureToggleService = featureToggleService,
                personidentService = personidentService,
                autovedtakSatsendringService = autovedtakSatsendringService,
                satsendringService = satsendringService
            )
        )
    }

    @Test
    fun `start satsendring og opprett satsendringtask på sak hvis toggler er på `() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_ENABLET, false) } returns true
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER) } returns true

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns PageImpl(
            listOf(behandling.fagsak),
            Pageable.ofSize(5),
            0
        )

        every { behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsak.id) } returns behandling

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandling.behandlingId
            )
        } returns
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    YearMonth.of(2022, 12),
                    YearMonth.of(2039, 11),
                    ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    YearMonth.of(2030, 12),
                    YearMonth.of(2039, 11),
                    ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1054
                )
            )

        startSatsendring.startSatsendring(5)

        verify(exactly = 1) { satskjøringRepository.save(any()) }
    }

    @Test
    fun `Ikke start satsendring på sak hvis ytelsen utløper før satstidspunkt, men marker at sastsendring alt er kjørt`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER) } returns true

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns PageImpl(
            listOf(behandling.fagsak),
            Pageable.ofSize(5),
            0
        )

        every { behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsak.id) } returns behandling

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandling.behandlingId
            )
        } returns
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2022, 12),
                    tom = YearMonth.of(2023, 2),
                    ytelseType = ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                )
            )

        startSatsendring.startSatsendring(5)

        val satskjøringSlot = slot<Satskjøring>()
        verify(exactly = 1) { satskjøringRepository.save(capture(satskjøringSlot)) }
        assertThat(satskjøringSlot.captured.fagsakId).isEqualTo(behandling.fagsak.id)
        assertThat(satskjøringSlot.captured.fagsakId).isEqualTo(behandling.fagsak.id)
        assertThat(satskjøringSlot.captured.ferdigTidspunkt).isEqualTo(behandling.endretTidspunkt)
    }

    @Test
    fun `finnLøpendeFagsaker har totalt antall sider 3, så den skal kalle finnLøpendeFagsaker 3 ganger for å få 5 satsendringer`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { featureToggleService.isEnabled(any()) } returns true
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_SJEKK_UTBETALING, true) } returns false

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns PageImpl(
            listOf(behandling.fagsak, behandling.fagsak),
            Pageable.ofSize(2), // 5/2 gir totalt 3 sider, så finnLøpendeFagsakerForSatsendring skal trigges 3 ganger
            5
        )

        every { behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsak.id) } returns behandling

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandling.behandlingId
            )
        } returns
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2022, 12),
                    tom = YearMonth.of(2040, 12),
                    ytelseType = ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                )
            )

        startSatsendring.startSatsendring(5)

        verify(exactly = 5) { satskjøringRepository.save(any()) }
        verify(exactly = 3) { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) }
    }

    @Test
    fun `Ikke start satsendring på sak som har åpen behandling`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER) } returns true

        val behandling = lagBehandling()

        every { behandlingRepository.findByFagsakAndAktivAndOpen(any()) } returns behandling

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns PageImpl(
            listOf(behandling.fagsak),
            Pageable.ofSize(5),
            0
        )

        startSatsendring.startSatsendring(5)

        verify(exactly = 0) { satskjøringRepository.save(any()) }
    }

    @Test
    fun `kanStarteSatsendringPåFagsak gir false når vi ikke har noen tidligere behandling`() {
        every { behandlingRepository.finnSisteIverksatteBehandling(1L) } returns null
        every { satskjøringRepository.findByFagsakId(1L) } returns Satskjøring(fagsakId = 1L)

        assertFalse(startSatsendring.kanStarteSatsendringPåFagsak(1L))
    }

    @Test
    fun `kanStarteSatsendringPåFagsak gir false når vi har en satskjøring for fagsaken i satskjøringsrepoet`() {
        every { behandlingRepository.finnSisteIverksatteBehandling(1L) } returns lagBehandling()
        every { satskjøringRepository.findByFagsakId(1L) } returns Satskjøring(fagsakId = 1L)

        assertFalse(startSatsendring.kanStarteSatsendringPåFagsak(1L))
    }

    @Test
    fun `kanStarteSatsendringPåFagsak gir false når harSisteSats er true`() {
        every { behandlingRepository.finnSisteIverksatteBehandling(1L) } returns lagBehandling()
        every { satskjøringRepository.findByFagsakId(1L) } returns null
        every { satsendringService.erFagsakOppdatertMedSisteSatser(any()) } returns true

        assertFalse(startSatsendring.kanStarteSatsendringPåFagsak(1L))
    }

    @Test
    fun `kanStarteSatsendringPåFagsak gir true når harSisteSats er false`() {
        every { behandlingRepository.finnSisteIverksatteBehandling(1L) } returns lagBehandling()
        every { satskjøringRepository.findByFagsakId(1L) } returns null
        every { satsendringService.erFagsakOppdatertMedSisteSatser(any()) } returns false

        assertTrue(startSatsendring.kanStarteSatsendringPåFagsak(1L))
    }

    @Test
    fun `kanGjennomføreSatsendringManuelt gir false når vi ikke har noen tidligere behandling`() {
        every { behandlingRepository.finnSisteIverksatteBehandling(1L) } returns null

        assertFalse(startSatsendring.kanGjennomføreSatsendringManuelt(1L))
    }

    @Test
    fun `kanGjennomføreSatsendringManuelt gir false når harSisteSats er true`() {
        every { behandlingRepository.finnSisteIverksatteBehandling(1L) } returns lagBehandling()
        every { satskjøringRepository.findByFagsakId(1L) } returns null
        every { satsendringService.erFagsakOppdatertMedSisteSatser(any()) } returns true

        assertFalse(startSatsendring.kanGjennomføreSatsendringManuelt(1L))
    }

    @Test
    fun `opprettSatsendringSynkrontVedGammelSats skal kaste dersom man ikke kan starte satsendring`() {
        every { startSatsendring.kanStarteSatsendringPåFagsak(any()) } returns false

        assertThrows<Exception> {
            startSatsendring.gjennomførSatsendringManuelt(0L)
        }
    }

    @Test
    fun `kanGjennomføreSatsendringManuelt skal kaste feil for alle andre resultater enn OK`() {
        every { startSatsendring.kanGjennomføreSatsendringManuelt(any()) } returns true

        val satsendringSvar = SatsendringSvar.values()

        satsendringSvar.forEach {
            every { autovedtakSatsendringService.kjørBehandling(any()) } returns it

            when (it) {
                SatsendringSvar.SATSENDRING_KJØRT_OK -> assertDoesNotThrow {
                    startSatsendring.gjennomførSatsendringManuelt(0L)
                }

                else -> assertThrows<Exception> {
                    startSatsendring.gjennomførSatsendringManuelt(0L)
                }
            }
        }
    }
}
