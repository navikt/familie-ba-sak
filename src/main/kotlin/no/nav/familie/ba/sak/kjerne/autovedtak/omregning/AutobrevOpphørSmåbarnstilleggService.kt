package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class AutobrevOpphørSmåbarnstilleggService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val autovedtakBrevService: AutovedtakBrevService,
    private val autovedtakStegService: AutovedtakStegService,
    private val persongrunnlagService: PersongrunnlagService,
    private val periodeOvergangsstønadGrunnlagRepository: PeriodeOvergangsstønadGrunnlagRepository
) {
    @Transactional
    fun kjørBehandlingOgSendBrevForOpphørAvSmåbarnstillegg(fagsakId: Long) {

        val behandling =
            behandlingHentOgPersisterService.hentAktivForFagsak(fagsakId = fagsakId)
                ?: error("Fant ikke aktiv behandling")

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
            if (yngsteBarnFylteTreÅrForrigeMåned) Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR
            else Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD

        if (!autovedtakBrevService.skalAutobrevBehandlingOpprettes(
                fagsakId = fagsakId,
                behandlingsårsak = behandlingsårsak,
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

        autovedtakStegService.kjørBehandlingOmregning(
            mottakersAktør = behandling.fagsak.aktør,
            behandlingsdata = AutovedtakBrevBehandlingsdata(
                aktør = behandling.fagsak.aktør,
                behandlingsårsak = behandlingsårsak,
                standardbegrunnelse = standardbegrunnelse
            )
        )
    }

    fun overgangstønadOpphørteForrigeMåned(listePeriodeOvergangsstønadGrunnlag: List<PeriodeOvergangsstønadGrunnlag>): Boolean =
        listePeriodeOvergangsstønadGrunnlag.any { periodeOvergangsstønadGrunnlag ->
            periodeOvergangsstønadGrunnlag.tom.toYearMonth() == YearMonth.now().minusMonths(1)
        }

    fun yngsteBarnFylteTreÅrForrigeMåned(personopplysningGrunnlag: PersonopplysningGrunnlag): Boolean {
        val yngsteBarnSinFødselsdato: YearMonth =
            personopplysningGrunnlag.yngsteBarnSinFødselsdato.toYearMonth()

        return yngsteBarnSinFødselsdato.plusYears(3) == YearMonth.now().minusMonths(1)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutobrevOpphørSmåbarnstilleggService::class.java)
    }
}
