package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.toPeriode
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtAlleOpprettedeEndringerErUtfylt
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtEndringerErTilknyttetAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer
import no.nav.familie.ba.sak.kjerne.endretutbetaling.validerDeltBostedEndringerIkkeKrysserUtvidetYtelse
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingsresultatSteg(
    private val behandlingService: BehandlingService,
    private val simuleringService: SimuleringService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val vilkårService: VilkårService,
    private val persongrunnlagService: PersongrunnlagService,
    private val beregningService: BeregningService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
) : BehandlingSteg<String> {

    override fun preValiderSteg(behandling: Behandling, stegService: StegService?) {
        if (behandling.skalBehandlesAutomatisk) return

        if (!behandling.erTekniskOpphør() && behandling.type != BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT) {
            val vilkårsvurdering = vilkårService.hentVilkårsvurdering(behandlingId = behandling.id)
                ?: throw Feil("Finner ikke vilkårsvurdering på behandling ved validering.")

            val listeAvFeil = mutableListOf<String>()

            val barna = persongrunnlagService.hentBarna(behandling)
            barna.map { barn ->
                vilkårsvurdering.personResultater
                    .flatMap { it.vilkårResultater }
                    .filter { it.personResultat?.personIdent == barn.personIdent.ident }
                    .forEach { vilkårResultat ->
                        if (vilkårResultat.resultat == Resultat.OPPFYLT && vilkårResultat.periodeFom == null) {
                            listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato.tilDagMånedÅr()} mangler fom dato.")
                        }
                        if (vilkårResultat.periodeFom != null && vilkårResultat.toPeriode().fom.isBefore(barn.fødselsdato)) {
                            listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato.tilDagMånedÅr()} har fra-og-med dato før barnets fødselsdato.")
                        }
                        if (vilkårResultat.periodeFom != null &&
                            vilkårResultat.toPeriode().fom.isAfter(barn.fødselsdato.plusYears(18)) &&
                            vilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR &&
                            vilkårResultat.erEksplisittAvslagPåSøknad != true
                        ) {
                            listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato.tilDagMånedÅr()} har fra-og-med dato etter barnet har fylt 18.")
                        }
                    }
            }
        }

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(
            behandlingId = behandling.id
        )!!

        validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
            tilkjentYtelse = tilkjentYtelse,
            personopplysningGrunnlag = personopplysningGrunnlag
        )

        validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(tilkjentYtelse)

        val andreBehandlingerPåBarna = personopplysningGrunnlag.barna.map {
            Pair(
                it,
                beregningService.hentIverksattTilkjentYtelseForBarn(it.personIdent, behandling)
            )
        }

        val endretUtbetalingAndeler = endretUtbetalingAndelService.hentForBehandling(behandling.id)
        validerAtAlleOpprettedeEndringerErUtfylt(endretUtbetalingAndeler)
        validerAtEndringerErTilknyttetAndelTilkjentYtelse(endretUtbetalingAndeler)
        validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(endretUtbetalingAndeler)

        validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
            behandlendeBehandlingTilkjentYtelse = tilkjentYtelse,
            barnMedAndreTilkjentYtelse = andreBehandlingerPåBarna,
            personopplysningGrunnlag = personopplysningGrunnlag
        )

        validerDeltBostedEndringerIkkeKrysserUtvidetYtelse(
            endretUtbetalingAndeler,
            tilkjentYtelse.andelerTilkjentYtelse.toList()
        )
    }

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {

        endretUtbetalingAndelService.oppdaterEndreteUtbetalingsandelerMedBegrunnelser(behandling)

        val behandlingMedResultat = if (behandling.erMigrering() && behandling.skalBehandlesAutomatisk) {
            settBehandlingResultat(behandling, BehandlingResultat.INNVILGET)
        } else {
            val resultat = behandlingsresultatService.utledBehandlingsresultat(behandlingId = behandling.id)
            behandlingService.oppdaterResultatPåBehandling(
                behandlingId = behandling.id,
                resultat = resultat
            )
        }

        if (behandlingMedResultat.opprettetÅrsak != BehandlingÅrsak.SATSENDRING) {
            vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(
                vedtak = vedtakService.hentAktivForBehandlingThrows(
                    behandlingId = behandling.id
                )
            )
        }

        if (behandlingMedResultat.skalRettFraBehandlingsresultatTilIverksetting()) {
            behandlingService.oppdaterStatusPåBehandling(
                behandlingMedResultat.id,
                BehandlingStatus.IVERKSETTER_VEDTAK
            )
        } else {
            simuleringService.oppdaterSimuleringPåBehandling(behandlingMedResultat)
        }

        return hentNesteStegForNormalFlyt(behandlingMedResultat)
    }

    override fun stegType(): StegType {
        return StegType.BEHANDLINGSRESULTAT
    }

    private fun settBehandlingResultat(behandling: Behandling, resultat: BehandlingResultat): Behandling {
        behandling.resultat = resultat
        return behandlingService.lagreEllerOppdater(behandling)
    }
}
