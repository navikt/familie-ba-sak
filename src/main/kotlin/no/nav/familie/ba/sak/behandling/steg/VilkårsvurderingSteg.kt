package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.resultat.BehandlingsresultatService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.TilkjentYtelseValidering
import no.nav.familie.ba.sak.common.RessursUtils
import no.nav.familie.ba.sak.common.VilkårsvurderingFeil
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.toPeriode
import no.nav.familie.ba.sak.nare.Resultat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårsvurderingSteg(
        private val vilkårService: VilkårService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService,
        private val vedtakService: VedtakService,
        private val behandlingsresultatService: BehandlingsresultatService,
        private val behandlingService: BehandlingService
) : BehandlingSteg<String> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: String): StegType {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandling.id)
                                       ?: error("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        if (behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            vilkårService.initierVilkårsvurderingForBehandling(behandling, true)
        }
        vedtakService.oppdaterOpphørsdatoPåVedtak(behandlingId = behandling.id)
        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        val resultat = if (behandling.erMigrering()) BehandlingResultat.IKKE_VURDERT else
            behandlingsresultatService.utledBehandlingsresultat(behandlingId = behandling.id)
        behandlingService.oppdaterResultatPåBehandling(behandlingId = behandling.id,
                                                       resultat = resultat)

        if (behandling.skalBehandlesAutomatisk) {
            behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)

            return when (resultat) {
                BehandlingResultat.FORTSATT_INNVILGET -> StegType.JOURNALFØR_VEDTAKSBREV
                else -> StegType.IVERKSETT_MOT_OPPDRAG
            }
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }

    override fun postValiderSteg(behandling: Behandling) {
        if (behandling.skalBehandlesAutomatisk) return

        if (!behandling.erTekniskOpphør() && behandling.type != BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT) {
            val vilkårsvurdering = vilkårService.hentVilkårsvurdering(behandlingId = behandling.id)
                                   ?: error("Finner ikke vilkårsvurdering på behandling ved validering.")

            val listeAvFeil = mutableListOf<String>()

            val periodeResultater = vilkårsvurdering.periodeResultater(brukMåned = false)

            val harGyldigePerioder = periodeResultater.any { periodeResultat ->
                periodeResultat.allePåkrevdeVilkårVurdert(PersonType.SØKER) &&
                periodeResultat.allePåkrevdeVilkårVurdert(PersonType.BARN)
            }

            when {
                !harGyldigePerioder -> {
                    listeAvFeil.add("Du har vilkår som mangler vurdering. Gå gjennom vilkårene og kontroller om alt er ok. Ta kontakt med Team Familie om du ikke kommer videre.")
                }
            }

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

            if (listeAvFeil.isNotEmpty()) {
                throw VilkårsvurderingFeil(melding = "Validering av vilkårsvurdering feilet for behandling ${behandling.id}",
                                           frontendFeilmelding = RessursUtils.lagFrontendMelding("Vilkårsvurderingen er ugyldig med følgende feil:",
                                                                                                 listeAvFeil)
                )
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

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(VilkårsvurderingSteg::class.java)
        private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}