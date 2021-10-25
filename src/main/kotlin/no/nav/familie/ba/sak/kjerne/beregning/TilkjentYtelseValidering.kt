package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
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
import java.math.BigDecimal
import java.time.LocalDateTime

// 3 år (krav i loven) og 2 måneder (på grunn av behandlingstid)
fun hentGyldigEtterbetalingFom(kravDato: LocalDateTime) =
    kravDato.minusYears(3)
        .minusMonths(2)
        .toLocalDate()
        .toYearMonth()

fun hentSøkersAndeler(
    andeler: List<AndelTilkjentYtelse>,
    søker: Person
) = andeler.filter { it.personIdent == søker.personIdent.ident }

fun hentBarnasAndeler(andeler: List<AndelTilkjentYtelse>, barna: List<Person>) = barna.map { barn ->
    barn to andeler.filter { it.personIdent == barn.personIdent.ident }
}

/**
 * Ekstra sikkerhet rundt hva som utbetales som på sikt vil legges inn i
 * de respektive stegene SB håndterer slik at det er lettere for SB å rette feilene.
 */
object TilkjentYtelseValidering {

    fun validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(tilkjentYtelse: TilkjentYtelse) {
        val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(tilkjentYtelse.behandling.opprettetTidspunkt)
        if (tilkjentYtelse.andelerTilkjentYtelse.any { it.stønadFom < gyldigEtterbetalingFom }) {
            throw UtbetalingsikkerhetFeil(
                melding = "Utbetalingsperioder for en eller flere av partene/personene går mer enn 3 år tilbake i tid.",
                frontendFeilmelding = "Utbetalingsperioder for en eller flere av partene/personene går mer enn 3 år tilbake i tid. Vennligst endre på datoene, eller ta kontakt med teamet for hjelp."
            )
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
        barnMedAndreTilkjentYtelse: List<Pair<Person, List<TilkjentYtelse>>>,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ) {
        val barna = personopplysningGrunnlag.barna.sortedBy { it.fødselsdato }

        val barnasAndeler = hentBarnasAndeler(behandlendeBehandlingTilkjentYtelse.andelerTilkjentYtelse.toList(), barna)

        barnasAndeler.forEach { (barn, andeler) ->
            val barnsAndelerFraAndreBehandlinger =
                barnMedAndreTilkjentYtelse.filter { it.first.personIdent.ident == barn.personIdent.ident }
                    .flatMap { it.second }
                    .flatMap { it.andelerTilkjentYtelse }

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
                    frontendFeilmelding = "Det utbetales allerede barnetrygd for ${barn.personIdent.ident} i perioden ${andelTilkjentYtelse.stønadFom.tilKortString()} - ${andelTilkjentYtelse.stønadTom.tilKortString()}."
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
            frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. Kontakt teamet for hjelp"
        )
    }

    val totalbeløp = andeler.map { it.kalkulertUtbetalingsbeløp }
        .fold(0) { sum, beløp -> sum + beløp }
    if (totalbeløp > maksTotalBeløp) {
        throw UtbetalingsikkerhetFeil(
            melding = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatt totalbeløp = $maksTotalBeløp, faktiske totalbeløp = $totalbeløp.",
            frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. Kontakt teamet for hjelp"
        )
    }
}
