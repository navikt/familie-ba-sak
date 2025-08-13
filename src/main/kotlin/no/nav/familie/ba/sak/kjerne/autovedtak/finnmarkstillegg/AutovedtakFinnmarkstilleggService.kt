package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.FinnmarkstilleggData
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.REVURDERING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.FINNMARKSTILLEGG
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutovedtakFinnmarkstilleggService(
    private val autovedtakService: AutovedtakService,
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val pdlRestClient: SystemOnlyPdlRestClient,
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
) : AutovedtakBehandlingService<FinnmarkstilleggData> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun skalAutovedtakBehandles(behandlingsdata: FinnmarkstilleggData): Boolean {
        val harLøpendeBarnetrygd by lazy { fagsakService.hentPåFagsakId(behandlingsdata.fagsakId).status == FagsakStatus.LØPENDE }

        val sisteIverksatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandlingsdata.fagsakId)
                ?: return false

        val sisteIverksatteBehandlingHarFinnmarkstilleggAndeler by lazy {
            beregningService
                .hentTilkjentYtelseForBehandling(sisteIverksatteBehandling.id)
                .andelerTilkjentYtelse
                .any { it.type == YtelseType.FINNMARKSTILLEGG }
        }

        val minstÉnAktørHarAdresseSomErRelevanteForFinnmarkstillegg by lazy {
            persongrunnlagService
                .hentAktivThrows(sisteIverksatteBehandling.id)
                .personer
                .map { it.aktør.aktivFødselsnummer() }
                .let { identer ->
                    pdlRestClient
                        .hentBostedsadresseOgDeltBostedForPersoner(identer)
                        .any { it.value.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg() }
                }
        }

        return harLøpendeBarnetrygd && (sisteIverksatteBehandlingHarFinnmarkstilleggAndeler || minstÉnAktørHarAdresseSomErRelevanteForFinnmarkstillegg)
    }

    @Transactional
    override fun kjørBehandling(behandlingsdata: FinnmarkstilleggData): String {
        logger.info("Kjører autovedtak for Finnmarkstillegg for fagsakId=${behandlingsdata.fagsakId}")

        val søkerAktør = fagsakService.hentAktør(behandlingsdata.fagsakId)
        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = søkerAktør,
                behandlingType = REVURDERING,
                behandlingÅrsak = FINNMARKSTILLEGG,
                fagsakId = behandlingsdata.fagsakId,
            )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat,
            )

        val task =
            when (behandlingEtterBehandlingsresultat.steg) {
                StegType.IVERKSETT_MOT_OPPDRAG -> {
                    IverksettMotOppdragTask.opprettTask(
                        behandlingEtterBehandlingsresultat,
                        opprettetVedtak,
                        SikkerhetContext.hentSaksbehandler(),
                    )
                }

                StegType.FERDIGSTILLE_BEHANDLING -> {
                    behandlingService.oppdaterStatusPåBehandling(
                        behandlingEtterBehandlingsresultat.id,
                        BehandlingStatus.IVERKSETTER_VEDTAK,
                    )
                    FerdigstillBehandlingTask.opprettTask(
                        søkerAktør.aktivFødselsnummer(),
                        behandlingEtterBehandlingsresultat.id,
                    )
                }

                else -> throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} for behandlingsårsak $FINNMARKSTILLEGG for fagsak=${behandlingsdata.fagsakId}")
            }

        taskService.save(task)

        return AutovedtakStegService.BEHANDLING_FERDIG
    }
}
