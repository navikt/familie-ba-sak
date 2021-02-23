package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
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

        val ytelsePersoner: List<YtelsePerson> =
                if (behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
                    val barn = persongrunnlagService.hentBarna(behandling).map { it.personIdent.ident }
                    YtelsePersonUtils.utledKravForFødselshendelseFGB(barn)
                } else {
                    YtelsePersonUtils.utledKrav(
                            søknadDTO = søknadGrunnlagService.hentAktiv(behandlingId = behandlingId)?.hentSøknadDto(),
                            forrigeAndelerTilkjentYtelse = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList())
                }

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(
                ytelsePersoner = ytelsePersoner,
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList(),
                forrigeAndelerTilkjentYtelse = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList(),
                personerMedEksplisitteAvslag = vilkårsvurderingService.finnPersonerMedEksplisittAvslagPåBehandling(behandlingId))

        val behandlingsresultat =
                BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersonerMedResultat)
        secureLogger.info("Resultater fra vilkårsvurdering på behandling ${behandling.id}: $ytelsePersonerMedResultat")
        LOG.info("Resultat fra vilkårsvurdering på behandling ${behandling.id}: $behandlingsresultat")

        return behandlingsresultat
    }

    companion object {

        val LOG: Logger = LoggerFactory.getLogger(this::class.java)
        val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}




