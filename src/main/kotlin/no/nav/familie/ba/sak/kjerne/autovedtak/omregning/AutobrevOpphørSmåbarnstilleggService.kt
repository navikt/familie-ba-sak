package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
class AutobrevOpphørSmåbarnstilleggService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val autovedtakService: AutovedtakService,
    private val periodeOvergangsstønadGrunnlagRepository: PeriodeOvergangsstønadGrunnlagRepository,
    private val vedtaksbegrunnelseRepository: VedtaksbegrunnelseRepository
) {
    @Transactional
    fun kjørBehandlingOgSendBrevForOpphørAvSmåbarnstillegg(behandlingId: Long) {

        val forrigeBehandling = behandlingService.hent(behandlingId)

        val alleVedtakbegrunnelserPåForrigeBehandling: List<Vedtaksbegrunnelse> =
            vedtaksbegrunnelseRepository.hentAlleVedtakbegrunnelserPåBehandling(forrigeBehandling.id)

        val opphørSmåbarnstilleggErAlleredeBegrunnet: Boolean =
            alleVedtakbegrunnelserPåForrigeBehandling.fold(false) { acc, curr ->
                if (
                    curr.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD ||
                    curr.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR
                ) {
                    true
                } else {
                    acc
                }
            }

        if (opphørSmåbarnstilleggErAlleredeBegrunnet) {
            logger.info(
                "For fagsak ${forrigeBehandling.fagsak.id} og behandlingsId ${forrigeBehandling.id} er opphør av småbarnstillegg allerede begrunnet."
            )
            return
        }

        val personopplysningGrunnlag: PersonopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)
                ?: throw FunksjonellFeil(
                    melding = "personopplysningGrunnlag er null for behandlingId: $behandlingId",
                )

        val listePeriodeOvergangsstønadGrunnlag: List<PeriodeOvergangsstønadGrunnlag> =
            periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(behandlingId)

        val minsteBarnFylteTreÅrForrigeMåned =
            minsteBarnFylteTreÅrForrigeMåned(personopplysningGrunnlag = personopplysningGrunnlag)

        val overgangstønadOpphørerDenneMåneden =
            overgangstønadOpphørerDenneMåneden(listePeriodeOvergangsstønadGrunnlag = listePeriodeOvergangsstønadGrunnlag)

        if (!minsteBarnFylteTreÅrForrigeMåned && !overgangstønadOpphørerDenneMåneden) {
            logger.info(
                "For fagsak ${forrigeBehandling.fagsak.id} ble verken yngste barn 3 år forrige måned eller har overgangsstønad som utløper denne måneden. " +
                    "Avbryter sending av autobrev for opphør av småbarnstillegg."
            )
            return
        }

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                fagsak = forrigeBehandling.fagsak,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG
            )

        val vedtakBegrunnelseSpesifikasjon =
            if (minsteBarnFylteTreÅrForrigeMåned) VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR
            else VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD

        vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingEtterBehandlingsresultat.id),
            vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon
        )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

        opprettTaskJournalførVedtaksbrev(vedtakId = opprettetVedtak.id)
    }

    fun overgangstønadOpphørerDenneMåneden(listePeriodeOvergangsstønadGrunnlag: List<PeriodeOvergangsstønadGrunnlag>): Boolean =
        listePeriodeOvergangsstønadGrunnlag.filter {
            it.tom.isSameOrAfter(
                LocalDate.now().withDayOfMonth(1)
            ) && it.tom.isBefore(LocalDate.now().førsteDagINesteMåned())
        }.isNotEmpty()

    fun minsteBarnFylteTreÅrForrigeMåned(personopplysningGrunnlag: PersonopplysningGrunnlag): Boolean {
        val fødselsdatoer: List<YearMonth> = personopplysningGrunnlag.personer.filter { it.type === PersonType.BARN }
            .map { it.fødselsdato.toYearMonth() }
        if (fødselsdatoer.any { it.isAfter(YearMonth.now().minusYears(3).minusMonths(1)) }) return false
        if (fødselsdatoer.any { it == YearMonth.now().minusYears(3).minusMonths(1) }) return true
        return false
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
