package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_OG_ENDRET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_UTBETALING
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_UTEN_UTBETALING
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.IKKE_VURDERT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.slf4j.LoggerFactory
import java.time.YearMonth

object BehandlingsresultatValideringUtils {
    private val logger = LoggerFactory.getLogger(BehandlingsresultatValideringUtils::class.java)

    internal fun validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
        personerFremstiltKravFor: List<Aktør>,
        personResultater: Set<PersonResultat>,
    ) {
        val personerSomHarEksplisittAvslag = personResultater.filter { it.harEksplisittAvslag() }

        if (personerSomHarEksplisittAvslag.any { !personerFremstiltKravFor.contains(it.aktør) && !it.erSøkersResultater() }) {
            throw FunksjonellFeil(
                frontendFeilmelding = "Det eksisterer personer som har fått eksplisitt avslag, men som det ikke er blitt fremstilt krav for.",
                melding = "Det eksisterer personer som har fått eksplisitt avslag, men som det ikke har blitt fremstilt krav for.",
            )
        }
    }

    internal fun validerBehandlingsresultat(
        behandling: Behandling,
    ) {
        validerBehandlingsresultatMotBehandlingstype(behandling)
        validerBehandlingsresultatMotBehandlingsårsak(behandling)
    }

    private fun validerBehandlingsresultatMotBehandlingstype(
        behandling: Behandling,
    ) {
        when {
            behandling.erFørstegangsbehandling() -> {
                val ugyldigeBehandlingsresultaterForTypeFørstegangsbehandling =
                    setOf(AVSLÅTT_OG_OPPHØRT, ENDRET_UTBETALING, ENDRET_UTEN_UTBETALING, ENDRET_OG_OPPHØRT, OPPHØRT, FORTSATT_INNVILGET, IKKE_VURDERT)
                if (behandling.resultat in ugyldigeBehandlingsresultaterForTypeFørstegangsbehandling) {
                    throw FunksjonellFeil("Behandlingsresultatet '${behandling.resultat.displayName}' er ugyldig i kombinasjon med behandlingstype '${behandling.type.visningsnavn}'.")
                }
            }

            behandling.erRevurdering() -> {
                if (behandling.resultat == IKKE_VURDERT) {
                    throw FunksjonellFeil("Behandlingsresultatet '${behandling.resultat.displayName}' er ugyldig i kombinasjon med behandlingstype '${behandling.type.visningsnavn}'.")
                }
            }
        }
    }

    private fun validerBehandlingsresultatMotBehandlingsårsak(
        behandling: Behandling,
    ) {
        when {
            behandling.erKlage() -> {
                val ugyldigeBehandlingsresultaterForÅrsakKlage =
                    setOf(AVSLÅTT_OG_OPPHØRT, AVSLÅTT_ENDRET_OG_OPPHØRT, AVSLÅTT_OG_ENDRET, AVSLÅTT)
                if (behandling.resultat in ugyldigeBehandlingsresultaterForÅrsakKlage) {
                    throw FunksjonellFeil("Behandlingsårsak '${behandling.opprettetÅrsak.visningsnavn}' er ugyldig i kombinasjon med resultat '${behandling.resultat.displayName}'.")
                }
            }

            behandling.erManuellMigrering() -> {
                if (behandling.resultat.erAvslått() || behandling.resultat == DELVIS_INNVILGET) {
                    throw FunksjonellFeil(
                        "Du har fått behandlingsresultatet ${behandling.resultat.displayName}. " +
                            "Dette er ikke støttet på migreringsbehandlinger. Meld sak i Porten om du er uenig i resultatet.",
                    )
                }
            }

            behandling.erOmregning() -> {
                if (behandling.resultat !in setOf(FORTSATT_INNVILGET, FORTSATT_OPPHØRT)) {
                    throw Feil("Behandling $behandling er omregningssak, men er ikke uendret behandlingsresultat")
                }
            }
        }
    }

    fun validerIngenEndringTilbakeITid(
        andelerDenneBehandlingen: Collection<AndelTilkjentYtelse>,
        andelerForrigeBehandling: Collection<AndelTilkjentYtelse>,
        nåMåned: YearMonth,
    ) {
        val forrigeMåned = nåMåned.minusMonths(1)
        val andelerIFortidenTidslinje = andelerDenneBehandlingen.tilTidslinjerPerAktørOgType().beskjærTilOgMed(forrigeMåned.sisteDagIInneværendeMåned())
        val andelerIFortidenForrigeBehanldingTidslinje = andelerForrigeBehandling.tilTidslinjerPerAktørOgType().beskjærTilOgMed(forrigeMåned.sisteDagIInneværendeMåned())

        val endringerIAndelerTilbakeITidTidslinjer =
            andelerIFortidenTidslinje.outerJoin(andelerIFortidenForrigeBehanldingTidslinje) { nyAndel, gammelAndel ->
                if (nyAndel?.kalkulertUtbetalingsbeløp != gammelAndel?.kalkulertUtbetalingsbeløp) {
                    ErEndringIAndel(andelForrigeBehandling = gammelAndel, andelDenneBehandlingen = nyAndel)
                } else {
                    IngenEndringIAndel
                }
            }

        endringerIAndelerTilbakeITidTidslinjer.kastFeilOgLoggVedEndringerIAndeler()
    }

    fun validerSatsErUendret(
        andelerDenneBehandlingen: Collection<AndelTilkjentYtelse>,
        andelerForrigeBehandling: Collection<AndelTilkjentYtelse>,
    ) {
        val andelerDenneBehandlingTidslinje = andelerDenneBehandlingen.tilTidslinjerPerAktørOgType()
        val andelerForrigeBehanldingTidslinje = andelerForrigeBehandling.tilTidslinjerPerAktørOgType()

        val endringISatsTidslinjer =
            andelerDenneBehandlingTidslinje.outerJoin(andelerForrigeBehanldingTidslinje) { nyAndel, gammelAndel ->
                if (nyAndel?.sats != gammelAndel?.sats && nyAndel?.kalkulertUtbetalingsbeløp != 0 && gammelAndel?.kalkulertUtbetalingsbeløp != 0) {
                    ErEndringIAndel(andelForrigeBehandling = gammelAndel, andelDenneBehandlingen = nyAndel)
                } else {
                    IngenEndringIAndel
                }
            }

        endringISatsTidslinjer.kastFeilOgLoggVedEndringerIAndeler()
    }

    private fun Map<Pair<Aktør, YtelseType>, Tidslinje<EndringIAndel>>.kastFeilOgLoggVedEndringerIAndeler() {
        this.forEach { (aktør, ytelsetype), endringIAndelTidslinje ->
            endringIAndelTidslinje.tilPerioderIkkeNull().forEach {
                if (it.verdi is ErEndringIAndel) {
                    val erEndringIAndel = it.verdi as ErEndringIAndel
                    val fom = it.fom?.toYearMonth() ?: MIN_MÅNED
                    val tom = it.tom?.toYearMonth() ?: MAX_MÅNED
                    secureLogger.info(
                        "Det er en uforventet endring i $ytelsetype-andel for $aktør i perioden $fom til $tom.\n" +
                            "Andel denne behandlingen: ${erEndringIAndel.andelDenneBehandlingen}\n" +
                            "Andel forrige behandling: ${erEndringIAndel.andelForrigeBehandling}",
                    )
                    throw Feil("Det er en uforventet endring i andel. Gjelder andel i perioden $fom til $tom. Se secure log for mer detaljer.")
                }
            }
        }
    }

    fun validerKompetanse(
        kompetanser: Collection<Kompetanse>,
    ) {
        kompetanser.forEach { kompetanse ->
            val erNorgeSekundærland = kompetanse.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND
            if (erNorgeSekundærland && setOf(kompetanse.søkersAktivitetsland, kompetanse.annenForeldersAktivitetsland, kompetanse.barnetsBostedsland).all { it == "NO" }) {
                throw FunksjonellFeil("Dersom Norge er sekundærland, må søkers aktivitetsland, annen forelders aktivitetsland eller barnets bostedsland være satt til noe annet enn Norge")
            }
        }
    }

    fun validerFinnmarkstilleggBehandling(
        andelerNåværendeBehandling: Collection<AndelTilkjentYtelse>,
        andelerForrigeBehandling: Collection<AndelTilkjentYtelse>,
        inneværendeMåned: YearMonth,
    ) {
        validerAtDetIkkeHarVærtEndringIUtbetalingUtenomYtelseType(
            andelerNåværendeBehandling = andelerNåværendeBehandling,
            andelerForrigeBehandling = andelerForrigeBehandling,
            ytelseType = YtelseType.FINNMARKSTILLEGG,
            behandlingÅrsak = BehandlingÅrsak.FINNMARKSTILLEGG,
        )

        validerAtIngenAndelerMedYtelseTypeErInnvilgetFramITid(
            andelerNåværendeBehandling = andelerNåværendeBehandling,
            andelerForrigeBehandling = andelerForrigeBehandling,
            ytelseType = YtelseType.FINNMARKSTILLEGG,
            inneværendeMåned = inneværendeMåned,
        )
    }

    fun validerSvalbardtilleggBehandling(
        andelerNåværendeBehandling: Collection<AndelTilkjentYtelse>,
        andelerForrigeBehandling: Collection<AndelTilkjentYtelse>,
        inneværendeMåned: YearMonth,
    ) {
        validerAtDetIkkeHarVærtEndringIUtbetalingUtenomYtelseType(
            andelerNåværendeBehandling = andelerNåværendeBehandling,
            andelerForrigeBehandling = andelerForrigeBehandling,
            ytelseType = YtelseType.SVALBARDTILLEGG,
            behandlingÅrsak = BehandlingÅrsak.SVALBARDTILLEGG,
        )

        validerAtIngenAndelerMedYtelseTypeErInnvilgetFramITid(
            andelerNåværendeBehandling = andelerNåværendeBehandling,
            andelerForrigeBehandling = andelerForrigeBehandling,
            ytelseType = YtelseType.SVALBARDTILLEGG,
            inneværendeMåned = inneværendeMåned,
        )
    }

    private fun validerAtDetIkkeHarVærtEndringIUtbetalingUtenomYtelseType(
        andelerNåværendeBehandling: Collection<AndelTilkjentYtelse>,
        andelerForrigeBehandling: Collection<AndelTilkjentYtelse>,
        ytelseType: YtelseType,
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val andelerUtenomYtelseTypeDenneBehandling = andelerNåværendeBehandling.filterNot { it.type == ytelseType }
        val andelerUtenomYtelseTypeForrigeBehandling = andelerForrigeBehandling.filterNot { it.type == ytelseType }

        val erEndringIUtbetaling =
            EndringIUtbetalingUtil
                .lagEndringIUtbetalingTidslinje(
                    nåværendeAndeler = andelerUtenomYtelseTypeDenneBehandling,
                    forrigeAndeler = andelerUtenomYtelseTypeForrigeBehandling,
                ).tilPerioder()
                .any { it.verdi == true }

        if (erEndringIUtbetaling) {
            throw Feil("Det er oppdaget forskjell i utbetaling utenom $ytelseType andeler. Dette kan ikke skje i en behandling der årsak er $behandlingÅrsak, og den automatiske kjøring stoppes derfor.")
        }
    }

    private fun validerAtIngenAndelerMedYtelseTypeErInnvilgetFramITid(
        andelerNåværendeBehandling: Collection<AndelTilkjentYtelse>,
        andelerForrigeBehandling: Collection<AndelTilkjentYtelse>,
        ytelseType: YtelseType,
        inneværendeMåned: YearMonth,
    ) {
        val nåværendeAndelerMedYtelseTypeTidslinjePerAktør =
            andelerNåværendeBehandling
                .filter { it.type == ytelseType }
                .tilTidslinjerPerAktørOgType()
                .mapKeys { it.key.first }

        val forrigeAndelerMedYtelseTypeTidslinjePerAktør =
            andelerForrigeBehandling
                .filter { it.type == ytelseType }
                .tilTidslinjerPerAktørOgType()
                .mapKeys { it.key.first }

        val endringIAndelMedYtelseTypeTidslinjePerAktør =
            nåværendeAndelerMedYtelseTypeTidslinjePerAktør
                .outerJoin(forrigeAndelerMedYtelseTypeTidslinjePerAktør) { nyAndel, gammelAndel ->
                    nyAndel?.kalkulertUtbetalingsbeløp != gammelAndel?.kalkulertUtbetalingsbeløp
                }

        val tidligsteInnvilgelsesTidspunktPerAktør =
            endringIAndelMedYtelseTypeTidslinjePerAktør
                .mapValues { endringIAndelMedYtelseTypeTidslinje ->
                    endringIAndelMedYtelseTypeTidslinje
                        .value
                        .tilPerioder()
                        .filter { it.verdi == true }
                        .minOfOrNull { it.fom ?: throw Feil("Fra og med-dato må være satt for AndelTilkjentYtelse") }
                        ?.toYearMonth()
                }

        val nesteMåned = inneværendeMåned.plusMonths(1)

        val andelErInnvilgetOmToMånederEllerSenere = tidligsteInnvilgelsesTidspunktPerAktør.any { it.value != null && it.value!! > nesteMåned }
        if (andelErInnvilgetOmToMånederEllerSenere) {
            val andelErInnvilgetInneværendeMånedEllerTidligere = tidligsteInnvilgelsesTidspunktPerAktør.any { it.value != null && it.value!! < nesteMåned }
            if (andelErInnvilgetInneværendeMånedEllerTidligere) {
                throw Feil(
                    "Det eksisterer $ytelseType-andeler som er innvilget inneværende måned eller tidligere, " +
                        "samtidig som det eksisterer andeler som blir innvilget mer enn en måned fram i tid. " +
                        "Dette kan ikke behandles automatisk, og behandlingen stoppes derfor.",
                )
            }

            throw RekjørSenereException(
                årsak =
                    "Det eksisterer $ytelseType-andeler som er innvilget mer enn en måned fram i tid. " +
                        "Disse andelene kan ikke innvilges ennå. Prøver igjen neste måned.",
                triggerTid = nesteMåned.førsteDagIInneværendeMåned().atTime(6, 0),
            )
        }
    }
}

private sealed interface EndringIAndel

private object IngenEndringIAndel : EndringIAndel

private data class ErEndringIAndel(
    val andelForrigeBehandling: AndelTilkjentYtelse?,
    val andelDenneBehandlingen: AndelTilkjentYtelse?,
) : EndringIAndel
