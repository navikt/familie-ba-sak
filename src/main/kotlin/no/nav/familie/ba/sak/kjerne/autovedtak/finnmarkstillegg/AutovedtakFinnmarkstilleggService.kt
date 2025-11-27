package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.FinnmarkstilleggData
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.REVURDERING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.FINNMARKSTILLEGG
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus.LØPENDE
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.BARN_ENSLIG_MINDREÅRIG
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.NORMAL
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
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

val FAGSAKTYPER_DER_FINNMARKSTILLEG_KAN_AUTOVEDTAS = setOf(NORMAL, BARN_ENSLIG_MINDREÅRIG)

@Service
class AutovedtakFinnmarkstilleggService(
    private val autovedtakService: AutovedtakService,
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val pdlRestKlient: SystemOnlyPdlRestKlient,
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val simuleringService: SimuleringService,
    private val autovedtakFinnmarkstilleggBegrunnelseService: AutovedtakFinnmarkstilleggBegrunnelseService,
) : AutovedtakBehandlingService<FinnmarkstilleggData> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun skalAutovedtakBehandles(behandlingsdata: FinnmarkstilleggData): Boolean {
        val fagsak = fagsakService.hentPåFagsakId(behandlingsdata.fagsakId)
        val fagsaktypeKanBehandles = fagsak.type in FAGSAKTYPER_DER_FINNMARKSTILLEG_KAN_AUTOVEDTAS
        val harLøpendeBarnetrygd = fagsak.status == LØPENDE

        if (!fagsaktypeKanBehandles || !harLøpendeBarnetrygd) return false

        val sisteVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandlingsdata.fagsakId)
                ?: return false

        val sisteVedtatteBehandlingHarFinnmarkstilleggAndeler =
            beregningService
                .hentTilkjentYtelseForBehandling(sisteVedtatteBehandling.id)
                .andelerTilkjentYtelse
                .any { it.type == YtelseType.FINNMARKSTILLEGG }

        val minstÉnAktørHarAdresseSomErRelevanteForFinnmarkstillegg =
            persongrunnlagService
                .hentAktivThrows(sisteVedtatteBehandling.id)
                .personer
                .map { it.aktør.aktivFødselsnummer() }
                .let { identer ->
                    pdlRestKlient
                        .hentBostedsadresseOgDeltBostedForPersoner(identer)
                        .mapValues { Adresser.opprettFra(it.value) }
                        .any { it.value.harAdresserSomErRelevantForFinnmarkstillegg() }
                }

        val skalBehandleFinnmarkstillegg =
            sisteVedtatteBehandlingHarFinnmarkstilleggAndeler || minstÉnAktørHarAdresseSomErRelevanteForFinnmarkstillegg

        if (skalBehandleFinnmarkstillegg && sisteVedtatteBehandling.kategori == BehandlingKategori.EØS) {
            throw AutovedtakMåBehandlesManueltFeil("Automatisk behandling av Finnmarkstillegg kan ikke gjennomføres for EØS-saker.\nRett til Finnmarkstillegg må håndteres manuelt.")
        }

        return skalBehandleFinnmarkstillegg
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

        simuleringService.oppdaterSimuleringPåBehandling(behandlingEtterBehandlingsresultat)

        val feilutbetaling = simuleringService.hentFeilutbetaling(behandlingEtterBehandlingsresultat.id)
        if (feilutbetaling > BigDecimal.ZERO) {
            throw AutovedtakMåBehandlesManueltFeil("Automatisk behandling av Finnmarkstillegg fører til feilutbetaling.\nEndring av Finnmarkstillegg må håndteres manuelt.")
        }

        if (behandlingEtterBehandlingsresultat.steg == StegType.IVERKSETT_MOT_OPPDRAG) {
            autovedtakFinnmarkstilleggBegrunnelseService.begrunnAutovedtakForFinnmarkstillegg(
                behandlingEtterBehandlingsresultat,
            )
        }

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

                else -> {
                    throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} for behandlingsårsak $FINNMARKSTILLEGG for fagsak=${behandlingsdata.fagsakId}")
                }
            }

        taskService.save(task)

        return AutovedtakStegService.BEHANDLING_FERDIG
    }
}
