package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenSammenhengendePerioder
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilInternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.tilPeriodeOvergangsstønadGrunnlag
import org.springframework.stereotype.Service

@Service
class SmåbarnstilleggService(
    private val efSakRestClient: EfSakRestClient,
    private val periodeOvergangsstønadGrunnlagRepository: PeriodeOvergangsstønadGrunnlagRepository,
    private val featureToggleService: FeatureToggleService
) {

    fun hentPerioderMedFullOvergangsstønad(
        personIdent: String,
        behandlingId: Long
    ): List<InternPeriodeOvergangsstønad> {
        return if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_SMÅBARNSTILLEGG)) {
            val periodeOvergangsstønad = efSakRestClient.hentPerioderMedFullOvergangsstønad(
                personIdent
            ).perioder

            periodeOvergangsstønadGrunnlagRepository.deleteByBehandlingId(behandlingId)
            periodeOvergangsstønadGrunnlagRepository.saveAll(
                periodeOvergangsstønad.map {
                    it.tilPeriodeOvergangsstønadGrunnlag(
                        behandlingId
                    )
                }
            )

            periodeOvergangsstønad.map { it.tilInternPeriodeOvergangsstønad() }.slåSammenSammenhengendePerioder()
        } else emptyList()
    }
}
