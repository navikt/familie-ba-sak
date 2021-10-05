package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.objectMapper
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
            frontendFeilmelding = "Barn må legges til for å gjennomføre vilkårsvurdering."
        )

        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)

            ?: throw Feil("Finner ikke aktiv vilkårsvurdering")

        val personerFremstiltKravFor =
            hentPersonerFramstiltKravFor(
                behandling = behandling,
                søknadDTO = søknadGrunnlag?.hentSøknadDto(),
                forrigeBehandling = forrigeBehandling
            )

        val personerVurdertIDenneBehandlingen = persongrunnlagService.hentAktiv(behandling.id)?.personer?.filter {
            val vilkårResultater =
                vilkårsvurdering.personResultater.find { personResultat -> personResultat.personIdent == it.personIdent.ident }?.vilkårResultater
                    ?: emptyList()

            val vilkårVurdertIDenneBehandlingen =
                vilkårResultater.filter { vilkårResultat -> vilkårResultat.behandlingId == behandlingId }

            when (it.type) {
                PersonType.BARN -> vilkårVurdertIDenneBehandlingen.isNotEmpty()
                PersonType.SØKER -> vilkårVurdertIDenneBehandlingen.any { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
                else -> false
            }
        }

        val behandlingsresultatPersoner = personerVurdertIDenneBehandlingen?.map {
            BehandlingsresultatUtils.utledBehandlingsresultatDataForPerson(
                person = it,
                personerFremstiltKravFor = personerFremstiltKravFor,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                tilkjentYtelse = tilkjentYtelse,
                erEksplisittAvslag = vilkårsvurdering.personResultater.find { personResultat -> personResultat.personIdent == it.personIdent.ident }
                    ?.harEksplisittAvslag()
                    ?: false
            )
        } ?: emptyList()

        secureLogger.info("Behandlingsresultatpersoner: ${behandlingsresultatPersoner.convertDataClassToJson()}")

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(behandlingsresultatPersoner)

        validerYtelsePersoner(behandlingId = behandling.id, ytelsePersoner = ytelsePersonerMedResultat)

        vilkårsvurdering.let {
            vilkårsvurderingService.oppdater(vilkårsvurdering)
                .also { it.ytelsePersoner = ytelsePersonerMedResultat.writeValueAsString() }
        }

        val behandlingsresultat =
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersonerMedResultat)
        secureLogger.info("Resultater fra vilkårsvurdering på behandling $behandling: $ytelsePersonerMedResultat")
        logger.info("Resultat fra vilkårsvurdering på behandling $behandling: $behandlingsresultat")

        return behandlingsresultat
    }

    private fun validerYtelsePersoner(behandlingId: Long, ytelsePersoner: List<YtelsePerson>) {
        val søkerIdent = persongrunnlagService.hentSøker(behandlingId)?.personIdent?.ident
            ?: throw Feil("Fant ikke søker på behandling")
        if (ytelsePersoner.any { it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && it.personIdent != søkerIdent }) throw Feil("Barn kan ikke ha ytelsetype utvidet")
        if (ytelsePersoner.any { it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && it.personIdent == søkerIdent }) throw Feil("Søker kan ikke ha ytelsetype ordinær")
    }

    private fun hentPersonerFramstiltKravFor(
        behandling: Behandling,
        søknadDTO: SøknadDTO? = null,
        forrigeBehandling: Behandling?
    ): List<String> {
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

        return (
            barnFraSøknad +
                barnMedEksplisitteAvslag +
                utvidetBarnetrygdSøker +
                nyeBarn
            ).distinct()
    }

    private fun List<YtelsePerson>.writeValueAsString(): String = objectMapper.writeValueAsString(this)

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(BehandlingsresultatService::class.java)
        private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
