package no.nav.familie.ba.sak.saksstatistikk

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.journalføring.JournalføringService
import no.nav.familie.ba.sak.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_NAVN
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.ResultatBegrunnelseDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class SaksstatistikkService(private val behandlingService: BehandlingService,
                            private val journalføringRepository: JournalføringRepository,
                            private val journalføringService: JournalføringService,
                            private val arbeidsfordelingService: ArbeidsfordelingService,
                            private val totrinnskontrollService: TotrinnskontrollService,
                            private val vedtakService: VedtakService,
                            private val fagsakService: FagsakService,
                            private val personopplysningerService: PersonopplysningerService,
                            private val featureToggleService: FeatureToggleService) {

    fun mapTilBehandlingDVH(behandlingId: Long, forrigeBehandlingId: Long? = null): BehandlingDVH? {
        val behandling = behandlingService.hent(behandlingId)

        if (behandling.skalBehandlesAutomatisk && fødselshendelseSkalRullesTilbake()) return null

        val datoMottatt = when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.SØKNAD -> {
                val journalpost = journalføringRepository.findByBehandlingId(behandlingId)
                journalpost.mapNotNull { journalføringService.hentJournalpost(it.journalpostId).data }
                        .filter { it.journalposttype == Journalposttype.I }
                        .filter { it.tittel != null && it.tittel!!.contains("søknad", ignoreCase = true) }
                        .mapNotNull { it.datoMottatt }
                        .minOrNull() ?: behandling.opprettetTidspunkt
            }
            BehandlingÅrsak.FØDSELSHENDELSE -> {
                behandling.opprettetTidspunkt
            }
            BehandlingÅrsak.TEKNISK_OPPHØR -> {
                behandling.opprettetTidspunkt
            }
            else -> error("Statistikkhåndtering for behandling med opprinnelse ${behandling.opprettetÅrsak.name} ikke implementert.")
        }

        val behandlendeEnhetsKode = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId).behandlendeEnhetId
        val ansvarligEnhetKode = arbeidsfordelingService.hentArbeidsfordelingsenhet(behandling).enhetId

        val aktivtVedtak = vedtakService.hentAktivForBehandling(behandlingId)
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId)

        val now = ZonedDateTime.now()
        val behandlingDVH = BehandlingDVH(funksjonellTid = now,
                                          tekniskTid = now, // TODO burde denne vært satt til opprettetTidspunkt/endretTidspunkt?
                                          mottattDato = datoMottatt.atZone(TIMEZONE),
                                          registrertDato = datoMottatt.atZone(TIMEZONE),
                                          behandlingId = behandling.id.toString(),
                                          sakId = behandling.fagsak.id.toString(),
                                          behandlingType = behandling.type.name,
                                          behandlingStatus = behandling.status.name,
                                          behandlingKategori = behandling.kategori.name,
                                          behandlingUnderkategori = behandling.underkategori.name,
                                          utenlandstilsnitt = "NASJONAL",
                                          ansvarligEnhetKode = ansvarligEnhetKode,
                                          behandlendeEnhetKode = behandlendeEnhetsKode,
                                          ansvarligEnhetType = "NORG",
                                          behandlendeEnhetType = "NORG",
                                          totrinnsbehandling = totrinnskontroll?.saksbehandler != SYSTEM_NAVN,
                                          avsender = "familie-ba-sak",
                                          versjon = hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: "2",
                // Ikke påkrevde felt
                                          vedtaksDato = aktivtVedtak?.vedtaksdato,
                                          relatertBehandlingId = forrigeBehandlingId?.toString(),
                                          vedtakId = aktivtVedtak?.id?.toString(),
                                          resultat = aktivtVedtak?.hentUtbetalingBegrunnelse(behandlingId)?.begrunnelseType?.name,
                                          behandlingTypeBeskrivelse = behandling.type.visningsnavn,
                                          resultatBegrunnelser = aktivtVedtak?.utbetalingBegrunnelser?.mapNotNull { it.vedtakBegrunnelse }
                                                  ?.map { ResultatBegrunnelseDVH(it.name, it.tittel) }
                                                  .orEmpty(),
                                          behandlingOpprettetAv = behandling.opprettetAv,
                                          behandlingOpprettetType = "saksbehandlerId",
                                          behandlingOpprettetTypeBeskrivelse = "saksbehandlerId. VL ved automatisk behandling"
        )
        if (totrinnskontroll != null) {
            behandlingDVH.copy(beslutter = totrinnskontroll.beslutter,
                               saksbehandler = totrinnskontroll.saksbehandler)
        }

        return behandlingDVH
    }

    fun mapTilSakDvh(sakId: Long): SakDVH? {
        val fagsak = fagsakService.hentRestFagsak(sakId).getDataOrThrow()

        //Skipper saker som har automatisk behandling
        val skalBehandleAutomatisk = behandlingService.hentAktivForFagsak(fagsakId = fagsak.id)?.skalBehandlesAutomatisk ?: false
        if (skalBehandleAutomatisk && fødselshendelseSkalRullesTilbake()) return null

        val søkersAktørId = personopplysningerService.hentAktivAktørId(Ident(fagsak.søkerFødselsnummer))

        val deltagere = fagsakService.hentFagsakDeltager(fagsak.søkerFødselsnummer).filter { it.fagsakId == sakId }
                .map { AktørDVH(personopplysningerService.hentAktivAktørId(Ident(it.ident)).id.toLong(), it.rolle.name) }

        return SakDVH(
                funksjonellTid = ZonedDateTime.now(),
                tekniskTid = ZonedDateTime.now(),
                opprettetDato = LocalDate.now(),
                sakId = sakId.toString(),
                aktorId = søkersAktørId.id.toLong(),
                aktorer = deltagere,
                sakStatus = fagsak.status.name,
                avsender = "familie-ba-sak",
                versjon = hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: "2",
        )
    }


    private fun fødselshendelseSkalRullesTilbake() : Boolean =
            featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")

    companion object {

        val TIMEZONE = ZoneId.of("Europe/Paris")
    }
}