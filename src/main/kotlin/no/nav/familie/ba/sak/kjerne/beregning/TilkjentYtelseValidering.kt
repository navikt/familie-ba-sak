package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.SatsendringFeil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.Utils.slåSammen
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.SatsendringSvar
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.maksBeløp
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjeMedAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringUtil.tilFørsteEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.søker
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun hentGyldigEtterbetaling3ÅrFom(kravDato: LocalDate): YearMonth =
    kravDato
        .minusYears(3)
        .toYearMonth()

fun hentGyldigEtterbetaling3MndFom(kravDato: LocalDate): YearMonth =
    kravDato
        .minusMonths(3)
        .toYearMonth()

fun hentSøkersAndeler(
    andeler: Iterable<AndelTilkjentYtelse>,
    søker: PersonEnkel,
) = andeler.filter { it.aktør == søker.aktør }

fun hentBarnasAndeler(
    andeler: Iterable<AndelTilkjentYtelse>,
    barna: List<PersonEnkel>,
) = barna.map { barn ->
    barn to andeler.filter { it.aktør == barn.aktør }
}

/**
 * Ekstra sikkerhet rundt hva som utbetales som på sikt vil legges inn i
 * de respektive stegene SB håndterer slik at det er lettere for SB å rette feilene.
 */
