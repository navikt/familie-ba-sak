package no.nav.familie.ba.sak.saksstatistikk

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import no.nav.familie.ba.sak.journalføring.JournalføringService
import no.nav.familie.ba.sak.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class SaksstatistikkService(private val behandlingService: BehandlingService,
                            private val journalføringRepository: JournalføringRepository,
                            private val journalføringService: JournalføringService,
                            private val arbeidsfordelingService: ArbeidsfordelingService,
                            private val totrinnskontrollService: TotrinnskontrollService,
                            private val vedtakService: VedtakService) {

    fun loggBehandlingStatus(behandlingId: Long, forrigeBehandlingId: Long?): BehandlingDVH {
        val behandling = behandlingService.hent(behandlingId)


        var datoMottatt: LocalDateTime
        when (behandling.opprinnelse) {
            BehandlingOpprinnelse.MANUELL -> {
            val journalpost = journalføringRepository.findByBehandlingId(behandlingId)
                datoMottatt = journalpost.mapNotNull { journalføringService.hentJournalpost(it.journalpostId).data }
                                      .filter { it.journalposttype == Journalposttype.I }
                                      .filter { it.tittel != null && it.tittel!!.contains("søknad", ignoreCase = true) }
                                      .mapNotNull { it.datoMottatt }
                                      .minOrNull() ?: behandling.opprettetTidspunkt  //Skal man kaste feil eller ikke
            }
            BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE -> {
                datoMottatt = behandling.opprettetTidspunkt
            }
            else -> error("Statistikkhåndtering for behandling med opprinnelse ${behandling.opprinnelse.name} ikke implementert.")
        }

        val behandlendeEnhetsKode = arbeidsfordelingService.bestemBehandlendeEnhet(behandling)
        val ansvarligEnhetKode = arbeidsfordelingService.hentBehandlendeEnhet(behandling.fagsak).firstOrNull()?.enhetId

        val aktivtVedtak = vedtakService.hentAktivForBehandling(behandlingId)
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId)

        val now = ZonedDateTime.now()
        val behandlingDVH = BehandlingDVH(funksjonellTid = now,
                                          tekniskTid = now, //now()
                                          mottattDato = datoMottatt.atZone(TIMEZONE), // TODO hva er dato Mottat for manuell, automatisk fra hendelse, automatisk fra journalpost. Vi trekker fra 2 dager på skanning
                                          registrertDato = datoMottatt.atZone(TIMEZONE),
                                          behandlingId = behandling.id.toString(),
                                          sakId = behandling.fagsak.id.toString(),
                                          behandlingType = behandling.type.name,
                                          behandlingStatus = behandling.status.name,
                                          utenlandstilsnitt = "NASJONAL", /* TODO Kode som beskriver behandlingens  utlandstilsnitt i henhold til NAV spesialisering.
                                              I hoved sak vil denne koden beskrive om saksbehandlingsfrister er i henhold til utlandssaker
                                              eller innlandssaker, men vil for mange kildesystem være angitt med en høyere oppløsning."

                                              https://confluence.adeo.no/display/DVH/Fellesdimensjoner+-+Utenlandstilsnitt
                                              */

                                          ansvarligEnhetKode = ansvarligEnhetKode!!,
                                          behandlendeEnhetKode = behandlendeEnhetsKode, //der hvor saksbehandler hører hjemme //TODO Hva gjør vi hvis behandlendeEnhetsKode ikke finnes?
                                          ansvarligEnhetType = "NORG",
                                          behandlendeEnhetType = "NORG",
                                          totrinnsbehandling = behandling.opprinnelse == BehandlingOpprinnelse.MANUELL,
                                          avsender = "familie-ba-sak",
                                          versjon = hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: "2",
                // Ikke påkrevde felt
                                          vedtaksDato = aktivtVedtak?.vedtaksdato,
                                          relatertBehandlingId = forrigeBehandlingId?.toString(), // Kan en behandling være avsluttet, men ikke i status TEKNISK_OPPHØR eller AVSLUTTET, kan man bruke siste deaktiverte? kan man bruke forrigeVedtakId og så finne behandling
                                          vedtakId = aktivtVedtak?.id?.toString(),
                                          resultat = aktivtVedtak?.hentUtbetalingBegrunnelse(behandlingId)?.resultat?.name,
                                          resultatBegrunnelse = aktivtVedtak?.hentUtbetalingBegrunnelse(behandlingId)?.behandlingresultatOgVilkårBegrunnelse?.name,
                                          behandlingTypeBeskrivelse = behandling.type.visningsnavn,
//                                          behandlingStatusBeskrivelse = null, //har ingen beskrivelse
                                          resultatBegrunnelseBeskrivelse = aktivtVedtak?.hentUtbetalingBegrunnelse(behandlingId)?.behandlingresultatOgVilkårBegrunnelse?.tittel, // Denne er feil
//                                          utenlandstilsnittBeskrivelse = "relatertButenlandstilsnittBeskrivelseehandlingId",
                                          beslutter = totrinnskontroll?.beslutter,  // TODO skal den være annerledes ved automatisk behandling eller kan vi sette null/tom string
                                          saksbehandler = totrinnskontroll?.saksbehandler,
                                          behandlingOpprettetAv = behandling.opprettetAv,
                                          behandlingOpprettetType = "saksbehandlerId",
                                          behandlingOpprettetTypeBeskrivelse = "saksbehandlerId. VL ved automatisk behandling"
        )
        return behandlingDVH
    }

    companion object {

        val TIMEZONE = ZoneId.of("Europe/Paris")
    }
}