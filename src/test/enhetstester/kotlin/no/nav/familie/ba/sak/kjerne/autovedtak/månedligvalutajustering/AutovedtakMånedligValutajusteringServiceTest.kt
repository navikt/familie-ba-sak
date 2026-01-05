package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori.EØS
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.familie.util.VirkedagerProvider.nesteVirkedag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class AutovedtakMånedligValutajusteringServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val valutaKursService = mockk<ValutakursService>()
    private val clockProvider = TestClockProvider()
    private val startSatsendring = mockk<StartSatsendring>()

    private val autovedtakMånedligValutajusteringService =
        AutovedtakMånedligValutajusteringService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            valutakursService = valutaKursService,
            autovedtakService = mockk(),
            snikeIKøenService = mockk(),
            behandlingService = mockk(),
            simuleringService = mockk(),
            taskRepository = mockk(),
            startSatsendring = startSatsendring,
            clockProvider = clockProvider,
        )

    @BeforeEach
    internal fun setUp() {
        every { startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(fagsakId = any()) } returns false
    }

    @Test
    fun `utførMånedligValutajustering skal avsluttes hvis siste iverksatte behandling ikke er EØS-behandling`() {
        // Arrange
        val logger = LoggerFactory.getLogger(AutovedtakMånedligValutajusteringService::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(listAppender)

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns lagBehandling(behandlingKategori = BehandlingKategori.NASJONAL)

        // Act
        autovedtakMånedligValutajusteringService.utførMånedligValutajustering(
            fagsakId = 0,
            måned = YearMonth.now(),
        )

        // Assert
        assertThat(listAppender.list).anySatisfy {
            assertThat(it.level.toString()).isEqualTo("WARN")
            assertThat(it.formattedMessage).isEqualTo("Prøver å utføre månedlig valutajustering for nasjonal fagsak 0. Hopper ut")
        }
    }

    @Test
    fun `utførMånedligValutajustering kaster Feil hvis en annen enn nåværende måned blir sendt inn`() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns lagBehandling(behandlingKategori = EØS)
        every { valutaKursService.hentValutakurser(any()) } returns
            listOf(
                Valutakurs(
                    fom = YearMonth.now().minusYears(1),
                    tom = null,
                    vurderingsform = Vurderingsform.MANUELL,
                ),
            )

        assertThrows<Feil> {
            autovedtakMånedligValutajusteringService.utførMånedligValutajustering(
                fagsakId = 0,
                måned = YearMonth.now().minusMonths(1),
            )
        }.run {
            assertThat(message).isEqualTo("Prøver å utføre månedlig valutajustering for en annen måned enn nåværende måned.")
        }
    }

    @Test
    fun `utførMånedligValutajustering kaster Feil hvis fagsak ikke har en vedtatt behandling`() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null

        assertThrows<Feil> {
            autovedtakMånedligValutajusteringService.utførMånedligValutajustering(
                fagsakId = 0,
                måned = YearMonth.now(),
            )
        }.run {
            assertThat(message).isEqualTo("Fant ikke siste vedtatte behandling for 0")
        }
    }

    @Test
    fun `utførMånedligValutajustering kaster Feil hvis fagsakstatus ikke er løpende`() {
        val fagsak = defaultFagsak()
        val behandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns behandling
        every { valutaKursService.hentValutakurser(any()) } returns
            listOf(
                Valutakurs(
                    fom = YearMonth.now().minusMonths(1),
                    tom = null,
                    vurderingsform = Vurderingsform.AUTOMATISK,
                ),
            )

        assertThrows<Feil> {
            autovedtakMånedligValutajusteringService.utførMånedligValutajustering(
                fagsakId = fagsak.id,
                måned = YearMonth.now(),
            )
        }.run {
            assertThat(message).isEqualTo("Forsøker å utføre månedlig valutajustering på ikke løpende fagsak ${fagsak.id}")
        }
    }

    @Test
    fun `utførMånedligValutajustering kaster RekjørSenereException hvis satsendring er trigget`() {
        val fagsak = defaultFagsak().apply { status = FagsakStatus.LØPENDE }
        val behandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns behandling
        every { valutaKursService.hentValutakurser(any()) } returns
            listOf(
                Valutakurs(
                    fom = YearMonth.now().minusMonths(1),
                    tom = null,
                    vurderingsform = Vurderingsform.AUTOMATISK,
                ),
            )
        every { startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(fagsakId = any()) } returns true

        assertThrows<RekjørSenereException> {
            autovedtakMånedligValutajusteringService.utførMånedligValutajustering(
                fagsakId = fagsak.id,
                måned = YearMonth.now(),
            )
        }.run {
            assertThat(årsak).isEqualTo("Satsendring skal kjøre ferdig før man behandler månedlig valutajustering for fagsakId=${fagsak.id}")
        }
    }

    @Test
    fun `utførMånedligValutajustering kaster RekjørSenereException hvis åpen behandling har status FATTER_VEDTAK`() {
        val fagsak = defaultFagsak().apply { status = FagsakStatus.LØPENDE }
        val behandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, behandlingKategori = EØS)
        val åpenBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.FATTER_VEDTAK)
        val klokkenSeksNesteVirkedag = (1..3).fold(LocalDate.now()) { acc, _ -> nesteVirkedag(acc) }.atTime(6, 0)

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns behandling
        every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(any()) } returns åpenBehandling
        every { valutaKursService.hentValutakurser(any()) } returns
            listOf(
                Valutakurs(
                    fom = YearMonth.now().minusMonths(1),
                    tom = null,
                    vurderingsform = Vurderingsform.AUTOMATISK,
                ),
            )

        assertThrows<RekjørSenereException> {
            autovedtakMånedligValutajusteringService.utførMånedligValutajustering(
                fagsakId = fagsak.id,
                måned = YearMonth.now(),
            )
        }.run {
            assertThat(message).startsWith("Rekjører senere")
            assertThat(årsak).startsWith("Åpen behandling har status FATTER_VEDTAK")
            assertThat(triggerTid).isEqualTo(klokkenSeksNesteVirkedag)
        }
    }

    @ParameterizedTest
    @EnumSource(BehandlingStatus::class, names = ["SATT_PÅ_MASKINELL_VENT", "IVERKSETTER_VEDTAK"])
    fun `utførMånedligValutajustering kaster RekjørSenereException hvis åpen behandling har status SATT_PÅ_MASKINELL_VENT eller IVERKSETTER_VEDTAK`(
        behandlingStatus: BehandlingStatus,
    ) {
        val fagsak = defaultFagsak().apply { status = FagsakStatus.LØPENDE }
        val behandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, behandlingKategori = EØS)
        val åpenBehandling = lagBehandling(fagsak = fagsak, status = behandlingStatus)
        val nå = LocalDateTime.now()

        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns nå
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns behandling
        every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(any()) } returns åpenBehandling
        every { valutaKursService.hentValutakurser(any()) } returns
            listOf(
                Valutakurs(
                    fom = YearMonth.now().minusMonths(1),
                    tom = null,
                    vurderingsform = Vurderingsform.AUTOMATISK,
                ),
            )

        assertThrows<RekjørSenereException> {
            autovedtakMånedligValutajusteringService.utførMånedligValutajustering(
                fagsakId = fagsak.id,
                måned = YearMonth.now(),
            )
        }.run {
            assertThat(message).startsWith("Rekjører senere")
            assertThat(årsak).startsWith("Åpen behandling har status $behandlingStatus")
            assertThat(triggerTid).isEqualTo(nå.plusHours(1))
        }

        unmockkStatic(LocalDateTime::class)
    }
}
