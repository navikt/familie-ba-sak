package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.maksBeløp
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjeMedAndeler
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
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

    fun validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(
        forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>?,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        opprettetTidspunkt: LocalDateTime,
    ) {
        val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(opprettetTidspunkt)

        val andelerPåPersoner: Map<String, Pair<List<AndelTilkjentYtelse>, List<AndelTilkjentYtelse>?>> =
            hentAndelerPerPerson(andelerTilkjentYtelse, forrigeAndelerTilkjentYtelse)

        val erUgyldigEtterbetaling =
            andelerPåPersoner.any { (_, andelPåPerson: Pair<List<AndelTilkjentYtelse>, List<AndelTilkjentYtelse>?>) ->
                val (andelerTilkjentYtelseForPerson, forrigeAndelerTilkjentYtelseForPerson) = andelPåPerson

                erUgyldigEtterbetalingPåPerson(
                    forrigeAndelerTilkjentYtelseForPerson,
                    andelerTilkjentYtelseForPerson,
                    gyldigEtterbetalingFom
                )
            }

        if (erUgyldigEtterbetaling) {
            throw UtbetalingsikkerhetFeil(
                melding = "Endring i utbetalingsperioder for en eller flere av partene/personene " +
                    "går mer enn 3 år tilbake i tid.",
                frontendFeilmelding =
                "Endring i utbetalingsperioder for en eller flere av partene/personene " +
                    "går mer enn 3 år tilbake i tid. Vennligst endre på datoene, eller ta kontakt med teamet for hjelp."
            )
        }
    }

    @Deprecated("Bruk validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode har gåt gjennom QA")
    fun validerAtTilkjentYtelseHarGyldigEtterbetalingsperiodeGammel(tilkjentYtelse: TilkjentYtelse) {
        val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(tilkjentYtelse.behandling.opprettetTidspunkt)
        if (tilkjentYtelse.andelerTilkjentYtelse.any { it.stønadFom < gyldigEtterbetalingFom }) {
            throw UtbetalingsikkerhetFeil(
                melding = "Utbetalingsperioder for en eller flere av partene/personene går mer enn 3 år tilbake i tid.",
                frontendFeilmelding = "Utbetalingsperioder for en eller flere av partene/personene går mer enn 3 år tilbake i tid. Vennligst endre på datoene, eller ta kontakt med teamet for hjelp."
            )
        }
    }

    private fun hentAndelerPerPerson(
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>?
    ): Map<String, Pair<List<AndelTilkjentYtelse>, List<AndelTilkjentYtelse>?>> {
        val aktørIderFraAndeler = andelerTilkjentYtelse.map { it.aktør.aktørId }
        val aktøerIderFraForrigeAndeler = forrigeAndelerTilkjentYtelse?.map { it.aktør.aktørId } ?: emptyList()
        val aktørIder = (aktørIderFraAndeler + aktøerIderFraForrigeAndeler).toSet()

        val andelerPåPersoner: Map<String, Pair<List<AndelTilkjentYtelse>, List<AndelTilkjentYtelse>?>> =
            aktørIder.associateWith { aktørId ->
                Pair(
                    andelerTilkjentYtelse.filter { it.aktør.aktørId == aktørId },
                    forrigeAndelerTilkjentYtelse?.filter { it.aktør.aktørId == aktørId },
                )
            }
        return andelerPåPersoner
    }

    private fun erUgyldigEtterbetalingPåPerson(
        forrigeAndelerTilkjentYtelseForPerson: List<AndelTilkjentYtelse>?,
        andelerTilkjentYtelseForPerson: List<AndelTilkjentYtelse>,
        gyldigEtterbetalingFom: YearMonth?
    ): Boolean {
        val forrigeAndelerTidslinje =
            hentTidslinjeForAndelerTilkjentYtelse(forrigeAndelerTilkjentYtelseForPerson?.toList())
        val andelerTidslinje = hentTidslinjeForAndelerTilkjentYtelse(andelerTilkjentYtelseForPerson.toList())

        val erAndelTilkjentYtelseMedØktBeløpMerEnn3ÅrTilbake =
            erAndelTilkjentYtelseMedØktBeløpMerEnn3ÅrTilbake(
                andelerTidslinje = andelerTidslinje,
                forrigeAndelerTidslinje = forrigeAndelerTidslinje,
                gyldigEtterbetalingFom = gyldigEtterbetalingFom
            )

        val erLagtTilSegmentMerEnn3ÅrTilbake =
            erLagtTilSegmentMerEnn3ÅrTilbake(
                andelerTidslinje,
                forrigeAndelerTidslinje,
                gyldigEtterbetalingFom
            )

        return erAndelTilkjentYtelseMedØktBeløpMerEnn3ÅrTilbake || erLagtTilSegmentMerEnn3ÅrTilbake
    }

    private fun erLagtTilSegmentMerEnn3ÅrTilbake(
        andelerTidslinje: LocalDateTimeline<AndelTilkjentYtelse>,
        forrigeAndelerTidslinje: LocalDateTimeline<AndelTilkjentYtelse>,
        gyldigEtterbetalingFom: YearMonth?
    ): Boolean {
        val segmenterLagtTil = andelerTidslinje.disjoint(forrigeAndelerTidslinje)
        return (segmenterLagtTil).any { it.value.stønadFom < gyldigEtterbetalingFom }
    }

    private fun erAndelTilkjentYtelseMedØktBeløpMerEnn3ÅrTilbake(
        andelerTidslinje: LocalDateTimeline<AndelTilkjentYtelse>,
        forrigeAndelerTidslinje: LocalDateTimeline<AndelTilkjentYtelse>,
        gyldigEtterbetalingFom: YearMonth?
    ): Boolean {
        val overlappendeAndeler = andelerTidslinje.intersection(forrigeAndelerTidslinje)

        return overlappendeAndeler
            .filter { it.value.stønadFom < gyldigEtterbetalingFom }
            .any { andelTilkjentYtelse ->
                val tidligereAndelTilkjentYtelse = forrigeAndelerTidslinje.find { it.fom == andelTilkjentYtelse.fom }!!

                tidligereAndelTilkjentYtelse
                    .value.kalkulertUtbetalingsbeløp < andelTilkjentYtelse.value.kalkulertUtbetalingsbeløp
            }
    }

    private fun hentTidslinjeForAndelerTilkjentYtelse(andelerTilkjentYtelse: List<AndelTilkjentYtelse>?) =
        LocalDateTimeline(
            andelerTilkjentYtelse?.map {
                LocalDateSegment(
                    it.stønadFom.førsteDagIInneværendeMåned(),
                    it.stønadTom.sisteDagIInneværendeMåned(),
                    it
                )
            }
        )

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
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ) {
        val barna = personopplysningGrunnlag.barna.sortedBy { it.fødselsdato }

        val barnasAndeler = hentBarnasAndeler(behandlendeBehandlingTilkjentYtelse.andelerTilkjentYtelse.toList(), barna)

        barnasAndeler.forEach { (barn, andeler) ->
            val barnsAndelerFraAndreBehandlinger =
                barnMedAndreRelevanteTilkjentYtelser.filter { it.first.aktør == barn.aktør }
                    .flatMap { it.second }
                    .flatMap { it.andelerTilkjentYtelse }
                    .filter { it.aktør == barn.aktør }

            validerIngenOverlappAvAndeler(
                andeler,
                barnsAndelerFraAndreBehandlinger,
                behandlendeBehandlingTilkjentYtelse,
                barn
            )
        }
    }

    private fun validerIngenOverlappAvAndeler(
        andeler: List<AndelTilkjentYtelse>,
        barnsAndelerFraAndreBehandlinger: List<AndelTilkjentYtelse>,
        behandlendeBehandlingTilkjentYtelse: TilkjentYtelse,
        barn: Person
    ) {
        andeler.forEach { andelTilkjentYtelse ->
            if (barnsAndelerFraAndreBehandlinger.any
                {
                    andelTilkjentYtelse.overlapperMed(it) &&
                        andelTilkjentYtelse.prosent + it.prosent > BigDecimal(100)
                }
            ) {
                throw UtbetalingsikkerhetFeil(
                    melding = "Vi finner flere utbetalinger for barn på behandling ${behandlendeBehandlingTilkjentYtelse.behandling.id}",
                    frontendFeilmelding = "Det er allerede innvilget utbetaling av barnetrygd for ${barn.aktør.aktivFødselsnummer()} i perioden ${andelTilkjentYtelse.stønadFom.tilKortString()} - ${andelTilkjentYtelse.stønadTom.tilKortString()}."
                )
            }
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
