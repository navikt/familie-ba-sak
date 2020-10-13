package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.tilTidslinjeMedAndeler
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
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

/**
 * Ekstra sikkerhet rundt hva som utbetales som på sikt vil legges inn i
 * de respektive stegene SB håndterer slik at det er lettere for SB å rette feilene.
 */
object TilkjentYtelseValidering {

    fun validerAtTilkjentYtelseHarGyldigEtterbetalingsperiode(tilkjentYtelse: TilkjentYtelse) {
        val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(tilkjentYtelse.behandling.opprettetTidspunkt)
        if (tilkjentYtelse.andelerTilkjentYtelse.any {
                    it.stønadFom < gyldigEtterbetalingFom
                }) {
            throw UtbetalingsikkerhetFeil(message = "Utbetalingsperioder for en eller flere av partene/personene går mer enn 3 år tilbake i tid. Gå tilbake til vilkårsvurderingen og endre på datoene, , eller ta kontakt med teamet for hjelp.",
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

            validerAtAndelerForPartErGyldige(søker, søkersAndeler)

            barnasAndeler.forEach { (person, andeler) ->
                validerAtAndelerForPartErGyldige(person, andeler)
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
        val minUtbetalingsdato = barna.first().fødselsdato.førsteDagINesteMåned()
        val maksUtbetalingsdato = barna.last().fødselsdato.plusYears(18).sisteDagIForrigeMåned()

        val søkersAndeler = hentSøkersAndeler(tilkjentYtelse.andelerTilkjentYtelse.toList(), søker)
        val barnasAndeler = hentBarnasAndeler(tilkjentYtelse.andelerTilkjentYtelse.toList(), barna)

        barnasAndeler.forEach { barnMedAndeler ->
            if (barnMedAndeler.second.any {
                        it.stønadFom < barnMedAndeler.first.fødselsdato.førsteDagINesteMåned() || it.stønadTom > barnMedAndeler.first.fødselsdato.plusYears(
                                18).sisteDagIForrigeMåned()
                    }) {
                throw UtbetalingsikkerhetFeil(message = "Barn har andeler som strekker seg utover 0-18 år",
                                              frontendFeilmelding = "${barnMedAndeler.first.personIdent.ident} har utbetalinger utover 0-18 år. Ta kontakt med teamet for hjelp.")
            }
        }

        if (søkersAndeler.any { it.stønadFom < minUtbetalingsdato || it.stønadTom > maksUtbetalingsdato }) {
            throw UtbetalingsikkerhetFeil(message = "Søker har andeler som strekker seg utover barnas 0-18 års periode",
                                          frontendFeilmelding = "Søker har utbetalinger utover barnas 0-18 års periode. Ta kontakt med teamet for hjelp.")
        }
    }

    fun valider100ProsentGraderingForBarna(behandlendeBehandlingTilkjentYtelse: TilkjentYtelse,
                                           barnMedAndreTilkjentYtelse: List<Pair<Person, List<TilkjentYtelse>>>,
                                           personopplysningGrunnlag: PersonopplysningGrunnlag) {
        val barna = personopplysningGrunnlag.barna.sortedBy { it.fødselsdato }

        val barnasAndeler = hentBarnasAndeler(behandlendeBehandlingTilkjentYtelse.andelerTilkjentYtelse.toList(), barna)

        barnasAndeler.forEach { (barn, andeler) ->
            val barnsAndelerFraAndreBehandlinger =
                    barnMedAndreTilkjentYtelse.filter { it.first.personIdent.ident == barn.personIdent.ident }
                            .flatMap { it.second }
                            .flatMap { it.andelerTilkjentYtelse }

            andeler.forEach { andelTilkjentYtelse ->
                if (barnsAndelerFraAndreBehandlinger.any { andelTilkjentYtelse.overlapperMed(it) }) {
                    throw UtbetalingsikkerhetFeil(message = "Vi finner flere utbetalinger for barn på behandling ${behandlendeBehandlingTilkjentYtelse.behandling.id}",
                                                  frontendFeilmelding = "Det utbetales allerede barnetrygd (${andelTilkjentYtelse.type.name}) for ${barn.personIdent} i perioden ${andelTilkjentYtelse.stønadFom} - ${andelTilkjentYtelse.stønadTom}.")
                }
            }
        }
    }
}

private fun validerAtAndelerForPartErGyldige(person: Person,
                                             andeler: List<AndelTilkjentYtelse>,
                                             maksAntallAndeler: Int = 2,
                                             maksTotalBeløp: Int = 2500) {
    if (andeler.size > maksAntallAndeler) {
        throw UtbetalingsikkerhetFeil(message = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatte andeler = ${maksAntallAndeler}, faktiske andeler = ${andeler.size}.",
                                      frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. Kontakt teamet for hjelp")
    }

    val totalbeløp = andeler.map { it.beløp }
            .fold(0) { sum, beløp -> sum + beløp }
    if (totalbeløp > maksTotalBeløp) {
        throw UtbetalingsikkerhetFeil(message = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatt totalbeløp = ${maksTotalBeløp}, faktiske totalbeløp = ${totalbeløp}.",
                                      frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. Kontakt teamet for hjelp")
    }
}