package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeMedBegrunnelser

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.KAN_BEHANDLE_EØS
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.LAG_REDUKSJONSPERIODER_FRA_INNVILGELSESTIDSPUNKT
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.finnOgOppdaterOverlappendeUtbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.hentPerioderMedUtbetaling
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.identifiserReduksjonsperioderFraSistIverksatteBehandling
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.oppdaterUtbetalingsperioderMedReduksjonFraForrigeBehandling
import org.springframework.stereotype.Service

@Service
class UtbetalingspreiodeMedBegrunnelserService(
    private val featureToggleService: FeatureToggleService,
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val kompetanseRepository: KompetanseRepository,
) {

    fun hentUtbealtingsperioder(
        vedtak: Vedtak,
        opphørsperioder: List<VedtaksperiodeMedBegrunnelser>
    ): List<VedtaksperiodeMedBegrunnelser> {
        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = vedtak.behandling.id)

        val utbetalingsperioder = hentPerioderMedUtbetaling(
            andelerTilkjentYtelse,
            vedtak
        )

        val perioderMedReduksjonFraSistIverksatteBehandling =
            hentReduksjonsperioderFraInnvilgelsesTidspunkt(
                vedtak = vedtak,
                utbetalingsperioder = utbetalingsperioder,
                opphørsperioder = opphørsperioder
            )

        val utbetalingsperioderMedReduksjon =
            if (featureToggleService.isEnabled(FeatureToggleConfig.NY_MÅTE_Å_GENERERE_VEDTAKSPERIODER)) {
                oppdaterUtbetalingsperioderMedReduksjonFraForrigeBehandling(
                    utbetalingsperioder = utbetalingsperioder,
                    reduksjonsperioder = perioderMedReduksjonFraSistIverksatteBehandling
                )
            } else {
                finnOgOppdaterOverlappendeUtbetalingsperiode(
                    utbetalingsperioder,
                    perioderMedReduksjonFraSistIverksatteBehandling
                )
            }

        return if (featureToggleService.isEnabled(KAN_BEHANDLE_EØS)) {
            val kompetanser = kompetanseRepository.findByBehandlingId(vedtak.behandling.id)

            slåSammenUtbetalingsperioderMedKompetanse(
                utbetalingsperioder = utbetalingsperioderMedReduksjon,
                kompetanser = kompetanser.toList()
            )
        } else utbetalingsperioderMedReduksjon
    }

    fun hentReduksjonsperioderFraInnvilgelsesTidspunkt(
        vedtak: Vedtak,
        utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
        opphørsperioder: List<VedtaksperiodeMedBegrunnelser>
    ): List<VedtaksperiodeMedBegrunnelser> {
        val erToggelenPå = featureToggleService.isEnabled(LAG_REDUKSJONSPERIODER_FRA_INNVILGELSESTIDSPUNKT)
        if (!erToggelenPå) return emptyList()
        val behandling = vedtak.behandling
        if (behandling.skalBehandlesAutomatisk) return emptyList()

        val forrigeIverksatteBehandling: Behandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)
                ?: return emptyList()

        val forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag =
            forrigeIverksatteBehandling.let { persongrunnlagService.hentAktivThrows(it.id) }

        val forrigeAndelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeIverksatteBehandling.id)

        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vedtak.behandling.id)

        return identifiserReduksjonsperioderFraSistIverksatteBehandling(
            forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            vedtak = vedtak,
            utbetalingsperioder = utbetalingsperioder,
            personopplysningGrunnlag = personopplysningGrunnlag,
            opphørsperioder = opphørsperioder,
            aktørerIForrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag.søkerOgBarn.map { it.aktør },
            skalBrukeNyMåteÅGenerereVedtaksperioder = featureToggleService.isEnabled(FeatureToggleConfig.NY_MÅTE_Å_GENERERE_VEDTAKSPERIODER)
        )
    }
}
