package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.maksBeløp
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjeMedAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerPersonOgType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringUtil.tilFørsteEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilBrevTekst
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

// 3 år (krav i loven)
fun hentGyldigEtterbetalingFom(kravDato: LocalDateTime) =
    kravDato.minusYears(3)
        .toLocalDate()
        .toYearMonth()

fun hentSøkersAndeler(
    andeler: List<AndelTilkjentYtelse>,
    søker: Person,
) = andeler.filter { it.aktør == søker.aktør }

fun hentBarnasAndeler(andeler: List<AndelTilkjentYtelse>, barna: List<Person>) = barna.map { barn ->
    barn to andeler.filter { it.aktør == barn.aktør }
}

/**
 * Ekstra sikkerhet rundt hva som utbetales som på sikt vil legges inn i
 * de respektive stegene SB håndterer slik at det er lettere for SB å rette feilene.
 */
object TilkjentYtelseValidering {

    internal fun validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
        andelerFraForrigeBehandling: List<AndelTilkjentYtelse>,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    ) {
        val andelerGruppert = andelerTilkjentYtelse.tilTidslinjerPerPersonOgType()
        val forrigeAndelerGruppert = andelerFraForrigeBehandling.tilTidslinjerPerPersonOgType()

        andelerGruppert.outerJoin(forrigeAndelerGruppert) { nåværendeAndel, forrigeAndel ->
            when {
                forrigeAndel == null && nåværendeAndel != null ->
                    throw Feil("Satsendring kan ikke legge til en andel som ikke var der i forrige behandling")
                forrigeAndel != null && nåværendeAndel == null ->
                    throw Feil("Satsendring kan ikke fjerne en andel som fantes i forrige behandling")
                forrigeAndel != null && forrigeAndel.prosent != nåværendeAndel?.prosent ->
                    throw Feil("Satsendring kan ikke endre på prosenten til en andel")
                else -> false
            }
        }.values.map { it.perioder() } // Må kalle på .perioder() for at feilene over skal bli kastet
    }

    fun finnAktørIderMedUgyldigEtterbetalingsperiode(
        forrigeAndelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        kravDato: LocalDateTime,
    ): List<Aktør> {
        val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(kravDato)

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
    ): Boolean {
        return YtelseType.values().any { ytelseType ->
            val forrigeAndelerForPersonOgType = forrigeAndelerForPerson.filter { it.type == ytelseType }
            val andelerForPersonOgType = andelerForPerson.filter { it.type == ytelseType }

            val etterbetalingTidslinje = EndringIUtbetalingUtil.lagEtterbetalingstidslinjeForPersonOgType(
                nåværendeAndeler = andelerForPersonOgType,
                forrigeAndeler = forrigeAndelerForPersonOgType,
            )

            val førsteMånedMedEtterbetaling = etterbetalingTidslinje.tilFørsteEndringstidspunkt()

            førsteMånedMedEtterbetaling != null && førsteMånedMedEtterbetaling < gyldigEtterbetalingFom
        }
    }

    fun validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
        tilkjentYtelse: TilkjentYtelse,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ) {
        val søker = personopplysningGrunnlag.søker
        val barna = personopplysningGrunnlag.barna

        val tidslinjeMedAndeler = tilkjentYtelse.tilTidslinjeMedAndeler()

        val fagsakType = tilkjentYtelse.behandling.fagsak.type

        tidslinjeMedAndeler.toSegments().forEach {
            val søkersAndeler = hentSøkersAndeler(it.value, søker)
            val barnasAndeler = hentBarnasAndeler(it.value, barna)

            validerAtBeløpForPartStemmerMedSatser(person = søker, andeler = søkersAndeler, fagsakType = fagsakType)

            barnasAndeler.forEach { (person, andeler) ->
                validerAtBeløpForPartStemmerMedSatser(person = person, andeler = andeler, fagsakType = fagsakType)
            }
        }
    }

    fun validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
        behandlendeBehandlingTilkjentYtelse: TilkjentYtelse,
        barnMedAndreRelevanteTilkjentYtelser: List<Pair<Person, List<TilkjentYtelse>>>,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ) {
        val barna = personopplysningGrunnlag.barna.sortedBy { it.fødselsdato }

        val barnasAndeler = hentBarnasAndeler(behandlendeBehandlingTilkjentYtelse.andelerTilkjentYtelse.toList(), barna)

        val barnMedUtbetalingsikkerhetFeil = mutableListOf<Person>()
        barnasAndeler.forEach { (barn, andeler) ->
            val barnsAndelerFraAndreBehandlinger =
                barnMedAndreRelevanteTilkjentYtelser.filter { it.first.aktør == barn.aktør }
                    .flatMap { it.second }
                    .flatMap { it.andelerTilkjentYtelse }
                    .filter { it.aktør == barn.aktør }

            if (erOverlappAvAndeler(
                    andeler = andeler,
                    barnsAndelerFraAndreBehandlinger = barnsAndelerFraAndreBehandlinger,
                )
            ) {
                barnMedUtbetalingsikkerhetFeil.add(barn)
            }
        }
        if (barnMedUtbetalingsikkerhetFeil.isNotEmpty()) {
            throw UtbetalingsikkerhetFeil(
                melding = "Vi finner utbetalinger som overstiger 100% på hvert av barna: ${
                    barnMedUtbetalingsikkerhetFeil.map { it.fødselsdato }.tilBrevTekst()
                }",
                frontendFeilmelding = "Du kan ikke godkjenne dette vedtaket fordi det vil betales ut mer enn 100% for barn født ${
                    barnMedUtbetalingsikkerhetFeil.map { it.fødselsdato }.tilBrevTekst()
                }. Reduksjonsvedtak til annen person må være sendt til godkjenning før du kan gå videre.",
            )
        }
    }

    fun maksBeløp(personType: PersonType, fagsakType: FagsakType): Int {
        val satser = SatsService.hentAllesatser()
        val småbarnsTillegg = satser.filter { it.type == SatsType.SMA }
        val ordinærMedTillegg = satser.filter { it.type == SatsType.TILLEGG_ORBA }
        val utvidet = satser.filter { it.type == SatsType.UTVIDET_BARNETRYGD }
        if (småbarnsTillegg.isEmpty() || ordinærMedTillegg.isEmpty() || utvidet.isEmpty()) error("Fant ikke satser ved validering")
        val maksSmåbarnstillegg = småbarnsTillegg.maxByOrNull { it.beløp }!!.beløp
        val maksOrdinærMedTillegg = ordinærMedTillegg.maxByOrNull { it.beløp }!!.beløp
        val maksUtvidet = utvidet.maxBy { it.beløp }.beløp

        return if (fagsakType == FagsakType.BARN_ENSLIG_MINDREÅRIG) {
            maksOrdinærMedTillegg + maksUtvidet
        } else {
            when (personType) {
                PersonType.BARN -> maksOrdinærMedTillegg
                PersonType.SØKER -> maksUtvidet + maksSmåbarnstillegg
                else -> throw Feil("Ikke støtte for å utbetale til persontype ${personType.name}")
            }
        }
    }

    private fun erOverlappAvAndeler(
        andeler: List<AndelTilkjentYtelse>,
        barnsAndelerFraAndreBehandlinger: List<AndelTilkjentYtelse>,
    ): Boolean {
        return andeler.any { andelTilkjentYtelse ->
            barnsAndelerFraAndreBehandlinger.any {
                andelTilkjentYtelse.overlapperMed(it) &&
                    andelTilkjentYtelse.prosent + it.prosent > BigDecimal(100)
            }
        }
    }
}

private fun validerAtBeløpForPartStemmerMedSatser(
    person: Person,
    andeler: List<AndelTilkjentYtelse>,
    fagsakType: FagsakType,
) {
    val maksAntallAndeler =
        if (fagsakType == FagsakType.BARN_ENSLIG_MINDREÅRIG) 2 else if (person.type == PersonType.BARN) 1 else 2
    val maksTotalBeløp = maksBeløp(personType = person.type, fagsakType = fagsakType)

    if (andeler.size > maksAntallAndeler) {
        throw UtbetalingsikkerhetFeil(
            melding = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatte andeler = $maksAntallAndeler, faktiske andeler = ${andeler.size}.",
            frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX",
        )
    }

    val totalbeløp = andeler.map { it.kalkulertUtbetalingsbeløp }
        .fold(0) { sum, beløp -> sum + beløp }
    if (totalbeløp > maksTotalBeløp) {
        throw UtbetalingsikkerhetFeil(
            melding = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatt totalbeløp = $maksTotalBeløp, faktiske totalbeløp = $totalbeløp.",
            frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX",
        )
    }
}
