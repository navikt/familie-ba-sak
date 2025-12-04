package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.IVERKSETTER_VEDTAK
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatStegValideringService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatValideringUtils
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.småbarnstillegg.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.steg.EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingsresultatSteg(
    private val behandlingService: BehandlingService,
    private val simuleringService: SimuleringService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val persongrunnlagService: PersongrunnlagService,
    private val beregningService: BeregningService,
    private val småbarnstilleggService: SmåbarnstilleggService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val behandlingsresultatstegValideringService: BehandlingsresultatStegValideringService,
) : BehandlingSteg<String> {
    override fun preValiderSteg(
        behandling: Behandling,
        stegService: StegService?,
    ) {
        if (!behandling.erSatsendringMånedligValutajusteringFinnmarkstilleggEllerSvalbardtillegg() && behandling.skalBehandlesAutomatisk) {
            return
        }

        val søkerOgBarn = persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id)
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)

        validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
            tilkjentYtelse = tilkjentYtelse,
            søkerOgBarn = søkerOgBarn,
        )

        behandlingsresultatstegValideringService.validerAtUtenlandskPeriodebeløpOgValutakursErUtfylt(behandling = behandling)

        if (behandling.erSatsendring()) {
            behandlingsresultatstegValideringService.validerSatsendring(tilkjentYtelse)
        }

        if (behandling.erFinnmarkstillegg()) {
            behandlingsresultatstegValideringService.validerFinnmarkstilleggBehandling(tilkjentYtelse)
        }

        if (behandling.erSvalbardtillegg()) {
            behandlingsresultatstegValideringService.validerSvalbardtilleggBehandling(tilkjentYtelse)
        }

        if (!behandling.erSatsendringMånedligValutajusteringFinnmarkstilleggEllerSvalbardtillegg()) {
            behandlingsresultatstegValideringService.validerEndredeUtbetalingsandeler(tilkjentYtelse)
            behandlingsresultatstegValideringService.validerKompetanse(behandling.id)
            behandlingsresultatstegValideringService.validerAtDetIkkeFinnesPerioderMedSekundærlandKompetanseUtenUtenlandskbeløpEllerValutakurs(behandling.id)
        }

        if (behandling.erMånedligValutajustering()) {
            behandlingsresultatstegValideringService.validerIngenEndringTilbakeITid(tilkjentYtelse)
            behandlingsresultatstegValideringService.validerSatsErUendret(tilkjentYtelse)
        }

        if (behandling.erEndreMigreringsdato()) {
            behandlingsresultatstegValideringService
                .validerIngenEndringIUtbetalingEtterMigreringsdatoenTilForrigeIverksatteBehandling(behandling)
        }
    }

    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: String,
    ): StegType {
        val behandlingsresultat = behandlingsresultatService.utledBehandlingsresultat(behandling.id)
        val opprettVilkårsvurderingLogg = !(behandling.erMigrering() && behandling.skalBehandlesAutomatisk)
        val behandlingMedOppdatertBehandlingsresultat =
            behandlingService.oppdaterBehandlingsresultat(
                behandlingId = behandling.id,
                resultat = behandlingsresultat,
                opprettVilkårsvurderingLogg = opprettVilkårsvurderingLogg,
            )

        if (behandlingMedOppdatertBehandlingsresultat.erBehandlingMedVedtaksbrevutsending()) {
            behandlingService.nullstillEndringstidspunkt(behandling.id)
            val vedtak = vedtakService.hentAktivForBehandlingThrows(behandling.id)
            vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(vedtak)
        }

        val endringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi =
            beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling)

        val skalRettFraBehandlingsresultatTilIverksetting =
            behandlingMedOppdatertBehandlingsresultat.skalRettFraBehandlingsresultatTilIverksetting(
                endringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi == ENDRING_I_UTBETALING,
            )

        if (skalRettFraBehandlingsresultatTilIverksetting ||
            småbarnstilleggService.kanAutomatiskIverksetteSmåbarnstilleggEndring(behandlingMedOppdatertBehandlingsresultat)
        ) {
            behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, status = IVERKSETTER_VEDTAK)
        } else {
            simuleringService.oppdaterSimuleringPåBehandling(behandlingMedOppdatertBehandlingsresultat)
        }

        tilbakestillBehandlingService.slettTilbakekrevingsvedtakMotregningHvisBehandlingIkkeAvregner(behandling.id)

        return hentNesteStegGittEndringerIUtbetaling(
            behandling,
            endringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi,
        )
    }

    override fun postValiderSteg(behandling: Behandling) {
        BehandlingsresultatValideringUtils.validerBehandlingsresultat(behandling)
    }

    override fun stegType(): StegType = StegType.BEHANDLINGSRESULTAT

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)!!
    }
}
