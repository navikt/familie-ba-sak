package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDto
import no.nav.familie.ba.sak.ekstern.restDomene.MinimalFagsakDto
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.ekstern.restDomene.VisningBehandlingDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.domene.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
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
import java.util.Properties

fun generellAssertUtvidetBehandlingDto(
    utvidetBehandlingDto: Ressurs<UtvidetBehandlingDto>,
    behandlingStatus: BehandlingStatus,
    behandlingStegType: StegType? = null,
    behandlingsresultat: Behandlingsresultat? = null,
) {
    if (utvidetBehandlingDto.status != Ressurs.Status.SUKSESS) {
        throw Feil("generellAssertutvidetBehandlingDto feilet. status: ${utvidetBehandlingDto.status.name},  melding: ${utvidetBehandlingDto.melding}")
    }

    assertEquals(behandlingStatus, utvidetBehandlingDto.data?.status)

    if (behandlingStegType != null) {
        assertEquals(behandlingStegType, utvidetBehandlingDto.data?.steg)
    }

    if (behandlingsresultat != null) {
        assertEquals(behandlingsresultat, utvidetBehandlingDto.data?.resultat)
    }
}

fun generellAssertFagsak(
    fagsakDto: Ressurs<FagsakDto>,
    fagsakStatus: FagsakStatus,
    behandlingStegType: StegType? = null,
    behandlingsresultat: Behandlingsresultat? = null,
    aktivBehandlingId: Long? = null,
) {
    if (fagsakDto.status != Ressurs.Status.SUKSESS) throw Feil("generellAssertFagsak feilet. status: ${fagsakDto.status.name},  melding: ${fagsakDto.melding}")
    assertEquals(fagsakStatus, fagsakDto.data?.status)

    val aktivBehandling =
        if (aktivBehandlingId == null) {
            hentAktivBehandling(fagsakDto = fagsakDto.data!!)
        } else {
            fagsakDto.data!!.behandlinger.single { it.behandlingId == aktivBehandlingId }
        }

    if (behandlingStegType != null) {
        assertEquals(behandlingStegType, aktivBehandling.steg)
    }
    if (behandlingsresultat != null) {
        assertEquals(behandlingsresultat, aktivBehandling.resultat)
    }
}

fun assertUtbetalingsperiode(
    utbetalingsperiode: Utbetalingsperiode,
    antallBarn: Int,
    utbetaltPerMnd: Int,
) {
    assertEquals(antallBarn, utbetalingsperiode.utbetalingsperiodeDetaljer.size)
    assertEquals(utbetaltPerMnd, utbetalingsperiode.utbetaltPerMnd)
}

fun hentNåværendeEllerNesteMånedsUtbetaling(behandling: UtvidetBehandlingDto): Int {
    val utbetalingsperioder =
        behandling.utbetalingsperioder.sortedBy { it.periodeFom }
    val nåværendeUtbetalingsperiode =
        utbetalingsperioder
            .firstOrNull { it.periodeFom.isSameOrBefore(LocalDate.now()) && it.periodeTom.isAfter(LocalDate.now()) }

    val nesteUtbetalingsperiode = utbetalingsperioder.firstOrNull { it.periodeFom.isAfter(LocalDate.now()) }

    return nåværendeUtbetalingsperiode?.utbetaltPerMnd ?: nesteUtbetalingsperiode?.utbetaltPerMnd ?: 0
}

fun hentAktivBehandling(fagsakDto: FagsakDto): UtvidetBehandlingDto = fagsakDto.behandlinger.single()

fun hentAktivBehandling(minimalFagsakDto: MinimalFagsakDto): VisningBehandlingDto = minimalFagsakDto.behandlinger.single { it.aktiv }

