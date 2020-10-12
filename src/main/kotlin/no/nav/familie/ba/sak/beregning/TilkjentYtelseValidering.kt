package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.tilTidslinjeMedAndeler
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import java.time.LocalDateTime

// 3 år (krav i loven) og 2 måneder (på grunn av behandlingstid)
fun hentGyldigEtterbetalingFom(kravDato: LocalDateTime) =
        kravDato.minusYears(3)
                .minusMonths(2)
                .toLocalDate()
                .førsteDagIInneværendeMåned()

fun hentSøkersAndeler(andeler: List<AndelTilkjentYtelse>,
                      søker: Person) = andeler.filter { it.personIdent == søker.personIdent.ident }

fun hentBarnasAndeler(andeler: List<AndelTilkjentYtelse>, barna: List<Person>) = barna.map { barn ->
    barn to andeler.filter { it.personIdent == barn.personIdent.ident }
}

object TilkjentYtelseValidering {

    fun validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(tilkjentYtelse: TilkjentYtelse) {
        val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(tilkjentYtelse.behandling.opprettetTidspunkt)
        if (tilkjentYtelse.andelerTilkjentYtelse.any {
                    it.stønadFom < gyldigEtterbetalingFom
                }) {
            throw Feil(message = "Utbetalingsperioder for en eller flere av partene/personene går mer enn 3 år tilbake i tid. Gå tilbake til vilkårsvurderingen og endre på datoene, , eller ta kontakt med teamet for hjelp.",
                       frontendFeilmelding = "Utbetalingsperioder for en eller flere av partene/personene går mer enn 3 år tilbake i tid. Gå tilbake til vilkårsvurderingen og endre på datoene, , eller ta kontakt med teamet for hjelp.")
        }
    }

    fun validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse: TilkjentYtelse,
                                                            personopplysningGrunnlag: PersonopplysningGrunnlag) {
        val søker = personopplysningGrunnlag.søker
        val barna = personopplysningGrunnlag.barna

        val tidslinjeMedAndeler = tilkjentYtelse.tilTidslinjeMedAndeler()

        tidslinjeMedAndeler.toSegments().forEach {
            val søkersAndeler = hentSøkersAndeler(it.value, søker)
            val barnasAndeler = hentBarnasAndeler(it.value, barna)

            validerAtAndelerForPartErGyldige(søkersAndeler, søker, 2, 2000)
            barnasAndeler.forEach { personMedAndeler ->
                validerAtAndelerForPartErGyldige(personMedAndeler.second, personMedAndeler.first, 2, 2000)
            }
        }
    }

    /**
     * Validerer at barna kun har andeler som løper i alderen 0-18 år.
     * Validerer at søker kun har andeler som løper i fra 0 år på eldste barn til 18 år på yngste barn.
     */
    fun validerAtTilkjentYtelseKunHarGyldigTotalPeriode(tilkjentYtelse: TilkjentYtelse,
                                                        personopplysningGrunnlag: PersonopplysningGrunnlag) {
        val søker = personopplysningGrunnlag.søker
        val barna = personopplysningGrunnlag.barna.sortedBy { it.fødselsdato }
        val minUtbetalingsdato = barna.first().fødselsdato
        val maksUtbetalingsdato = barna.last().fødselsdato.plusYears(18)

        val søkersAndeler = hentSøkersAndeler(tilkjentYtelse.andelerTilkjentYtelse.toList(), søker)
        val barnasAndeler = hentBarnasAndeler(tilkjentYtelse.andelerTilkjentYtelse.toList(), barna)

        barnasAndeler.forEach { barnMedAndeler ->
            if (barnMedAndeler.second.any {
                        it.stønadFom < barnMedAndeler.first.fødselsdato || it.stønadTom > barnMedAndeler.first.fødselsdato.plusYears(
                                18)
                    }) {
                throw Feil(message = "Barn har andeler som strekker seg utover 0-18 år",
                           frontendFeilmelding = "${barnMedAndeler.first.personIdent.ident} har utbetalinger utover 0-18 år. Ta kontakt med teamet for hjelp.")
            }
        }

        if (søkersAndeler.any { it.stønadFom < minUtbetalingsdato || it.stønadTom > maksUtbetalingsdato }) {
            throw Feil(message = "Søker har andeler som strekker seg utover barnas 0-18 års periode",
                       frontendFeilmelding = "Søker har utbetalinger utover barnas 0-18 års periode. Ta kontakt med teamet for hjelp.")
        }
    }
}

private fun validerAtAndelerForPartErGyldige(andeler: List<AndelTilkjentYtelse>,
                                             person: Person,
                                             maksAntallAndeler: Int,
                                             maksTotalBeløp: Int) {
    if (andeler.size > maksAntallAndeler) {
        throw Feil(message = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatte andeler = ${maksAntallAndeler}, faktiske andeler = ${andeler.size}.",
                   frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. Kontakt teamet for hjelp")
    }

    val totalbeløp = andeler.map { it.beløp }
            .reduce { sum, beløp -> sum + beløp }
    if (totalbeløp > maksTotalBeløp) {
        throw Feil(message = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatt totalbeløp = ${maksTotalBeløp}, faktiske totalbeløp = ${totalbeløp}.",
                   frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. Kontakt teamet for hjelp")
    }
}