package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.erUlike
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.tilPeriodeOvergangsstønadGrunnlag
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SmåbarnstilleggService(
    private val efSakRestClient: EfSakRestClient,
    private val periodeOvergangsstønadGrunnlagRepository: PeriodeOvergangsstønadGrunnlagRepository,
    private val featureToggleService: FeatureToggleService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService
) {

    fun hentOgLagrePerioderMedFullOvergangsstønad(
        personIdent: String,
        behandlingId: Long
    ): List<PeriodeOvergangsstønad> {
        return if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_SMÅBARNSTILLEGG)) {
            val periodeOvergangsstønad = hentPerioderMedFullOvergangsstønad(personIdent)

            periodeOvergangsstønadGrunnlagRepository.deleteByBehandlingId(behandlingId)
            periodeOvergangsstønadGrunnlagRepository.saveAll(
                periodeOvergangsstønad.map {
                    it.tilPeriodeOvergangsstønadGrunnlag(
                        behandlingId
                    )
                }
            )

            periodeOvergangsstønad
        } else emptyList()
    }

    fun vedtakOmOvergangsstønadPåvirkerFagsak(personIdent: String): Boolean {
        val fagsak = fagsakService.hent(personIdent = PersonIdent(personIdent))
            ?: throw Feil(
                message = "Finner ikke fagsak på person",
                frontendFeilmelding = "Finner ikke fagsak på person med ident $personIdent"
            )

        val sistIverksatteBehandling = behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsak.id)
            ?: return false

        val tilkjentYtelseFraSistIverksatteBehandling =
            beregningService.hentTilkjentYtelseForBehandling(behandlingId = sistIverksatteBehandling.id)

        val forrigeSøkersAndeler = tilkjentYtelseFraSistIverksatteBehandling.andelerTilkjentYtelse.toList()

        val persongrunnlagFraSistIverksatteBehandling =
            persongrunnlagService.hentAktiv(behandlingId = sistIverksatteBehandling.id)
                ?: error("Finner ikke persongrunnlag")

        val nyePerioderMedFullOvergangsstønad = hentPerioderMedFullOvergangsstønad(personIdent = personIdent)

        return vedtakOmOvergangsstønadPåvirkerFagsak(
            småbarnstilleggBarnetrygdGenerator = SmåbarnstilleggBarnetrygdGenerator(
                behandlingId = sistIverksatteBehandling.id,
                tilkjentYtelse = tilkjentYtelseFraSistIverksatteBehandling
            ),
            nyePerioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
            forrigeSøkersAndeler = forrigeSøkersAndeler,
            barnasFødselsdatoer = persongrunnlagFraSistIverksatteBehandling.barna.map { it.fødselsdato },
        )
    }

    private fun hentPerioderMedFullOvergangsstønad(personIdent: String): List<PeriodeOvergangsstønad> {
        return efSakRestClient.hentPerioderMedFullOvergangsstønad(
            personIdent
        ).perioder
    }
}

fun vedtakOmOvergangsstønadPåvirkerFagsak(
    småbarnstilleggBarnetrygdGenerator: SmåbarnstilleggBarnetrygdGenerator,
    nyePerioderMedFullOvergangsstønad: List<PeriodeOvergangsstønad>,
    forrigeSøkersAndeler: List<AndelTilkjentYtelse>,
    barnasFødselsdatoer: List<LocalDate>
): Boolean {
    val (forrigeSøkersSmåbarnstilleggAndeler, forrigeSøkersAndreAndeler) = forrigeSøkersAndeler.partition { it.erSmåbarnstillegg() }

    val nyeSmåbarnstilleggAndeler = småbarnstilleggBarnetrygdGenerator.lagSmåbarnstilleggAndeler(
        perioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
        andelerSøker = forrigeSøkersAndreAndeler,
        barnasFødselsdatoer = barnasFødselsdatoer
    )

    return forrigeSøkersSmåbarnstilleggAndeler.erUlike(nyeSmåbarnstilleggAndeler)
}
