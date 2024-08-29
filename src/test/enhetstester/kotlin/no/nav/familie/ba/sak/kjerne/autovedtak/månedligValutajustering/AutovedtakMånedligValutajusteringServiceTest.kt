package no.nav.familie.ba.sak.kjerne.autovedtak.månedligValutajustering

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.LocalDateProvider
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.AutovedtakMånedligValutajusteringService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.familie.util.VirkedagerProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class AutovedtakMånedligValutajusteringServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val valutaKursService = mockk<ValutakursService>()
    private val localDateProvider = mockk<LocalDateProvider>()

    private val autovedtakMånedligValutajusteringService =
        AutovedtakMånedligValutajusteringService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            valutakursService = valutaKursService,
            localDateProvider = localDateProvider,
            autovedtakService = mockk(),
            snikeIKøenService = mockk(),
            behandlingService = mockk(),
            simuleringService = mockk(),
            taskRepository = mockk(),
        )

    @Test
    fun `utførMånedligValutajustering kaster Feil hvis en annen enn nåværende måned blir sendt inn`() {
        every { localDateProvider.now() } returns LocalDate.now()

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
    fun `utførMånedligValutajustering kaster IllegalStateException hvis fagsak ikke har en vedtatt behandling`() {
        every { localDateProvider.now() } returns LocalDate.now()
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null

        assertThrows<IllegalStateException> {
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
        val behandling = lagBehandling(fagsak = fagsak)

        every { localDateProvider.now() } returns LocalDate.now()
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
            assertThat(message).isEqualTo("Forsøker å utføre satsendring på ikke løpende fagsak ${fagsak.id}")
        }
    }

    @Test
    fun `utførMånedligValutajustering kaster RekjørSenereException hvis åpen behandling har status FATTER_VEDTAK`() {
        val fagsak = defaultFagsak().apply { status = FagsakStatus.LØPENDE }
        val behandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET)
        val åpenBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.FATTER_VEDTAK)
        val klokkenSeksNesteVirkedag = VirkedagerProvider.nesteVirkedag(LocalDate.now()).atTime(6, 0)

        every { localDateProvider.now() } returns LocalDate.now()
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
        val behandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET)
        val åpenBehandling = lagBehandling(fagsak = fagsak, status = behandlingStatus)
        val nå = LocalDateTime.now()

        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns nå
        every { localDateProvider.now() } returns LocalDate.now()
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
