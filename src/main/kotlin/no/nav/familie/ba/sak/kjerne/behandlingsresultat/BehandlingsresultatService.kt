package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.BehandlingUnderkategoriDTO
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BehandlingsresultatService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val personidentService: PersonidentService,
    private val persongrunnlagService: PersongrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val featureToggleService: FeatureToggleService
) {

    internal fun finnPersonerFremstiltKravFor(behandling: Behandling, søknadDTO: SøknadDTO?, forrigeBehandling: Behandling?): List<Aktør> {
        val personerFremstiltKravFor = if (behandling.opprettetÅrsak == BehandlingÅrsak.SØKNAD || behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            // alle barna som er krysset av på søknad
            val barnFraSøknad = søknadDTO?.barnaMedOpplysninger
                ?.filter { it.erFolkeregistrert && it.inkludertISøknaden }
                ?.map { personidentService.hentAktør(it.ident) }
                ?: emptyList()

            // hvis det søkes om utvidet skal søker med
            val utvidetBarnetrygdSøker = if (søknadDTO?.underkategori == BehandlingUnderkategoriDTO.UTVIDET) listOf(behandling.fagsak.aktør) else emptyList()

            // alle nye barn
            val nyeBarn = persongrunnlagService.finnNyeBarn(forrigeBehandling = forrigeBehandling, behandling = behandling)
                .map { it.aktør }

            // Hva gjør vi med barn som har fått satt eksplisitt avslag, men det er ikke søkt for personen?

            barnFraSøknad + nyeBarn + utvidetBarnetrygdSøker
        } else if (behandling.erManuellMigrering()) {
            val nåværendePersonopplysningsgrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandling.id)

            nåværendePersonopplysningsgrunnlag.personer.map { it.aktør }
        } else emptyList()

        return personerFremstiltKravFor.distinct()
    }

    internal fun utledBehandlingsresultat(behandlingId: Long): Behandlingsresultat {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling)

        val andelerMedEndringer = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        val forrigeAndelerMedEndringer = forrigeBehandling?.let {
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(it.id)
        } ?: emptyList()

        val barna = persongrunnlagService.hentBarna(behandling)
        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)
        if (barna.isEmpty() && (søknadGrunnlag?.hentUregistrerteBarn() ?: emptyList()).isEmpty()) {
            throw FunksjonellFeil(
                melding = "Ingen barn i personopplysningsgrunnlag ved validering av vilkårsvurdering på behandling ${behandling.id}",
                frontendFeilmelding = "Barn må legges til for å gjennomføre vilkårsvurdering."
            )
        }

        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
            ?: throw Feil("Finner ikke aktiv vilkårsvurdering")

        val personerFremstiltKravFor =
            hentPersonerFramstiltKravFor(
                behandling = behandling,
                søknadDTO = søknadGrunnlag?.hentSøknadDto(),
                forrigeBehandling = forrigeBehandling
            )

        val behandlingsresultatPersoner = persongrunnlagService.hentAktiv(behandling.id)?.søkerOgBarn?.filter {
            when (it.type) {
                PersonType.SØKER ->
                    vilkårsvurdering.personResultater
                        .flatMap { personResultat -> personResultat.vilkårResultater }
                        .any { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

                PersonType.BARN -> true
                PersonType.ANNENPART -> false
            }
        }?.map {
            BehandlingsresultatUtils.utledBehandlingsresultatDataForPerson(
                person = it,
                personerFremstiltKravFor = personerFremstiltKravFor,
                andelerFraForrigeTilkjentYtelse = forrigeAndelerMedEndringer,
                andelerTilkjentYtelse = andelerMedEndringer,
                erEksplisittAvslag = vilkårsvurdering.personResultater.find { personResultat -> personResultat.aktør == it.aktør }
                    ?.harEksplisittAvslag()
                    ?: false
            )
        } ?: emptyList()

        secureLogger.info(
            "Behandlingsresultatpersoner: ${
            behandlingsresultatPersoner.convertDataClassToJson()
            }"
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(
            behandlingsresultatPersoner = behandlingsresultatPersoner,
            uregistrerteBarn = søknadGrunnlag?.hentUregistrerteBarn()?.map {
                MinimertUregistrertBarn(
                    personIdent = it.ident,
                    navn = it.navn
                )
            } ?: emptyList()
        )

        validerYtelsePersoner(behandling, ytelsePersoner = ytelsePersonerMedResultat)

        vilkårsvurdering.let {
            vilkårsvurderingService.oppdater(vilkårsvurdering)
                .also { it.ytelsePersoner = ytelsePersonerMedResultat.writeValueAsString() }
        }

        return utledBehandlingsresultat(
            ytelsePersonerMedResultat,
            andelerMedEndringer,
            forrigeAndelerMedEndringer,
            behandling
        )
    }

    internal fun utledBehandlingsresultat(
        ytelsePersonerMedResultat: List<YtelsePerson>,
        andelerMedEndringer: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        forrigeAndelerMedEndringer: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        behandling: Behandling
    ) =
        BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            ytelsePersoner = ytelsePersonerMedResultat
        )
            .also { secureLogger.info("Resultater fra vilkårsvurdering på behandling $behandling: $ytelsePersonerMedResultat") }
            .also { logger.info("Resultat fra vilkårsvurdering på behandling $behandling: $it") }

    private fun validerYtelsePersoner(behandling: Behandling, ytelsePersoner: List<YtelsePerson>) {
        when (behandling.fagsak.type) {
            FagsakType.NORMAL -> {
                val søkerAktør = persongrunnlagService.hentSøker(behandling.id)?.aktør
                    ?: throw Feil("Fant ikke søker på behandling")
                if (ytelsePersoner.any { it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && it.aktør != søkerAktør }) {
                    throw Feil(
                        "Barn kan ikke ha ytelsetype utvidet"
                    )
                }
                if (ytelsePersoner.any { it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && it.aktør == søkerAktør }) {
                    throw Feil(
                        "Søker kan ikke ha ytelsetype ordinær"
                    )
                }
            }

            FagsakType.INSTITUSJON -> {
                val ytelseType = ytelsePersoner.single().ytelseType
                if (ytelseType != YtelseType.ORDINÆR_BARNETRYGD) {
                    throw Feil(
                        "Kan ikke ha ytelsetype $ytelseType på fagsaktype INSTITUSJON"
                    )
                }
            }

            FagsakType.BARN_ENSLIG_MINDREÅRIG -> {
                ytelsePersoner.single()
            }
        }
    }

    private fun hentPersonerFramstiltKravFor(
        behandling: Behandling,
        søknadDTO: SøknadDTO? = null,
        forrigeBehandling: Behandling?
    ): List<Aktør> {
        val barnFraSøknad = søknadDTO?.barnaMedOpplysninger
            ?.filter { it.inkludertISøknaden && it.erFolkeregistrert }
            ?.map { personidentService.hentAktør(it.ident) }
            ?: emptyList()

        val utvidetBarnetrygdSøker = søknadDTO?.let {
            when (it.underkategori) {
                BehandlingUnderkategoriDTO.UTVIDET -> listOf(behandling.fagsak.aktør)
                BehandlingUnderkategoriDTO.INSTITUSJON -> emptyList()
                BehandlingUnderkategoriDTO.ORDINÆR -> emptyList()
            }
        } ?: emptyList()

        val nyeBarn = persongrunnlagService.finnNyeBarn(forrigeBehandling = forrigeBehandling, behandling = behandling)
            .map { it.aktør }

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
