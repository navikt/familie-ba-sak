package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenSammenhengendePerioder
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilInternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.tilPeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.springframework.stereotype.Service

@Service
class SmåbarnstilleggService(
    private val efSakRestClient: EfSakRestClient,
    private val periodeOvergangsstønadGrunnlagRepository: PeriodeOvergangsstønadGrunnlagRepository,
    private val featureToggleService: FeatureToggleService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val persongrunnlagService: PersongrunnlagService,
) {

    fun hentOgLagrePerioderMedFullOvergangsstønad(
        aktør: Aktør,
        behandlingId: Long
    ): List<InternPeriodeOvergangsstønad> {
        return if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_SMÅBARNSTILLEGG)) {
            val periodeOvergangsstønad = hentPerioderMedFullOvergangsstønad(aktør)

            periodeOvergangsstønadGrunnlagRepository.deleteByBehandlingId(behandlingId)
            periodeOvergangsstønadGrunnlagRepository.saveAll(
                periodeOvergangsstønad.map {
                    it.tilPeriodeOvergangsstønadGrunnlag(
                        behandlingId, aktør
                    )
                }
            )

            periodeOvergangsstønad.map { it.tilInternPeriodeOvergangsstønad() }.slåSammenSammenhengendePerioder()
        } else emptyList()
    }

    fun vedtakOmOvergangsstønadPåvirkerFagsak(fagsak: Fagsak): Boolean {
        val sistIverksatteBehandling = behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsak.id)
            ?: return false

        val tilkjentYtelseFraSistIverksatteBehandling =
            tilkjentYtelseRepository.findByBehandling(behandlingId = sistIverksatteBehandling.id)

        val persongrunnlagFraSistIverksatteBehandling =
            persongrunnlagService.hentAktiv(behandlingId = sistIverksatteBehandling.id)
                ?: error("Finner ikke persongrunnlag")

        val nyePerioderMedFullOvergangsstønad =
            hentPerioderMedFullOvergangsstønad(aktør = fagsak.aktør).map { it.tilInternPeriodeOvergangsstønad() }

        return vedtakOmOvergangsstønadPåvirkerFagsak(
            småbarnstilleggBarnetrygdGenerator = SmåbarnstilleggBarnetrygdGenerator(
                behandlingId = sistIverksatteBehandling.id,
                tilkjentYtelse = tilkjentYtelseFraSistIverksatteBehandling
            ),
            nyePerioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
            forrigeAndelerTilkjentYtelse = tilkjentYtelseFraSistIverksatteBehandling.andelerTilkjentYtelse.toList(),
            barnasIdenterOgFødselsdatoer = persongrunnlagFraSistIverksatteBehandling.barna.map {
                Pair(
                    it.personIdent.ident,
                    it.fødselsdato
                )
            },
        )
    }

    private fun hentPerioderMedFullOvergangsstønad(aktør: Aktør): List<PeriodeOvergangsstønad> {
        return efSakRestClient.hentPerioderMedFullOvergangsstønad(
            aktør.aktivIdent()
        ).perioder
    }
}