object TilkjentYtelseValidering {
    internal fun validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
        andelerFraForrigeBehandling: Collection<AndelTilkjentYtelse>,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ) {
        val andelerGruppert = andelerTilkjentYtelse.tilTidslinjerPerAktørOgType()
        val forrigeAndelerGruppert = andelerFraForrigeBehandling.tilTidslinjerPerAktørOgType()

        andelerGruppert
            .outerJoin(forrigeAndelerGruppert) { nåværendeAndel, forrigeAndel ->
                when {
                    forrigeAndel == null && nåværendeAndel != null -> {
                        throw SatsendringFeil(
                            melding =
                                "Satsendring kan ikke legge til en andel som ikke var der i forrige behandling. " +
                                    "Satsendringen prøver å legge til en andel i perioden ${nåværendeAndel.stønadFom} - ${nåværendeAndel.stønadTom}",
                            satsendringSvar = SatsendringSvar.BEHANDLING_HAR_FEIL_PÅ_ANDELER,
                        ).also { secureLogger.info("forrigeAndel er null, nåværendeAndel=$nåværendeAndel") }
                    }

                    forrigeAndel != null && nåværendeAndel == null -> {
                        throw SatsendringFeil(
                            melding =
                                "Satsendring kan ikke fjerne en andel som fantes i forrige behandling. " +
                                    "Satsendringen prøver å fjerne andel i perioden ${forrigeAndel.stønadFom} - ${forrigeAndel.stønadTom}",
                            satsendringSvar = SatsendringSvar.BEHANDLING_HAR_FEIL_PÅ_ANDELER,
                        ).also { secureLogger.info("nåværendeAndel er null, forrigeAndel=$forrigeAndel") }
                    }

                    forrigeAndel != null && forrigeAndel.prosent != nåværendeAndel?.prosent -> {
                        throw SatsendringFeil(
                            melding =
                                "Satsendring kan ikke endre på prosenten til en andel. " +
                                    "Gjelder perioden ${forrigeAndel.stønadFom} - ${forrigeAndel.stønadTom}. " +
                                    "Prøver å endre fra ${forrigeAndel.prosent} til ${nåværendeAndel?.prosent} prosent.",
                            satsendringSvar = SatsendringSvar.BEHANDLING_HAR_FEIL_PÅ_ANDELER,
                        ).also { secureLogger.info("nåværendeAndel=$nåværendeAndel, forrigeAndel=$forrigeAndel") }
                    }

                    forrigeAndel != null && forrigeAndel.type != nåværendeAndel?.type -> {
                        throw SatsendringFeil(
                            melding =
                                "Satsendring kan ikke endre YtelseType til en andel. " +
                                    "Gjelder perioden ${forrigeAndel.stønadFom} - ${forrigeAndel.stønadTom}. " +
                                    "Prøver å endre fra ytelsetype ${forrigeAndel.type} til ${nåværendeAndel?.type}.",
                            satsendringSvar = SatsendringSvar.BEHANDLING_HAR_FEIL_PÅ_ANDELER,
                        ).also { secureLogger.info("nåværendeAndel=$nåværendeAndel, forrigeAndel=$forrigeAndel") }
                    }

                    else -> {
                        false
                    }
                }
            }.values
            .map { it.tilPerioder() } // Må kalle på .perioder() for at feilene over skal bli kastet
    }

    fun finnAktørIderMedUgyldigEtterbetalingsperiode(
        forrigeAndelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        gyldigEtterbetalingFom: YearMonth,
    ): List<Aktør> {
        val aktører = unikeAntører(andelerTilkjentYtelse, forrigeAndelerTilkjentYtelse)

        val personerMedUgyldigEtterbetaling =
            aktører.mapNotNull { aktør ->
                val andelerTilkjentYtelseForPerson = andelerTilkjentYtelse.filter { it.aktør == aktør }
                val forrigeAndelerTilkjentYtelseForPerson = forrigeAndelerTilkjentYtelse.filter { it.aktør == aktør }

                aktør.takeIf {
                    erUgyldigEtterbetalingPåPerson(
                        forrigeAndelerTilkjentYtelseForPerson,
                        andelerTilkjentYtelseForPerson,
                        gyldigEtterbetalingFom,
                    )
                }
            }

        return personerMedUgyldigEtterbetaling
    }

    private fun unikeAntører(
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        forrigeAndelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): Set<Aktør> {
        val aktørIderFraAndeler = andelerTilkjentYtelse.map { it.aktør }
        val aktøerIderFraForrigeAndeler = forrigeAndelerTilkjentYtelse.map { it.aktør }
        return (aktørIderFraAndeler + aktøerIderFraForrigeAndeler).toSet()
    }

    fun erUgyldigEtterbetalingPåPerson(
        forrigeAndelerForPerson: List<AndelTilkjentYtelse>,
        andelerForPerson: List<AndelTilkjentYtelse>,
        gyldigEtterbetalingFom: YearMonth,
    ): Boolean =
        YtelseType.entries.any { ytelseType ->
            val forrigeAndelerForPersonOgType = forrigeAndelerForPerson.filter { it.type == ytelseType }
            val andelerForPersonOgType = andelerForPerson.filter { it.type == ytelseType }

            val etterbetalingTidslinje =
                EndringIUtbetalingUtil.lagEtterbetalingstidslinjeForPersonOgType(
                    nåværendeAndeler = andelerForPersonOgType,
                    forrigeAndeler = forrigeAndelerForPersonOgType,
                )

            val førsteMånedMedEtterbetaling = etterbetalingTidslinje.tilFørsteEndringstidspunkt()

            førsteMånedMedEtterbetaling != null && førsteMånedMedEtterbetaling < gyldigEtterbetalingFom
        }

    fun validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
        tilkjentYtelse: TilkjentYtelse,
        søkerOgBarn: List<PersonEnkel>,
    ) {
        val søker = søkerOgBarn.søker()
        val barna = søkerOgBarn.barn()

        val tidslinjeMedAndeler = tilkjentYtelse.tilTidslinjeMedAndeler()

        val fagsakType = tilkjentYtelse.behandling.fagsak.type

        tidslinjeMedAndeler.tilPerioderIkkeNull().map { it.verdi }.forEach {
            val søkersAndeler = hentSøkersAndeler(it, søker)
            val barnasAndeler = hentBarnasAndeler(it, barna)

            validerAtBeløpForPartStemmerMedSatser(person = søker, andeler = søkersAndeler, fagsakType = fagsakType)

            barnasAndeler.forEach { (barn, andeler) ->
                validerAtBeløpForPartStemmerMedSatser(person = barn, andeler = andeler, fagsakType = fagsakType)
            }
        }
    }

    fun validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
        behandlendeBehandlingTilkjentYtelse: TilkjentYtelse,
        barnMedAndreRelevanteTilkjentYtelser: List<Pair<PersonEnkel, List<TilkjentYtelse>>>,
        søkerOgBarn: List<PersonEnkel>,
    ) {
        val barna = søkerOgBarn.barn().sortedBy { it.fødselsdato }

        val barnasAndeler = hentBarnasAndeler(behandlendeBehandlingTilkjentYtelse.andelerTilkjentYtelse.toList(), barna)

        val barnMedUtbetalingsikkerhetFeil = mutableMapOf<PersonEnkel, List<MånedPeriode>>()
        barnasAndeler.forEach { (barn, andeler) ->
            val barnsAndelerFraAndreBehandlinger =
                barnMedAndreRelevanteTilkjentYtelser
                    .filter { it.first.aktør == barn.aktør }
                    .flatMap { it.second }
                    .flatMap { it.andelerTilkjentYtelse }
                    .filter { it.aktør == barn.aktør }

            val perioderMedOverlapp =
                finnPeriodeMedOverlappAvAndeler(
                    andeler = andeler,
                    barnsAndelerFraAndreBehandlinger = barnsAndelerFraAndreBehandlinger,
                )
            if (perioderMedOverlapp.isNotEmpty()) {
                barnMedUtbetalingsikkerhetFeil.put(barn, perioderMedOverlapp)
            }
        }
        if (barnMedUtbetalingsikkerhetFeil.isNotEmpty()) {
            val sammenlignedeBehandlingerMedDeltBosted =
                barnMedAndreRelevanteTilkjentYtelser
                    .flatMap { it.second }
                    .filter { it.andelerTilkjentYtelse.any { andel -> andel.erDeltBosted() } }
                    .map { it.behandling.id }
                    .distinct()

            throw UtbetalingsikkerhetFeil(
                melding = "Vi finner utbetalinger som overstiger 100% på hvert av barna: ${
                    barnMedUtbetalingsikkerhetFeil.tilFeilmeldingTekst()
                }. ${if (sammenlignedeBehandlingerMedDeltBosted.isNotEmpty()) "Sammenligning gjort med behandling: ${sammenlignedeBehandlingerMedDeltBosted.joinToString(",") { it.toString() }} som omhandler delt bosted. Mulig det finnes behandlinger som ligger til godkjenning som vil korrigere feilen." else ""}",
                frontendFeilmelding = "Du kan ikke godkjenne dette vedtaket fordi det vil betales ut mer enn 100% for barn født ${
                    barnMedUtbetalingsikkerhetFeil.tilFeilmeldingTekst()
                }. Reduksjonsvedtak til annen person må være sendt til godkjenning før du kan gå videre.",
            )
        }
    }

    fun Map<PersonEnkel, List<MånedPeriode>>.tilFeilmeldingTekst() = this.map { "${it.key.fødselsdato.tilKortString()} i perioden ${it.value.joinToString(", ") { "${it.fom} til ${it.tom}" }}" }.slåSammen()

    fun maksBeløp(
        personType: PersonType,
        fagsakType: FagsakType,
    ): Int {
        val satser = SatsService.hentAllesatser()
        val småbarnsTillegg = satser.filter { it.type == SatsType.SMA }
        val ordinærMedTillegg = satser.filter { it.type == SatsType.TILLEGG_ORBA }
        val ordinær = satser.filter { it.type == SatsType.ORBA }
        val utvidet = satser.filter { it.type == SatsType.UTVIDET_BARNETRYGD }
        val finnmarkstillegg = satser.filter { it.type == SatsType.FINNMARKSTILLEGG }

        if (småbarnsTillegg.isEmpty() || ordinærMedTillegg.isEmpty() || utvidet.isEmpty()) throw Feil("Fant ikke satser ved validering")

        val maksSmåbarnstillegg = småbarnsTillegg.maxBy { it.beløp }.beløp
        val maksOrdinærMedTillegg = ordinærMedTillegg.maxBy { it.beløp }.beløp
        val maksOrdinær = ordinær.maxBy { it.beløp }.beløp
        val maksUtvidet = utvidet.maxBy { it.beløp }.beløp
        val maksFinnmarkstillegg = finnmarkstillegg.maxByOrNull { it.beløp }?.beløp ?: 0

        return if (fagsakType == FagsakType.BARN_ENSLIG_MINDREÅRIG) {
            maxOf(maksOrdinær, maksOrdinærMedTillegg) + maksUtvidet + maksFinnmarkstillegg
        } else {
            when (personType) {
                PersonType.BARN -> maxOf(maksOrdinær, maksOrdinærMedTillegg) + maksFinnmarkstillegg
                PersonType.SØKER -> maksUtvidet + maksSmåbarnstillegg
                else -> throw Feil("Ikke støtte for å utbetale til persontype ${personType.name}")
            }
        }
    }

    fun finnPeriodeMedOverlappAvAndeler(
        andeler: List<AndelTilkjentYtelse>,
        barnsAndelerFraAndreBehandlinger: List<AndelTilkjentYtelse>,
    ): List<MånedPeriode> {
        val kombinertOverlappTidslinje =
            YtelseType
                .entries
                .map { ytelseType ->
                    lagErOver100ProsentUtbetalingPåYtelseTidslinje(
                        andeler = andeler.filter { it.type == ytelseType },
                        barnsAndelerFraAndreBehandlinger = barnsAndelerFraAndreBehandlinger.filter { it.type == ytelseType },
                    )
                }.kombiner { it.minstEnYtelseMedBehandlingIdHarOverlapp() }

        return kombinertOverlappTidslinje
            .tilPerioder()
            .filter { it.verdi == true }
            .map { MånedPeriode(it.fom?.toYearMonth() ?: MIN_MÅNED, it.tom?.toYearMonth() ?: MAX_MÅNED) }
    }

    internal fun Iterable<Boolean>.minstEnYtelseHarOverlapp(): Boolean = any { it }

    internal fun Iterable<ErOver100ProsentMedBehandlingId>.minstEnYtelseMedBehandlingIdHarOverlapp(): Boolean = any { it.erOver100Prosent }

    data class ErOver100ProsentMedBehandlingId(
        val erOver100Prosent: Boolean,
        val behandlingIds: List<Long> = emptyList(),
    )

    fun lagErOver100ProsentUtbetalingPåYtelseTidslinje(
        andeler: List<AndelTilkjentYtelse>,
        barnsAndelerFraAndreBehandlinger: List<AndelTilkjentYtelse>,
    ): Tidslinje<ErOver100ProsentMedBehandlingId> {
        if (barnsAndelerFraAndreBehandlinger.isEmpty()) {
            return tomTidslinje()
        }

        val andelerPerBehandling = (andeler + barnsAndelerFraAndreBehandlinger).groupBy { it.behandlingId }

        val prosenttidslinjerPerBehandling =
            andelerPerBehandling.mapValues { (_, andelerForBehandling) ->
                andelerForBehandling.tilProsentAvYtelseUtbetaltTidslinje()
            }

        val totalProsentTidslinje =
            prosenttidslinjerPerBehandling.values
                .fold(tomTidslinje<BigDecimal>()) { summertProsentTidslinje, prosentTidslinje ->
                    summertProsentTidslinje.kombinerMed(prosentTidslinje) { sumProsentForPeriode, prosentForAndel ->
                        (sumProsentForPeriode ?: BigDecimal.ZERO) + (prosentForAndel ?: BigDecimal.ZERO)
                    }
                }
        val erOver100ProsentTidslinje =
            totalProsentTidslinje.mapVerdi { sumProsentForPeriode ->
                val erOver100Prosent = (sumProsentForPeriode ?: BigDecimal.ZERO) > BigDecimal.valueOf(100)

                ErOver100ProsentMedBehandlingId(erOver100Prosent = erOver100Prosent, behandlingIds = andelerPerBehandling.keys.filterNotNull())
            }

        return erOver100ProsentTidslinje
    }
}

