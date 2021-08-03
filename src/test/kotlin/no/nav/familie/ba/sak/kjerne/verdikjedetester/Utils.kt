package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate


fun generellAssertFagsak(restFagsak: Ressurs<RestFagsak>,
                         fagsakStatus: FagsakStatus,
                         behandlingStegType: StegType? = null,
                         behandlingResultat: BehandlingResultat? = null) {
    if (restFagsak.status != Ressurs.Status.SUKSESS) throw IllegalStateException("generellAssertFagsak feilet. status: ${restFagsak.status.name},  melding: ${restFagsak.melding}")
    assertEquals(fagsakStatus, restFagsak.data?.status)
    if (behandlingStegType != null) {
        assertEquals(behandlingStegType, hentAktivBehandling(restFagsak = restFagsak.data!!)?.steg)
    }
    if (behandlingResultat != null) {
        assertEquals(behandlingResultat, hentAktivBehandling(restFagsak = restFagsak.data!!)?.resultat)
    }
}

fun assertUtbetalingsperiode(utbetalingsperiode: Utbetalingsperiode, antallBarn: Int, utbetaltPerMnd: Int) {
    assertEquals(antallBarn, utbetalingsperiode.utbetalingsperiodeDetaljer.size)
    assertEquals(utbetaltPerMnd, utbetalingsperiode.utbetaltPerMnd)
}

fun hentNåværendeEllerNesteMånedsUtbetaling(behandling: RestUtvidetBehandling): Int {
    val utbetalingsperioder =
            behandling.utbetalingsperioder.sortedBy { it.periodeFom }
    val nåværendeUtbetalingsperiode = utbetalingsperioder
            .firstOrNull { it.periodeFom.isBefore(LocalDate.now()) && it.periodeTom.isAfter(LocalDate.now()) }

    val nesteUtbetalingsperiode = utbetalingsperioder.firstOrNull { it.periodeFom.isAfter(LocalDate.now()) }

    return nåværendeUtbetalingsperiode?.utbetaltPerMnd ?: nesteUtbetalingsperiode?.utbetaltPerMnd ?: 0
}

fun hentAktivBehandling(restFagsak: RestFagsak): RestUtvidetBehandling? {
    return restFagsak.behandlinger.firstOrNull { it.aktiv }
}

fun hentAktivtVedtak(restFagsak: RestFagsak): RestVedtak? {
    return hentAktivBehandling(restFagsak)?.vedtakForBehandling?.firstOrNull { it.aktiv }
}

fun behandleFødselshendelse(
        nyBehandlingHendelse: NyBehandlingHendelse,
        fagsakStatusEtterVurdering: FagsakStatus = FagsakStatus.OPPRETTET,
        behandleFødselshendelseTask: BehandleFødselshendelseTask,
        fagsakService: FagsakService,
        behandlingService: BehandlingService,
        vedtakService: VedtakService,
        stegService: StegService,
): Behandling? {
    val søkerFnr = nyBehandlingHendelse.morsIdent

    behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(
            nyBehandling = nyBehandlingHendelse
    )))

    val restFagsakEtterVurdering = fagsakService.hentRestFagsakForPerson(personIdent = PersonIdent(søkerFnr))
    if (restFagsakEtterVurdering.status != Ressurs.Status.SUKSESS) {
        return null
    }

    val behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = restFagsakEtterVurdering.data!!.id)!!
    if (behandlingEtterVurdering.erHenlagt()) {
        return behandlingEtterVurdering
    }

    generellAssertFagsak(restFagsak = restFagsakEtterVurdering,
                         fagsakStatus = fagsakStatusEtterVurdering,
                         behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG)

    return håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingEtterVurdering,
            søkerFnr = søkerFnr,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
    )
}

fun håndterIverksettingAvBehandling(
        behandlingEtterVurdering: Behandling,
        søkerFnr: String,
        skalJournalføre: Boolean = true,
        fagsakStatusEtterIverksetting: FagsakStatus = FagsakStatus.LØPENDE,
        fagsakService: FagsakService,
        vedtakService: VedtakService,
        stegService: StegService): Behandling {

    val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterVurdering.id)
    val behandlingEtterIverksetteVedtak =
            stegService.håndterIverksettMotØkonomi(behandlingEtterVurdering, IverksettingTaskDTO(
                    behandlingsId = behandlingEtterVurdering.id,
                    vedtaksId = vedtak.id,
                    saksbehandlerId = "System",
                    personIdent = søkerFnr
            ))

    val behandlingEtterStatusFraOppdrag =
            stegService.håndterStatusFraØkonomi(behandlingEtterIverksetteVedtak, StatusFraOppdragMedTask(
                    statusFraOppdragDTO = StatusFraOppdragDTO(fagsystem = FAGSYSTEM,
                                                              personIdent = søkerFnr,
                                                              behandlingsId = behandlingEtterIverksetteVedtak.id,
                                                              vedtaksId = vedtak.id),
                    task = Task.nyTask(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = "")
            ))

    val behandlingSomSkalFerdigstilles = if (skalJournalføre) {
        val behandlingEtterJournalførtVedtak =
                stegService.håndterJournalførVedtaksbrev(behandlingEtterStatusFraOppdrag, JournalførVedtaksbrevDTO(
                        vedtakId = vedtak.id,
                        task = Task.nyTask(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = "")
                ))

        val behandlingEtterDistribuertVedtak =
                stegService.håndterDistribuerVedtaksbrev(behandlingEtterJournalførtVedtak,
                                                         DistribuerVedtaksbrevDTO(behandlingId = behandlingEtterJournalførtVedtak.id,
                                                                                  journalpostId = "1234",
                                                                                  personIdent = søkerFnr))
        behandlingEtterDistribuertVedtak
    } else behandlingEtterStatusFraOppdrag


    val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandlingSomSkalFerdigstilles)

    val restFagsakEtterAvsluttetBehandling = fagsakService.hentRestFagsakForPerson(personIdent = PersonIdent(søkerFnr))
    generellAssertFagsak(restFagsak = restFagsakEtterAvsluttetBehandling,
                         fagsakStatus = fagsakStatusEtterIverksetting,
                         behandlingStegType = StegType.BEHANDLING_AVSLUTTET)

    return ferdigstiltBehandling
}