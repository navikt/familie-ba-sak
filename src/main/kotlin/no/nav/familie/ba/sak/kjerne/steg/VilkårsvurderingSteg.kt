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
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse.Companion.disjunkteAndeler
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårsvurderingSteg(
        private val vilkårService: VilkårService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService,
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val behandlingsresultatService: BehandlingsresultatService,
        private val behandlingService: BehandlingService,
        private val simuleringService: SimuleringService,
        private val vedtakService: VedtakService
) : BehandlingSteg<String> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: String): StegType {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandling.id)
                                       ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        if (behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            vilkårService.initierVilkårsvurderingForBehandling(behandling, true)
        }

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        val behandlingMedResultat = if (behandling.erMigrering() && behandling.skalBehandlesAutomatisk) {
            settBehandlingResultat(behandling, BehandlingResultat.INNVILGET)
        } else if (behandling.opprettetÅrsak == BehandlingÅrsak.SATSENDRING) {
            // TODO: Når man støtter å utlede behandlingsresultat ENDRET ved ren beløpsendring kan resultat for satsendring settes på samme måte
            val forrigebehandling = behandlingService.hentForrigeBehandlingSomErIverksatt(behandling = behandling)
                                    ?: throw Feil("Forsøker å utføre satsendring på fagsak uten iverksatte behandlinger")
            val forrigeAndeler = beregningService.hentAndelerTilkjentYtelseForBehandling(forrigebehandling.id).toSet()
            val andeler = beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id).toSet()
            if (forrigeAndeler.disjunkteAndeler(andeler).isEmpty())
                settBehandlingResultat(behandling, BehandlingResultat.FORTSATT_INNVILGET)
            else
                settBehandlingResultat(behandling, BehandlingResultat.ENDRET)
        } else {
            val resultat = behandlingsresultatService.utledBehandlingsresultat(behandlingId = behandling.id)
            behandlingService.oppdaterResultatPåBehandling(behandlingId = behandling.id,
                                                           resultat = resultat)
        }

        if (behandling.opprettetÅrsak != BehandlingÅrsak.SATSENDRING) {
            vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(vedtak = vedtakService.hentAktivForBehandlingThrows(
                    behandlingId = behandling.id))
        }

        if (behandlingMedResultat.skalBehandlesAutomatisk) {
            behandlingService.oppdaterStatusPåBehandling(behandlingMedResultat.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        } else {
            simuleringService.oppdaterSimuleringPåBehandling(behandlingMedResultat)
        }

        return hentNesteStegForNormalFlyt(behandlingMedResultat)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }

    override fun postValiderSteg(behandling: Behandling) {
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
                                vilkårResultat.erEksplisittAvslagPåSøknad != true) {
                                listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato.tilDagMånedÅr()} har fra-og-med dato etter barnet har fylt 18.")
                            }
                        }
            }
        }

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(
                behandlingId = behandling.id)!!

        TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse = tilkjentYtelse,
                                                                                     personopplysningGrunnlag = personopplysningGrunnlag)

        TilkjentYtelseValidering.validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(tilkjentYtelse)

        val andreBehandlingerPåBarna = personopplysningGrunnlag.barna.map {
            Pair(it,
                 beregningService.hentIverksattTilkjentYtelseForBarn(it.personIdent, behandling)
            )
        }
        TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(behandlendeBehandlingTilkjentYtelse = tilkjentYtelse,
                                                                                   barnMedAndreTilkjentYtelse = andreBehandlingerPåBarna,
                                                                                   personopplysningGrunnlag = personopplysningGrunnlag)
    }

    private fun settBehandlingResultat(behandling: Behandling, resultat: BehandlingResultat): Behandling {
        behandling.resultat = resultat
        return behandlingService.lagreEllerOppdater(behandling)
    }
}