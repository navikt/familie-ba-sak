package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
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

        val barna = persongrunnlagService.hentBarna(behandling)
        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)
        if (barna.isEmpty() && (søknadGrunnlag?.hentUregistrerteBarn() ?: emptyList()).isEmpty()) throw FunksjonellFeil(
                melding = "Ingen barn i personopplysningsgrunnlag ved validering av vilkårsvurdering på behandling ${behandling.id}",
                frontendFeilmelding = "Barn må legges til for å gjennomføre vilkårsvurdering.")

        val ytelsePersoner: List<YtelsePerson> =
                if (behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
                    val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
                    val parterSomErVurdertIInneværendeBehandling =
                            vilkårsvurdering?.personResultater?.filter { it.vilkårResultater.any { vilkårResultat -> vilkårResultat.behandlingId == behandlingId } }
                                    ?.map { it.personIdent } ?: emptyList()

                    val barn = barna
                            .filter { parterSomErVurdertIInneværendeBehandling.contains(it.personIdent.ident) }
                            .map { it.personIdent.ident }
                    YtelsePersonUtils.utledKravForFødselshendelseFGB(barn)
                } else {
                    val personIdenter =
                            hentPersonerFramstiltKravFor(behandling = behandling,
                                                         søknadDTO = søknadGrunnlag?.hentSøknadDto(),
                                                         forrigeBehandling = forrigeBehandling)

                    YtelsePersonUtils.utledKrav(
                            personerMedKrav = persongrunnlagService.hentPersonerPåBehandling(identer = personIdenter,
                                                                                             behandling = behandling),
                            forrigeAndelerTilkjentYtelse = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList())
                }

        validerYtelsePersoner(behandlingId = behandling.id, ytelsePersoner = ytelsePersoner)

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(
                ytelsePersoner = ytelsePersoner,
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList(),
                forrigeAndelerTilkjentYtelse = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList())

        val behandlingsresultat =
                BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersonerMedResultat)
        secureLogger.info("Resultater fra vilkårsvurdering på behandling ${behandling.id}: $ytelsePersonerMedResultat")
        logger.info("Resultat fra vilkårsvurdering på behandling ${behandling.id}: $behandlingsresultat")

        return behandlingsresultat
    }

    private fun validerYtelsePersoner(behandlingId: Long, ytelsePersoner: List<YtelsePerson>) {
        val søkerIdent = persongrunnlagService.hentSøker(behandlingId)?.personIdent?.ident
                         ?: throw Feil("Fant ikke søker på behandling")
        if (ytelsePersoner.any { it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && it.personIdent != søkerIdent }) throw Feil("Barn kan ikke ha ytelsetype utvidet")
        if (ytelsePersoner.any { it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && it.personIdent == søkerIdent }) throw Feil("Søker kan ikke ha ytelsetype ordinær")
    }

    private fun hentPersonerFramstiltKravFor(behandling: Behandling,
                                             søknadDTO: SøknadDTO? = null,
                                             forrigeBehandling: Behandling?): List<String> {
        val barnFraSøknad = søknadDTO?.barnaMedOpplysninger
                                    ?.filter { it.inkludertISøknaden }
                                    ?.map { it.ident }
                            ?: emptyList()
        val utvidetBarnetrygdSøker =
                if (søknadDTO?.underkategori == BehandlingUnderkategori.UTVIDET) listOf(søknadDTO.søkerMedOpplysninger.ident) else emptyList()

        val nyeBarn = persongrunnlagService.finnNyeBarn(forrigeBehandling = forrigeBehandling, behandling = behandling)
                .map { it.personIdent.ident }

        val barnMedEksplisitteAvslag =
                vilkårsvurderingService.finnBarnMedEksplisittAvslagPåBehandling(behandlingId = behandling.id)

        return (barnFraSøknad
                + barnMedEksplisitteAvslag
                + utvidetBarnetrygdSøker
                + nyeBarn).distinct()
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(BehandlingsresultatService::class.java)
        private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}




