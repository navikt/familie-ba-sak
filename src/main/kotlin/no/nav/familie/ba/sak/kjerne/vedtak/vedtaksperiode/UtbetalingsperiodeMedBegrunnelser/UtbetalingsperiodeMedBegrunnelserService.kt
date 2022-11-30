package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeMedBegrunnelser

import hentPerioderMedUtbetaling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.identifiserReduksjonsperioderFraSistIverksatteBehandling
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.oppdaterUtbetalingsperioderMedReduksjonFraForrigeBehandling
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service

@Service
class UtbetalingsperiodeMedBegrunnelserService(
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val kompetanseRepository: KompetanseRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {
    fun hentUtbetalingsperioder(
        vedtak: Vedtak,
        opphørsperioder: List<VedtaksperiodeMedBegrunnelser>
    ): List<VedtaksperiodeMedBegrunnelser> {
        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(vedtak.behandling.id)

        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = vedtak.behandling.id)

        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = vedtak.behandling.id)
        val utbetalingsperioder = hentPerioderMedUtbetaling(
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            vedtak = vedtak,
            personResultater = vilkårsvurdering.personResultater,
            personerIPersongrunnlag = personopplysningGrunnlag.personer.toList()
        )

        val perioderMedReduksjonFraSistIverksatteBehandling =
            hentReduksjonsperioderFraInnvilgelsesTidspunkt(
                vedtak = vedtak,
                utbetalingsperioder = utbetalingsperioder,
                opphørsperioder = opphørsperioder
            )

        val utbetalingsperioderMedReduksjon =
            oppdaterUtbetalingsperioderMedReduksjonFraForrigeBehandling(
                utbetalingsperioder = utbetalingsperioder,
                reduksjonsperioder = perioderMedReduksjonFraSistIverksatteBehandling
            )

        val kompetanser = kompetanseRepository.finnFraBehandlingId(vedtak.behandling.id)
        return splittUtbetalingsperioderPåKompetanser(
            utbetalingsperioder = utbetalingsperioderMedReduksjon,
            kompetanser = kompetanser.toList()
        )
    }

    fun hentReduksjonsperioderFraInnvilgelsesTidspunkt(
        vedtak: Vedtak,
        utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
        opphørsperioder: List<VedtaksperiodeMedBegrunnelser>
    ): List<VedtaksperiodeMedBegrunnelser> {
        val behandling = vedtak.behandling
        if (behandling.skalBehandlesAutomatisk) return emptyList()

        val forrigeIverksatteBehandling: Behandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)
                ?: return emptyList()

        val forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag =
            forrigeIverksatteBehandling.let { persongrunnlagService.hentAktivThrows(it.id) }

        val forrigeAndelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(forrigeIverksatteBehandling.id)

        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vedtak.behandling.id)

        return identifiserReduksjonsperioderFraSistIverksatteBehandling(
            forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            vedtak = vedtak,
            utbetalingsperioder = utbetalingsperioder,
            personopplysningGrunnlag = personopplysningGrunnlag,
            opphørsperioder = opphørsperioder,
            aktørerIForrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag.søkerOgBarn.map { it.aktør }
        )
    }
}