private fun List<AndelTilkjentYtelse>.tilProsentAvYtelseUtbetaltTidslinje() =
    this
        .map {
            Periode(
                verdi = it.prosent,
                fom = it.periode.fom.førsteDagIInneværendeMåned(),
                tom = it.periode.tom.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()

private fun validerAtBeløpForPartStemmerMedSatser(
    person: PersonEnkel,
    andeler: List<AndelTilkjentYtelse>,
    fagsakType: FagsakType,
) {
    val antallOrdinær = andeler.count { it.type == YtelseType.ORDINÆR_BARNETRYGD }
    val antallFinnmarkstillegg = andeler.count { it.type == YtelseType.FINNMARKSTILLEGG }
    val antallSvalbardtillegg = andeler.count { it.type == YtelseType.SVALBARDTILLEGG }
    val antallUtvidet = andeler.count { it.type == YtelseType.UTVIDET_BARNETRYGD }

    if (antallSvalbardtillegg > 0 && antallFinnmarkstillegg > 0) {
        throw UtbetalingsikkerhetFeil(
            melding = "Validering feilet for person med fødselsdato ${person.fødselsdato} - Barnet kan ikke ha både finnmarkstillegg og svalbardtillegg i samme periode.",
            frontendFeilmelding = "Det har skjedd en systemfeil, og andelene stemmer ikke overens med det som er lov. $KONTAKT_TEAMET_SUFFIX",
        )
    }

    val maksAntallAndeler =
        when {
            fagsakType == FagsakType.BARN_ENSLIG_MINDREÅRIG -> {
                if (antallOrdinær > 1 || antallFinnmarkstillegg > 1 || antallUtvidet > 1 || antallSvalbardtillegg > 1) {
                    throw UtbetalingsikkerhetFeil(
                        melding = "Validering feilet for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}): Barnet kan ha maks én ordinær, en utvidet, en finnmarkstillegg/svalbardtillegg andel for en gitt periode.",
                        frontendFeilmelding = "Det har skjedd en systemfeil, og andelene stemmer ikke overens med det som er lov. $KONTAKT_TEAMET_SUFFIX",
                    )
                }
                3
            }

            person.type == PersonType.BARN -> {
                if (antallOrdinær > 1 || antallFinnmarkstillegg > 1 || antallSvalbardtillegg > 1) {
                    throw UtbetalingsikkerhetFeil(
                        melding = "Validering feilet for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}): Barn kan ha maks én ordinær og én finnmarkstillegg andel for en gitt periode.",
                        frontendFeilmelding = "Det har skjedd en systemfeil, og andelene stemmer ikke overens med det som er lov. $KONTAKT_TEAMET_SUFFIX",
                    )
                }
                2
            }

            else -> {
                2
            }
        }

    val maksTotalBeløp = maksBeløp(personType = person.type, fagsakType = fagsakType)

    if (andeler.size > maksAntallAndeler) {
        throw UtbetalingsikkerhetFeil(
            melding = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatte andeler = $maksAntallAndeler, faktiske andeler = ${andeler.size}.",
            frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX",
        )
    }

    val totalbeløp =
        andeler
            .map { it.kalkulertUtbetalingsbeløp }
            .fold(0) { sum, beløp -> sum + beløp }
    if (totalbeløp > maksTotalBeløp) {
        throw UtbetalingsikkerhetFeil(
            melding = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatt totalbeløp = $maksTotalBeløp, faktiske totalbeløp = $totalbeløp.",
            frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX",
        )
    }
}
