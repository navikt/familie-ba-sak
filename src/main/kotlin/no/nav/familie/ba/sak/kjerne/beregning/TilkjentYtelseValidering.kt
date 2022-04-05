package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.maksBeløp
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.hentTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjeMedAndeler
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
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
    søker: Person
) = andeler.filter { it.aktør == søker.aktør }

fun hentBarnasAndeler(andeler: List<AndelTilkjentYtelse>, barna: List<Person>) = barna.map { barn ->
    barn to andeler.filter { it.aktør == barn.aktør }
}

/**
 * Ekstra sikkerhet rundt hva som utbetales som på sikt vil legges inn i
 * de respektive stegene SB håndterer slik at det er lettere for SB å rette feilene.
 */
object TilkjentYtelseValidering {

    fun finnAktørIderMedUgyldigEtterbetalingsperiode(
        forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>?,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        kravDato: LocalDateTime,
    ): List<String> {
        val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(kravDato)

        val aktørIder =
            hentAktørIderForDenneOgForrigeBehandling(andelerTilkjentYtelse, forrigeAndelerTilkjentYtelse)

        val personerMedUgyldigEtterbetaling =
            aktørIder.mapNotNull { aktørId ->
                val andelerTilkjentYtelseForPerson = andelerTilkjentYtelse.filter { it.aktør.aktørId == aktørId }
                val forrigeAndelerTilkjentYtelseForPerson =
                    forrigeAndelerTilkjentYtelse?.filter { it.aktør.aktørId == aktørId }

                val etterbetalingErUgyldig = erUgyldigEtterbetalingPåPerson(
                    forrigeAndelerTilkjentYtelseForPerson,
                    andelerTilkjentYtelseForPerson,
                    gyldigEtterbetalingFom
                )

                if (etterbetalingErUgyldig) {
                    aktørId
                } else null
            }

        return personerMedUgyldigEtterbetaling
    }

    fun hentAktørIderForDenneOgForrigeBehandling(
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>?
    ): Set<String> {

        val aktørIderFraAndeler = andelerTilkjentYtelse.map { it.aktør.aktørId }
        val aktøerIderFraForrigeAndeler = forrigeAndelerTilkjentYtelse?.map { it.aktør.aktørId } ?: emptyList()
        return (aktørIderFraAndeler + aktøerIderFraForrigeAndeler).toSet()
    }

    fun erUgyldigEtterbetalingPåPerson(
        forrigeAndelerForPerson: List<AndelTilkjentYtelse>?,
        andelerForPerson: List<AndelTilkjentYtelse>,
        gyldigEtterbetalingFom: YearMonth?
    ): Boolean {
        return YtelseType.values().any { ytelseType ->
            val forrigeAndelerForPersonOgType = forrigeAndelerForPerson?.filter { it.type == ytelseType }
            val andelerForPersonOgType = andelerForPerson.filter { it.type == ytelseType }

            val forrigeAndelerTidslinje = forrigeAndelerForPersonOgType?.toList().hentTidslinje()
            val andelerTidslinje = andelerForPersonOgType.toList().hentTidslinje()

            val erAndelMedØktBeløpFørGyldigEtterbetalingsdato =
                erAndelMedØktBeløpFørDato(
                    forrigeAndeler = forrigeAndelerForPersonOgType,
                    andeler = andelerForPersonOgType,
                    måned = gyldigEtterbetalingFom
                )

            val segmenterLagtTil = andelerTidslinje.disjoint(forrigeAndelerTidslinje)
            val erLagtTilSegmentFørGyldigEtterbetalingsdato =
                segmenterLagtTil.any { it.value.stønadFom < gyldigEtterbetalingFom }

            erAndelMedØktBeløpFørGyldigEtterbetalingsdato || erLagtTilSegmentFørGyldigEtterbetalingsdato
        }
    }