fun behandleFødselshendelse(
    nyBehandlingHendelse: NyBehandlingHendelse,
    fagsakStatusEtterVurdering: FagsakStatus = FagsakStatus.OPPRETTET,
    behandleFødselshendelseTask: BehandleFødselshendelseTask,
    fagsakService: FagsakService,
    behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    personidentService: PersonidentService,
    vedtakService: VedtakService,
    stegService: StegService,
    brevmalService: BrevmalService,
): Behandling? {
    val søkerFnr = nyBehandlingHendelse.morsIdent
    val søkerAktør = personidentService.hentAktør(søkerFnr)

    behandleFødselshendelseTask.doTask(
        BehandleFødselshendelseTask.opprettTask(
            BehandleFødselshendelseTaskDTO(
                nyBehandling = nyBehandlingHendelse,
            ),
        ),
    )

    val minimalFagsakDtoEtterVurdering = fagsakService.hentMinimalFagsakForPerson(aktør = søkerAktør)
    if (minimalFagsakDtoEtterVurdering.status != Ressurs.Status.SUKSESS) {
        return null
    }

    val behandlingEtterVurdering =
        behandlingHentOgPersisterService
            .hentBehandlinger(fagsakId = minimalFagsakDtoEtterVurdering.data!!.id)
            .maxByOrNull { it.opprettetTidspunkt }!!
    if (behandlingEtterVurdering.erHenlagt()) {
        return behandlingEtterVurdering
    }

    generellAssertFagsak(
        fagsakDto = fagsakService.hentFagsakDto(minimalFagsakDtoEtterVurdering.data!!.id),
        fagsakStatus = fagsakStatusEtterVurdering,
        behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG,
        aktivBehandlingId = behandlingEtterVurdering.id,
    )

    return håndterIverksettingAvBehandling(
        behandlingEtterVurdering = behandlingEtterVurdering,
        søkerFnr = søkerFnr,
        fagsakService = fagsakService,
        vedtakService = vedtakService,
        stegService = stegService,
        brevmalService = brevmalService,
    )
}

fun håndterIverksettingAvBehandling(
    behandlingEtterVurdering: Behandling,
    søkerFnr: String,
    fagsakStatusEtterIverksetting: FagsakStatus = FagsakStatus.LØPENDE,
    fagsakService: FagsakService,
    vedtakService: VedtakService,
    stegService: StegService,
    brevmalService: BrevmalService,
): Behandling {
    val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterVurdering.id)
    val behandlingEtterIverksetteVedtak =
        stegService.håndterIverksettMotØkonomi(
            behandlingEtterVurdering,
            IverksettingTaskDTO(
                behandlingsId = behandlingEtterVurdering.id,
                vedtaksId = vedtak.id,
                saksbehandlerId = "System",
                personIdent = behandlingEtterVurdering.fagsak.aktør.aktivFødselsnummer(),
            ),
        )

    val behandlingEtterStatusFraOppdrag =
        stegService.håndterStatusFraØkonomi(
            behandlingEtterIverksetteVedtak,
            StatusFraOppdragMedTask(
                statusFraOppdragDTO =
                    StatusFraOppdragDTO(
                        fagsystem = FAGSYSTEM,
                        personIdent = søkerFnr,
                        behandlingsId = behandlingEtterIverksetteVedtak.id,
                        vedtaksId = vedtak.id,
                    ),
                task = Task(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = ""),
            ),
        )

    val behandlingEtterIverksettTilbakekreving =
        if (behandlingEtterStatusFraOppdrag.steg == StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) {
            stegService.håndterIverksettMotFamilieTilbake(
                behandling = behandlingEtterStatusFraOppdrag,
                metadata = Properties(),
            )
        } else {
            behandlingEtterStatusFraOppdrag
        }

    val behandlingSomSkalFerdigstilles =
        if (behandlingEtterIverksettTilbakekreving.steg == StegType.JOURNALFØR_VEDTAKSBREV) {
            val behandlingEtterJournalførtVedtak =
                stegService.håndterJournalførVedtaksbrev(
                    behandlingEtterStatusFraOppdrag,
                    JournalførVedtaksbrevDTO(
                        vedtakId = vedtak.id,
                        task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = ""),
                    ),
                )

            val behandlingEtterDistribuertVedtak =
                stegService.håndterDistribuerVedtaksbrev(
                    behandlingEtterJournalførtVedtak,
                    DistribuerDokumentDTO(
                        behandlingId = behandlingEtterJournalførtVedtak.id,
                        journalpostId = "1234",
                        fagsakId = behandlingEtterJournalførtVedtak.fagsak.id,
                        brevmal =
                            brevmalService.hentBrevmal(
                                behandlingEtterJournalførtVedtak,
                            ),
                        erManueltSendt = false,
                    ),
                )
            behandlingEtterDistribuertVedtak
        } else {
            behandlingEtterStatusFraOppdrag
        }

    val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandlingSomSkalFerdigstilles)

    val minimalFagsakDtoEtterAvsluttetBehandling =
        fagsakService.hentMinimalFagsakForPerson(aktør = ferdigstiltBehandling.fagsak.aktør)
    generellAssertFagsak(
        fagsakDto = fagsakService.hentFagsakDto(minimalFagsakDtoEtterAvsluttetBehandling.data!!.id),
        fagsakStatus = fagsakStatusEtterIverksetting,
        behandlingStegType = StegType.BEHANDLING_AVSLUTTET,
        aktivBehandlingId =
            hentAktivBehandling(
                minimalFagsakDtoEtterAvsluttetBehandling.data!!,
            ).behandlingId,
    )

    return ferdigstiltBehandling
}
