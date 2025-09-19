package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.SvalbardtilleggData
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.REVURDERING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.SVALBARDTILLEGG
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus.LØPENDE
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.BARN_ENSLIG_MINDREÅRIG
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.NORMAL
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

val FAGSAKTYPER_DER_SVALBARDTILLEGG_KAN_AUTOVEDTAS = setOf(NORMAL, BARN_ENSLIG_MINDREÅRIG)

@Service
class AutovedtakSvalbardtilleggService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val beregningService: BeregningService,
    private val pdlRestClient: SystemOnlyPdlRestClient,
    private val autovedtakService: AutovedtakService,
    private val simuleringService: SimuleringService,
    private val behandlingService: BehandlingService,
    private val taskService: TaskService,
) : AutovedtakBehandlingService<SvalbardtilleggData> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun skalAutovedtakBehandles(behandlingsdata: SvalbardtilleggData): Boolean {
        val fagsak = fagsakService.hentPåFagsakId(behandlingsdata.fagsakId)
        val fagsaktypeKanBehandles = fagsak.type in FAGSAKTYPER_DER_SVALBARDTILLEGG_KAN_AUTOVEDTAS
        val harLøpendeBarnetrygd = fagsak.status == LØPENDE

        if (!fagsaktypeKanBehandles || !harLøpendeBarnetrygd) return false

        val sisteIverksatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandlingsdata.fagsakId)
                ?: return false

        val sisteIverksatteBehandlingHarSvalbardtilleggAndeler =
            beregningService
                .hentTilkjentYtelseForBehandling(sisteIverksatteBehandling.id)
                .andelerTilkjentYtelse
                .any { it.type == YtelseType.SVALBARDTILLEGG }

        val minstÈnAktørHarAdresseSomErRelevantForSvalbardtillegg =
            persongrunnlagService
                .hentAktivThrows(sisteIverksatteBehandling.id)
                .personer
                .map { it.aktør.aktivFødselsnummer() }
                .let { identer ->
                    pdlRestClient
                        .hentBostedsadresseDeltBostedOgOppholdsadresseForPersoner(identer)
                        .mapValues { Adresser.opprettFra(it.value) }
                        .any { it.value.harAdresserSomErRelevantForSvalbardtillegg() }
                }

        return sisteIverksatteBehandlingHarSvalbardtilleggAndeler || minstÈnAktørHarAdresseSomErRelevantForSvalbardtillegg
    }

    @Transactional
    override fun kjørBehandling(behandlingsdata: SvalbardtilleggData): String {
        logger.info("Kjører autovedtak for Svalbardtillegg for fagsak=${behandlingsdata.fagsakId}")

        val søkerAktør = fagsakService.hentAktør(behandlingsdata.fagsakId)
        val behandlingEtterBehandlingsresultat =
            autovedtakService
                .opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                    aktør = søkerAktør,
                    behandlingType = REVURDERING,
                    behandlingÅrsak = SVALBARDTILLEGG,
                    fagsakId = behandlingsdata.fagsakId,
                )

        simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingEtterBehandlingsresultat.id)

        val feilutbetaling = simuleringService.hentFeilutbetaling(behandlingEtterBehandlingsresultat.id)

        if (feilutbetaling > BigDecimal.ZERO) {
            throw Feil("Det er oppdaget feilutbetaling ved kjøring av svalbardtillegg for fagsakId=${behandlingsdata.fagsakId}. Automatisk kjøring stoppes.")
        }

        if (behandlingEtterBehandlingsresultat.steg == StegType.IVERKSETT_MOT_OPPDRAG) {
            // TODO: Implementer AutovedtakSvalbardtilleggBegrunnelseService
        }

        val opprettVedtak = autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandlingEtterBehandlingsresultat)

        val task =
            when (behandlingEtterBehandlingsresultat.steg) {
                StegType.IVERKSETT_MOT_OPPDRAG -> {
                    IverksettMotOppdragTask.opprettTask(
                        behandlingEtterBehandlingsresultat,
                        opprettVedtak,
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

                else -> throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} for behandlingsårsak $SVALBARDTILLEGG for fagsak=${behandlingsdata.fagsakId}")
            }

        taskService.save(task)

        return AutovedtakStegService.BEHANDLING_FERDIG
    }
}
