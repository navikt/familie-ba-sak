package no.nav.familie.ba.sak.saksstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.journalføring.JournalføringService
import no.nav.familie.ba.sak.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaksstatistikkServiceTest {


    private val behandlingService: BehandlingService = mockk()
    private val journalføringRepository: JournalføringRepository  = mockk()
    private val journalføringService: JournalføringService  = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService  = mockk()
    private val totrinnskontrollService: TotrinnskontrollService = mockk(relaxed = true)

    private val vedtakService: VedtakService = mockk()

    private val sakstatistikkService = SaksstatistikkService(behandlingService,
                                                             journalføringRepository,
                                                             journalføringService,
                                                             arbeidsfordelingService,
                                                             totrinnskontrollService,
                                                             vedtakService)

    private val behandling = lagBehandling(opprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE)

    private val vedtak = lagVedtak(behandling)

    @BeforeAll
    fun init() {

        every { behandlingService.hent(any()) } returns behandling
        // every { journalføringRepository.findByBehandlingId(any()) }

        every { arbeidsfordelingService.bestemBehandlendeEnhet(any()) } returns "4820"
        every { arbeidsfordelingService.hentBehandlendeEnhet(any()) } returns listOf(Arbeidsfordelingsenhet("4821", "NAV"))

        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
//        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(behandling = behandling,
//                                                                                                 saksbehandler = "saksbehandler",
//                                                                                                 beslutter = "beslutter")

    }

    @Test
    fun loggBehandlingStatus() {

        val behandlingDvh = sakstatistikkService.loggBehandlingStatus(2, 1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))

        assertThat(behandlingDvh.funksjonellTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh.tekniskTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh.mottattDato).isEqualTo(ZonedDateTime.of(behandling.opprettetTidspunkt, SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh.registrertDato).isEqualTo(ZonedDateTime.of(behandling.opprettetTidspunkt, SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh.vedtaksDato).isEqualTo(vedtak.vedtaksdato)
        assertThat(behandlingDvh.behandlingId).isEqualTo(behandling.id.toString())
        assertThat(behandlingDvh.relatertBehandlingId).isEqualTo("1")
        assertThat(behandlingDvh.sakId).isEqualTo(behandling.fagsak.id.toString())
        assertThat(behandlingDvh.vedtakId).isEqualTo(vedtak.id.toString())
        assertThat(behandlingDvh.behandlingType).isEqualTo(behandling.type.name)
        assertThat(behandlingDvh.behandlingStatus).isEqualTo(behandling.status.name)
        assertThat(behandlingDvh.totrinnsbehandling).isFalse()
        assertThat(behandlingDvh.saksbehandler).isEmpty()
        assertThat(behandlingDvh.beslutter).isEmpty()
        assertThat(behandlingDvh.avsender).isEqualTo("familie-ba-sak")
        assertThat(behandlingDvh.versjon).isNotEmpty()

    }

}