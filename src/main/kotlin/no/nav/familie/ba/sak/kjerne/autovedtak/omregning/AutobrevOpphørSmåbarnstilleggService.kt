package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class AutobrevOpphørSmåbarnstilleggService(
    private val autobrevService: AutobrevService,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingService: BehandlingService,
    private val periodeOvergangsstønadGrunnlagRepository: PeriodeOvergangsstønadGrunnlagRepository,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService
) {
    @Transactional
    fun kjørBehandlingOgSendBrevForOpphørAvSmåbarnstillegg(fagsakId: Long) {

        val behandling =
            behandlingService.hentAktivForFagsak(fagsakId = fagsakId) ?: error("Fant ikke aktiv behandling")

        val personopplysningGrunnlag: PersonopplysningGrunnlag =
            persongrunnlagService.hentAktivThrows(behandling.id)

        val listePeriodeOvergangsstønadGrunnlag: List<PeriodeOvergangsstønadGrunnlag> =
            periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(behandlingId = behandling.id)

        val yngsteBarnFylteTreÅrForrigeMåned =
            yngsteBarnFylteTreÅrForrigeMåned(personopplysningGrunnlag = personopplysningGrunnlag)

        val overgangstønadOpphørteForrigeMåned =
            overgangstønadOpphørteForrigeMåned(listePeriodeOvergangsstønadGrunnlag = listePeriodeOvergangsstønadGrunnlag)

        val behandlingsårsak = BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG
        val standardbegrunnelse =
            if (yngsteBarnFylteTreÅrForrigeMåned) VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR
            else VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD

        if (!autobrevService.skalAutobrevBehandlingOpprettes(
                fagsakId = fagsakId,
                behandlingsårsak = behandlingsårsak,
                vedtaksperioderMedBegrunnelser = behandlingService.hentBehandlinger(fagsakId = fagsakId)
                    .filter { behandling ->
                        val vedtaksmåned =
                            vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id).vedtaksdato?.toLocalDate()
                                ?.toYearMonth()

                        behandling.status == BehandlingStatus.AVSLUTTET && (
                            vedtaksmåned == YearMonth.now() || vedtaksmåned == YearMonth.now()
                                .minusMonths(1)
                            )
                    }
                    .flatMap { behandling ->
                        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandling.id)
                        vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
                    },
                standardbegrunnelser = listOf(standardbegrunnelse)
            )
        ) {
            return
        }

        if (!yngsteBarnFylteTreÅrForrigeMåned && !overgangstønadOpphørteForrigeMåned) {
            logger.info(
                "For fagsak $fagsakId ble verken yngste barn 3 år forrige måned eller har overgangsstønad som utløper denne måneden. " +
                    "Avbryter sending av autobrev for opphør av småbarnstillegg."
            )
            return
        }

        autobrevService.opprettOgKjørOmregningsbehandling(
            behandling = behandling,
            behandlingsårsak = behandlingsårsak,
            standardbegrunnelse = standardbegrunnelse
        )
    }

    fun overgangstønadOpphørteForrigeMåned(listePeriodeOvergangsstønadGrunnlag: List<PeriodeOvergangsstønadGrunnlag>): Boolean =
        listePeriodeOvergangsstønadGrunnlag.maxOfOrNull { it.tom }?.toYearMonth() == YearMonth.now().minusMonths(1)

    fun yngsteBarnFylteTreÅrForrigeMåned(personopplysningGrunnlag: PersonopplysningGrunnlag): Boolean {
        val yngsteBarnSinFødselsdato: YearMonth =
            personopplysningGrunnlag.barna
                .maxOf { it.fødselsdato.toYearMonth() }

        return yngsteBarnSinFødselsdato.plusYears(3) == YearMonth.now().minusMonths(1)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutobrevOpphørSmåbarnstilleggService::class.java)
    }
}
