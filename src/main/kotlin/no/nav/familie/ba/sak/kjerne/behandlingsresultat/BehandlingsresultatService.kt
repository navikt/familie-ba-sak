package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.ekstern.restDomene.BehandlingUnderkategoriDTO
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
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
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val kompetanseService: KompetanseService
) {

    internal fun finnPersonerFremstiltKravFor(behandling: Behandling, søknadDTO: SøknadDTO?, forrigeBehandling: Behandling?) =
        when {
            behandling.opprettetÅrsak == BehandlingÅrsak.SØKNAD -> {
                // alle barna som er krysset av på søknad
                val barnFraSøknad = søknadDTO?.barnaMedOpplysninger
                    ?.filter { it.erFolkeregistrert && it.inkludertISøknaden }
                    ?.map { personidentService.hentAktør(it.ident) }
                    ?: emptyList()

                // hvis det søkes om utvidet skal søker med
                val utvidetBarnetrygdSøker = if (søknadDTO?.underkategori == BehandlingUnderkategoriDTO.UTVIDET) listOf(behandling.fagsak.aktør) else emptyList()

                barnFraSøknad + utvidetBarnetrygdSøker
            }
            behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE -> persongrunnlagService.finnNyeBarn(behandling, forrigeBehandling).map { it.aktør }
            behandling.erManuellMigrering() -> persongrunnlagService.hentAktivThrows(behandling.id).personer.map { it.aktør }
            else -> emptyList()
        }

    internal fun utledBehandlingsresultat(behandlingId: Long): Behandlingsresultat {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val forrigeBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id)

        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)

        val forrigeAndelerTilkjentYtelse = forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = it.id) } ?: emptyList()
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)
        val endretUtbetalingAndeler = endretUtbetalingAndelService.hentForBehandling(behandlingId = behandlingId)
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = behandlingId)

        val personerFremstiltKravFor = finnPersonerFremstiltKravFor(
            behandling = behandling,
            søknadDTO = søknadGrunnlag?.hentSøknadDto(),
            forrigeBehandling = forrigeBehandling
        )

        // 1 SØKNAD
        val søknadsresultat = if (behandling.opprettetÅrsak in listOf(BehandlingÅrsak.FØDSELSHENDELSE, BehandlingÅrsak.SØKNAD) || behandling.erManuellMigrering()) {
            BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
                nåværendeAndeler = andelerTilkjentYtelse,
                forrigeAndeler = forrigeAndelerTilkjentYtelse,
                endretUtbetalingAndeler = endretUtbetalingAndeler,
                personerFremstiltKravFor = personerFremstiltKravFor,
                nåværendePersonResultater = vilkårsvurdering.personResultater
            )
        } else {
            null
        }

        // 2 ENDRINGER
        val endringsresultat = if (forrigeBehandling != null) {
            val forrigeEndretUtbetalingAndeler = endretUtbetalingAndelService.hentForBehandling(behandlingId = forrigeBehandling.id)
            val forrigeVilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = forrigeBehandling.id)
            val kompetanser = kompetanseService.hentKompetanser(behandlingId = BehandlingId(behandlingId))
            val forrigeKompetanser = kompetanseService.hentKompetanser(behandlingId = BehandlingId(forrigeBehandling.id))

            BehandlingsresultatUtils.utledEndringsresultat(
                nåværendeAndeler = andelerTilkjentYtelse,
                forrigeAndeler = forrigeAndelerTilkjentYtelse,
                nåværendeEndretAndeler = endretUtbetalingAndeler,
                forrigeEndretAndeler = forrigeEndretUtbetalingAndeler,
                nåværendePersonResultat = vilkårsvurdering.personResultater,
                forrigePersonResultat = forrigeVilkårsvurdering.personResultater,
                nåværendeKompetanser = kompetanser.toList(),
                forrigeKompetanser = forrigeKompetanser.toList(),
                personerFremstiltKravFor = personerFremstiltKravFor
            )
        } else {
            BehandlingsresultatUtils.Endringsresultat.INGEN_ENDRING
        }

        // 3 OPPHØR
        val opphørsresultat = BehandlingsresultatUtils.hentOpphørsresultatPåBehandling(
            nåværendeAndeler = andelerTilkjentYtelse,
            forrigeAndeler = forrigeAndelerTilkjentYtelse
        )

        // KOMBINER
        val behandlingsresultat = BehandlingsresultatUtils.kombinerResultaterTilBehandlingsresultat(søknadsresultat, endringsresultat, opphørsresultat)

        return behandlingsresultat
    }

    @Deprecated("Skal erstattes av ny metode")
    internal fun utledBehandlingsresultatGammel(behandlingId: Long): Behandlingsresultat {
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

        return utledBehandlingsresultatGammel(
            ytelsePersonerMedResultat,
            andelerMedEndringer,
            forrigeAndelerMedEndringer,
            behandling
        )
    }

    internal fun utledBehandlingsresultatGammel(
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