    fun validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
        tilkjentYtelse: TilkjentYtelse,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ) {
        val søker = personopplysningGrunnlag.søker
        val barna = personopplysningGrunnlag.barna

        val tidslinjeMedAndeler = tilkjentYtelse.tilTidslinjeMedAndeler()

        tidslinjeMedAndeler.toSegments().forEach {
            val søkersAndeler = hentSøkersAndeler(it.value, søker)
            val barnasAndeler = hentBarnasAndeler(it.value, barna)

            validerAtBeløpForPartStemmerMedSatser(søker, søkersAndeler)

            barnasAndeler.forEach { (person, andeler) ->
                validerAtBeløpForPartStemmerMedSatser(person, andeler)
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
                    barnsAndelerFraAndreBehandlinger = barnsAndelerFraAndreBehandlinger
                )
            ) {
                barnMedUtbetalingsikkerhetFeil.add(barn)
            }
        }
        if (barnMedUtbetalingsikkerhetFeil.isNotEmpty()) {
            throw UtbetalingsikkerhetFeil(
                melding = "Vi finner utbetalinger som overstiger 100% på hvert av barna: ${barnMedUtbetalingsikkerhetFeil.map { it.fødselsdato }.tilBrevTekst()}",
                frontendFeilmelding = "Du kan ikke godkjenne dette vedtaket fordi det vil betales ut mer enn 100% for barn født ${barnMedUtbetalingsikkerhetFeil.map { it.fødselsdato }.tilBrevTekst()}. Reduksjonsvedtak til annen person må være sendt til godkjenning før du kan gå videre."
            )
        }
    }

    fun maksBeløp(personType: PersonType): Int {
        val satser = SatsService.hentAllesatser()
        val småbarnsTillegg = satser.filter { it.type == SatsType.SMA }
        val ordinærMedTillegg = satser.filter { it.type == SatsType.TILLEGG_ORBA }
        val ordinær = satser.filter { it.type == SatsType.ORBA }
        if (småbarnsTillegg.isEmpty() || ordinærMedTillegg.isEmpty() || ordinær.isEmpty()) error("Fant ikke satser ved validering")
        val maksSmåbarnstillegg = småbarnsTillegg.maxByOrNull { it.beløp }!!.beløp
        val maksOrdinærMedTillegg = ordinærMedTillegg.maxByOrNull { it.beløp }!!.beløp
        val maksOrdinær = ordinær.maxByOrNull { it.beløp }!!.beløp
        return when (personType) {
            PersonType.BARN -> maksOrdinærMedTillegg
            PersonType.SØKER -> maksOrdinær + maksSmåbarnstillegg
            else -> throw Feil("Ikke støtte for å utbetale til persontype ${personType.name}")
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

    private fun erAndelMedØktBeløpFørDato(
        forrigeAndeler: List<AndelTilkjentYtelse>?,
        andeler: List<AndelTilkjentYtelse>,
        måned: YearMonth?
    ): Boolean = andeler
        .filter { it.stønadFom < måned }
        .any { andel ->
            forrigeAndeler?.any {
                it.periode.overlapperHeltEllerDelvisMed(andel.periode) &&
                    it.kalkulertUtbetalingsbeløp < andel.kalkulertUtbetalingsbeløp
            } ?: false
        }
}

private fun validerAtBeløpForPartStemmerMedSatser(
    person: Person,
    andeler: List<AndelTilkjentYtelse>,
    maksAntallAndeler: Int = if (person.type == PersonType.BARN) 1 else 2,
    maksTotalBeløp: Int = maksBeløp(person.type)
) {
    if (andeler.size > maksAntallAndeler) {
        throw UtbetalingsikkerhetFeil(
            melding = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatte andeler = $maksAntallAndeler, faktiske andeler = ${andeler.size}.",
            frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX"
        )
    }

    val totalbeløp = andeler.map { it.kalkulertUtbetalingsbeløp }
        .fold(0) { sum, beløp -> sum + beløp }
    if (totalbeløp > maksTotalBeløp) {
        throw UtbetalingsikkerhetFeil(
            melding = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatt totalbeløp = $maksTotalBeløp, faktiske totalbeløp = $totalbeløp.",
            frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX"
        )
    }
}
