package no.nav.familie.ba.sak.saksstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.vedtak.UtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import no.nav.familie.ba.sak.journalføring.JournalføringService
import no.nav.familie.ba.sak.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_NAVN
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaksstatistikkServiceTest {


    private val behandlingService: BehandlingService = mockk()
    private val journalføringRepository: JournalføringRepository = mockk()
    private val journalføringService: JournalføringService = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()
    private val totrinnskontrollService: TotrinnskontrollService = mockk()

    private val vedtakService: VedtakService = mockk()

    private val sakstatistikkService = SaksstatistikkService(
            behandlingService,
            journalføringRepository,
            journalføringService,
            arbeidsfordelingService,
            totrinnskontrollService,
            vedtakService)


    @BeforeAll
    fun init() {
        every { arbeidsfordelingService.hentAbeidsfordelingPåBehandling(any()) } returns ArbeidsfordelingPåBehandling(
                behandlendeEnhetId = "4820",
                behandlendeEnhetNavn = "Nav",
                behandlingId = 1)
        every { arbeidsfordelingService.hentArbeidsfordelingsenhet(any()) } returns Arbeidsfordelingsenhet("4821", "NAV")
    }

    @Test
    fun `Skal mappe til behandlingDVH for Automatisk rute`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE, automatiskOpprettelse = true)
        val vedtak = lagVedtak(behandling)
        every { behandlingService.hent(any()) } returns behandling
        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(
                saksbehandler = SYSTEM_NAVN,
                beslutter = SYSTEM_NAVN,
                godkjent = true,
                behandling = behandling
        )

        val behandlingDvh = sakstatistikkService.loggBehandlingStatus(2, 1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))

        assertThat(behandlingDvh.funksjonellTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh.tekniskTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh.mottattDato).isEqualTo(ZonedDateTime.of(behandling.opprettetTidspunkt,
                                                                         SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh.registrertDato).isEqualTo(ZonedDateTime.of(behandling.opprettetTidspunkt,
                                                                            SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh.vedtaksDato).isEqualTo(vedtak.vedtaksdato)
        assertThat(behandlingDvh.behandlingId).isEqualTo(behandling.id.toString())
        assertThat(behandlingDvh.relatertBehandlingId).isEqualTo("1")
        assertThat(behandlingDvh.sakId).isEqualTo(behandling.fagsak.id.toString())
        assertThat(behandlingDvh.vedtakId).isEqualTo(vedtak.id.toString())
        assertThat(behandlingDvh.behandlingType).isEqualTo(behandling.type.name)
        assertThat(behandlingDvh.behandlingKategori).isEqualTo(behandling.kategori.name)
        assertThat(behandlingDvh.behandlingUnderkategori).isEqualTo(behandling.underkategori.name)
        assertThat(behandlingDvh.behandlingStatus).isEqualTo(behandling.status.name)
        assertThat(behandlingDvh.totrinnsbehandling).isFalse
        assertThat(behandlingDvh.saksbehandler).isNull()
        assertThat(behandlingDvh.beslutter).isNull()
        assertThat(behandlingDvh.avsender).isEqualTo("familie-ba-sak")
        assertThat(behandlingDvh.versjon).isNotEmpty

    }

    @Test
    fun `Skal mappe til behandlingDVH for manuell rute`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)

        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(
                saksbehandler = "Saksbehandler",
                beslutter = "Beslutter",
                godkjent = true,
                behandling = behandling
        )

        val vedtak = lagVedtak(behandling).also {
            it.utbetalingBegrunnelser.add(
                    UtbetalingBegrunnelse(vedtak = it,
                                          fom = LocalDate.now(),
                                          tom = LocalDate.now(),
                                          begrunnelseType = VedtakBegrunnelseType.INNVILGELSE,
                                          vedtakBegrunnelse = VedtakBegrunnelse.INNVILGET_BOR_HOS_SØKER))
        }


        every { behandlingService.hent(any()) } returns behandling
        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { journalføringRepository.findByBehandlingId(any()) } returns listOf(DbJournalpost(1,
                                                                                                 "foo",
                                                                                                 LocalDateTime.now(),
                                                                                                 behandling,
                                                                                                 "123"))
        val mottattDato = LocalDateTime.of(2019, 12, 20, 10, 0, 0)
        val jp = lagTestJournalpost("123", "123").copy(relevanteDatoer = listOf(RelevantDato(mottattDato, "DATO_REGISTRERT")))
        every { journalføringService.hentJournalpost(any()) } returns Ressurs.Companion.success(jp)

        val behandlingDvh = sakstatistikkService.loggBehandlingStatus(2, 1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))

        assertThat(behandlingDvh.funksjonellTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh.tekniskTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh.mottattDato).isEqualTo(mottattDato.atZone(SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh.registrertDato).isEqualTo(mottattDato.atZone(SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh.vedtaksDato).isEqualTo(vedtak.vedtaksdato)
        assertThat(behandlingDvh.behandlingId).isEqualTo(behandling.id.toString())
        assertThat(behandlingDvh.relatertBehandlingId).isEqualTo("1")
        assertThat(behandlingDvh.sakId).isEqualTo(behandling.fagsak.id.toString())
        assertThat(behandlingDvh.vedtakId).isEqualTo(vedtak.id.toString())
        assertThat(behandlingDvh.behandlingType).isEqualTo(behandling.type.name)
        assertThat(behandlingDvh.behandlingStatus).isEqualTo(behandling.status.name)
        assertThat(behandlingDvh.totrinnsbehandling).isTrue
        assertThat(behandlingDvh.saksbehandler).isNull()
        assertThat(behandlingDvh.beslutter).isNull()
        assertThat(behandlingDvh.resultatBegrunnelser).hasSize(1)
                .extracting("resultatBegrunnelse")
                .containsOnly("INNVILGET_BOR_HOS_SØKER")
        assertThat(behandlingDvh.avsender).isEqualTo("familie-ba-sak")
        assertThat(behandlingDvh.versjon).isNotEmpty

    }

}