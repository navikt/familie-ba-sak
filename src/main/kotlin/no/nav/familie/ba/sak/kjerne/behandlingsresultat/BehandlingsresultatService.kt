package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BehandlingsresultatService(
        private val behandlingService: BehandlingService,
        private val søknadGrunnlagService: SøknadGrunnlagService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService,
        private val vilkårsvurderingService: VilkårsvurderingService,
) {

    fun utledBehandlingsresultat(behandlingId: Long): BehandlingResultat {
        val behandling = behandlingService.hent(behandlingId = behandlingId)
        val forrigeBehandling = behandlingService.hentForrigeBehandlingSomErIverksatt(behandling)

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingId)
        val forrigeTilkjentYtelse: TilkjentYtelse? =
                forrigeBehandling?.let { beregningService.hentOptionalTilkjentYtelseForBehandling(behandlingId = it.id) }

        val barnMedEksplisitteAvslag = vilkårsvurderingService.finnBarnMedEksplisittAvslagPåBehandling(behandlingId)

        val ytelsePersoner: List<YtelsePerson> =
                if (behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
                    val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
                    val parterSomErVurdertIInneværendeBehandling =
                            vilkårsvurdering?.personResultater?.filter { it.vilkårResultater.any { vilkårResultat -> vilkårResultat.behandlingId == behandlingId } }
                                    ?.map { it.personIdent } ?: emptyList()

                    val barn = persongrunnlagService.hentBarna(behandling)
                            .filter { parterSomErVurdertIInneværendeBehandling.contains(it.personIdent.ident) }
                            .map { it.personIdent.ident }
                    YtelsePersonUtils.utledKravForFødselshendelseFGB(barn)
                } else {
                    YtelsePersonUtils.utledKrav(
                            søknadDTO = søknadGrunnlagService.hentAktiv(behandlingId = behandlingId)?.hentSøknadDto(),
                            forrigeAndelerTilkjentYtelse = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList(),
                            barnMedEksplisitteAvslag = barnMedEksplisitteAvslag)
                }

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(
                ytelsePersoner = ytelsePersoner,
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList(),
                forrigeAndelerTilkjentYtelse = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList(),
                barnMedEksplisitteAvslag = barnMedEksplisitteAvslag)

        val behandlingsresultat =
                BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersonerMedResultat)
        secureLogger.info("Resultater fra vilkårsvurdering på behandling ${behandling.id}: $ytelsePersonerMedResultat")
        logger.info("Resultat fra vilkårsvurdering på behandling ${behandling.id}: $behandlingsresultat")

        return behandlingsresultat
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(BehandlingsresultatService::class.java)
        private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}




