package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.config.FeatureToggleConfig
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
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
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

    fun utledBehandlingsresultat(behandling: Behandling, forrigeBehandling: Behandling?) {
        // 1 SØKNAD
        val søknadsresultat = if (behandling.opprettetÅrsak == BehandlingÅrsak.SØKNAD || behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            utledResultatPåSøknad(behandling = behandling)
        } else emptyList()

        // 2 ENDRINGER
        val endringResultat = if (forrigeBehandling != null) erEndringerPåBehandlingIForholdTilForrige() else emptyList()

        // 3 OPPHØR
        val opphørResultat = erOpphørPåBehandling()

        // KOMBINER
        val behandlingsresultat = kombinerResultater(
            søknadsresultat = søknadsresultat,
            endretResultat = endringResultat,
            opphørResultat = opphørResultat
        )

        // VALIDERING
        validerBehandlingsresultat()
    }

    // Innvilget, avslått, fortsatt innvilget, (delvis innvilget)
    private fun utledResultatPåSøknad(behandling: Behandling): List<Behandlingsresultat> {
        // Henter relevante ting
        val forrigeAndeler = emptyList<AndelTilkjentYtelseMedEndreteUtbetalinger>()
        val nåværendeAndeler = emptyList<AndelTilkjentYtelseMedEndreteUtbetalinger>()
        val nåværendePersonResultater = emptyList<PersonResultat>()

        // Finner personer det er søkt for
        val personerFremstiltKravFor = finnPersonerFremstiltKravFor()

        // Gjør utledningen
        return utledResultatPåSøknadUtil(forrigeAndelerPrPerson = forrigeAndeler.groupBy { it.aktør }, nåværendeAndelerPrPerson = nåværendeAndeler.groupBy { it.aktør }, nåværendePersonResultater = nåværendePersonResultater, personerFremstiltKravFor = personerFremstiltKravFor)
    }

    private fun finnPersonerFremstiltKravFor(): List<Aktør> {
        // hvis søknad: alle personer det er krysset av for og evt søker hvis det er krysset av for utvidet
        // hvis fødselshendelse: barnet/barna som er født
        // hva gjør vi med uregistrerte barn??
        return emptyList()
    }

    private fun utledResultatPåSøknadUtil(
        forrigeAndelerPrPerson: Map<Aktør, List<AndelTilkjentYtelseMedEndreteUtbetalinger>>,
        nåværendeAndelerPrPerson: Map<Aktør, List<AndelTilkjentYtelseMedEndreteUtbetalinger>>,
        nåværendePersonResultater: List<PersonResultat>,
        personerFremstiltKravFor: List<Aktør>
    ): List<Behandlingsresultat> {
        val resultaterFraAndeler = personerFremstiltKravFor.flatMap {
            utledSøknadResultatFraAndelerTilkjentYtelseForPerson(forrigeAndelerPrPerson.getOrDefault(it, emptyList()), nåværendeAndelerPrPerson.getOrDefault(it, emptyList()))
        }

        val erEksplisittAvslagPåMinstEnPerson = nåværendePersonResultater.any { it.vilkårResultater.erEksplisittAvslagPåPerson() }

        val resultater = if (erEksplisittAvslagPåMinstEnPerson) resultaterFraAndeler.plus(Behandlingsresultat.AVSLÅTT).distinct() else resultaterFraAndeler.distinct()

        // kombinere resultatene til et behandlignsresultat
        // hvis ingenting av det over (ikke innvilget eller avslag) -> fortsatt innvilget
        return emptyList()
    }

    private fun utledSøknadResultatFraAndelerTilkjentYtelseForPerson(
        forrigeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        nåværendeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
    ): List<Behandlingsresultat> {
        // hvis nåværende = null -> ikke bry oss
        // hvis nåværende > 0 og ikke lik forrige -> innvilget
        // hvis nåværende = 0 og ikke lik forrige -> er det pga differanseberegning eller delt bosted endring? -> innvilget, er det pga etterbetaling 3 år osv -> avslått
        return emptyList()
    }

    private fun Set<VilkårResultat>.erEksplisittAvslagPåPerson(): Boolean {
        // sjekk om vilkårresultater inneholder eksplisitt avslag på et vilkår
        return this.any { it.erEksplisittAvslagPåSøknad == true }
    }

    // Endring
    private fun erEndringerPåBehandlingIForholdTilForrige(): List<Behandlingsresultat> {
        return if (erEndringIBeløp() || erEndringerIkkeIBeløp()) listOf(Behandlingsresultat.ENDRET_UTBETALING)
        else emptyList()
    }

    private fun erEndringIBeløp(): Boolean {
        // Hvis revurdering eller ikke søkt for personen: alle endringer i beløp
        // Hvis søkt for personen: alle endringer fra beløp til null (dobbeltsjekk med Eivind)
        return false
    }

    private fun erEndringerIkkeIBeløp(): Boolean {
        // Finn endringer i "overlappende perioder" fra forrige og nåværende behandling:
        // - Vilkårsvurdering (splitt, utdypende vv, regelverk)
        // - Kompetanseskjema
        // - Endringsperioder (trenger vi å se på prosent, barn og årsak?)
        return false
    }

    // Opphørt, fortsatt opphørt
    private fun erOpphørPåBehandling(): List<Behandlingsresultat> {
        // Hvis det er opphørt og ikke opphørt i forrige behandling -> opphørt
        // Hvis opphørt tidligere i denne behandlingen enn i forrige -> opphørt
        // Hvis opphørt på samme dato som i forrige behandling -> fortsatt opphørt
        return emptyList()
    }

    private fun kombinerResultater(søknadsresultat: List<Behandlingsresultat>, endretResultat: List<Behandlingsresultat>, opphørResultat: List<Behandlingsresultat>): List<Behandlingsresultat> {
        // hvis fortsatt opphørt & noe annet -> ikke ta med fortsatt opphørt i resultatet
        // hvis fortsatt innvilget + fortsatt opphørt -> hva blir resultatet??
        return emptyList()
    }

    private fun validerBehandlingsresultat() {
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
            ytelsePersoner = ytelsePersonerMedResultat,
            sjekkOmUtvidaBarnetrygdErEndra = featureToggleService.isEnabled(FeatureToggleConfig.SJEKK_OM_UTVIDET_ER_ENDRET_BEHANDLINGSRESULTAT)
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
