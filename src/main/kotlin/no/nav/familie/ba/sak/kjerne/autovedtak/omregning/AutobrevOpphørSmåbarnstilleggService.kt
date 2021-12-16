package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutobrevOpphørSmåbarnstilleggService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val autovedtakService: AutovedtakService,
    private val periodeOvergangsstønadGrunnlagRepository: PeriodeOvergangsstønadGrunnlagRepository
) {
    @Transactional
    fun kjørBehandlingOgSendBrevForOpphørAvSmåbarnstillegg(behandlingId: Long) {

        val behandling = behandlingService.hent(behandlingId)

        val personopplysningGrunnlag: PersonopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)
                ?: throw FunksjonellFeil(
                    melding = "personopplysningGrunnlag er null for behandlingId: $behandlingId",
                )

        val listePeriodeOvergangsstønadGrunnlag: List<PeriodeOvergangsstønadGrunnlag> =
            periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(behandlingId)

        val harBarnUnderTreÅr = harBarnUnderTreÅr(personopplysningGrunnlag = personopplysningGrunnlag)
        if (harBarnUnderTreÅr) {
            logger.info(
                "Fagsak ${behandling.fagsak.id} har fortsatt barn under tre år. " +
                    "Avbryter sending av autobrev for opphør av småbarnstillegg."
            )
            return
        }

        val minsteBarnFylteTreÅrForrigeMåned =
            minsteBarnFylteTreÅrForrigeMåned(personopplysningGrunnlag = personopplysningGrunnlag)

        val overgangstønadOpphørerDenneMåneden =
            overgangstønadOpphørerDenneMåneden(listePeriodeOvergangsstønadGrunnlag = listePeriodeOvergangsstønadGrunnlag)

        if (!minsteBarnFylteTreÅrForrigeMåned && !overgangstønadOpphørerDenneMåneden) {
            logger.info(
                "For fagsak ${behandling.fagsak.id} ble verken yngste barn 3 år forrige måned eller har overgangsstønad som utløper denne måneden. " +
                    "Avbryter sending av autobrev for opphør av småbarnstillegg."
            )
            return
        }

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                fagsak = behandling.fagsak,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG
            )

        val vedtakBegrunnelseSpesifikasjon =
            if (minsteBarnFylteTreÅrForrigeMåned) VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR
            else VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD

        vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingEtterBehandlingsresultat.id),
            vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD
        )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

        opprettTaskJournalførVedtaksbrev(vedtakId = opprettetVedtak.id)
    }

    private fun overgangstønadOpphørerDenneMåneden(listePeriodeOvergangsstønadGrunnlag: List<PeriodeOvergangsstønadGrunnlag>): Boolean {
        TODO("Not yet implemented")
    }

    private fun minsteBarnFylteTreÅrForrigeMåned(personopplysningGrunnlag: PersonopplysningGrunnlag): Boolean {
        TODO("Not yet implemented")
    }

    private fun harBarnUnderTreÅr(personopplysningGrunnlag: PersonopplysningGrunnlag): Boolean {
        TODO("Not yet implemented")
    }

    private fun opprettTaskJournalførVedtaksbrev(vedtakId: Long) {
        val task = Task(
            JournalførVedtaksbrevTask.TASK_STEP_TYPE,
            "$vedtakId"
        )
        taskRepository.save(task)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutobrevOpphørSmåbarnstilleggService::class.java)
    }
}
