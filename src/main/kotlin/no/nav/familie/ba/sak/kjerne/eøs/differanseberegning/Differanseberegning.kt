package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.common.erTilogMed3ÅrTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.tilKronerPerValutaenhet
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.tilMånedligValutabeløp
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.times
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.join
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.joinIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerKunVerdiMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullOgIkkeTom
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull

/**
 * ADVARSEL: Muterer TilkjentYtelse
 * Denne BURDE gjøres ikke-muterbar og returnere en ny instans av TilkjentYtelse
 * Muteringen skyldes at TilkjentYtelse er under JPA-kontekst og ikke "tåler" copy(andelerTilkjentYtelse = ...)
 * Starten på én løsning er at EndretUtebetalingPeriode kobles løs fra AndelTilkjentYtelse og kobles rett på behandlingen
 */
fun beregnDifferanse(
    andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    valutakurser: Collection<Valutakurs>
): List<AndelTilkjentYtelse> {
    val utenlandskePeriodebeløpTidslinjer = utenlandskePeriodebeløp.tilSeparateTidslinjerForBarna()
    val valutakursTidslinjer = valutakurser.tilSeparateTidslinjerForBarna()
    val andelTilkjentYtelseTidslinjer = andelerTilkjentYtelse.tilSeparateTidslinjerForBarna()

    val barnasUtenlandskePeriodebeløpINorskeKronerTidslinjer =
        utenlandskePeriodebeløpTidslinjer.outerJoin(valutakursTidslinjer) { upb, valutakurs ->
            upb.tilMånedligValutabeløp() * valutakurs.tilKronerPerValutaenhet()
        }

    val barnasDifferanseberegneteAndelTilkjentYtelseTidslinjer =
        andelTilkjentYtelseTidslinjer.outerJoin(barnasUtenlandskePeriodebeløpINorskeKronerTidslinjer) { aty, beløp ->
            aty.oppdaterDifferanseberegning(beløp)
        }

    val barnasAndeler = barnasDifferanseberegneteAndelTilkjentYtelseTidslinjer.tilAndelerTilkjentYtelse()
    val søkersAndeler = andelerTilkjentYtelse.filter { it.erSøkersAndel() }

    return søkersAndeler + barnasAndeler
}

/**
 * ADVARSEL: Muterer TilkjentYtelse
 * Differanseberegner søkers ytelser, dvs utvidet barnetrygd og småbarnstillegg
 * Forutsetningen er at barnas andeler allerede er differanseberegnet
 * Funksjonen returnerer det nye settet av andeler tilkjent ytelse, inklusive barnas
 */
fun Collection<AndelTilkjentYtelse>.differanseberegnSøkersYtelser(
    barna: List<Person>
): List<AndelTilkjentYtelse> {
    val utvidetBarnetrygdTidslinje = this.tilTidslinjeForSøkersYtelse(YtelseType.UTVIDET_BARNETRYGD)
    val småbarnstilleggTidslinje = this.tilTidslinjeForSøkersYtelse(YtelseType.SMÅBARNSTILLEGG)
    val barnasAndelerTidslinjer = this.tilSeparateTidslinjerForBarna()

    // Lag tidslinjer for hvert barn som inneholder underskuddet fra differanseberegningen på ordinær barnetrygd.
    // Resultatet er tidslinjer med underskuddet som positivt beløp der det inntreffer
    val barnasUnderskuddPåDifferanseberegningTidslinjer =
        barnasAndelerTidslinjer.tilUnderskuddPåDifferanseberegningen()

    // Vi finner hvor mye hvert barn skal ha som andel av utvidet barnetrygd på hvert tidspunkt.
    // Det tilsvarer utvidet barnetrygd på et gitt tidspunkt delt på antall barn som har ytelse på det tidspunktet
    val barnasDelAvUtvidetBarnetrygdTidslinjer =
        utvidetBarnetrygdTidslinje.fordelForholdsmessigPåBarnasAndeler(barnasAndelerTidslinjer)

    // Vi finner det minste av barnets underskudd og del av utvidet barnetrygd
    // og summerer resultatet for alle barna
    val reduksjonUtvidetBarnetrygdTidslinje = minsteAvHver(
        barnasDelAvUtvidetBarnetrygdTidslinjer,
        barnasUnderskuddPåDifferanseberegningTidslinjer
    ).sum()

    // Til slutt oppdaterer vi differanseberegningen på utvidet barnetrygd med reduksjonen
    val differanseberegnetUtvidetBarnetrygdTidslinje =
        utvidetBarnetrygdTidslinje.oppdaterDifferanseberegning(reduksjonUtvidetBarnetrygdTidslinje)

    // For hvert barn finner vi ut hvor mye underskudd som gjenstår etter at delen av utvidet barnetrygd er trukket fra
    val barnasGjenståendeUnderskuddTidslinjer = barnasUnderskuddPåDifferanseberegningTidslinjer
        .minus(barnasDelAvUtvidetBarnetrygdTidslinjer)
        .filtrerHverKunVerdi { it > 0 }

    // For hvert barn kombiner andel-tidslinjen med 3-års-tidslinjen. Resultatet er andelene når barna er inntil 3 år
    val barnasAndelerInntil3ÅrTidslinjer = barnasAndelerTidslinjer.kunAndelerTilOgMed3År(barna)

    // Vi finner hvor mye hvert barn skal ha som andel av småbarnstillegget på hvert tidspunkt.
    // Det tilsvarer småbarnstillegget på et gitt tidspunkt delt på antall barn under 3 år som har ytelse på det tidspunktet
    val barnasDelAvSmåbarnstilleggetTidslinjer =
        småbarnstilleggTidslinje.fordelForholdsmessigPåBarnasAndeler(barnasAndelerInntil3ÅrTidslinjer)

    // Vi finner det minste av barnets underskudd og del av småbarnstillegget
    // og summerer resultatet for alle barna
    val reduksjonSmåbarnstilleggTidslinje = minsteAvHver(
        barnasDelAvSmåbarnstilleggetTidslinjer,
        barnasGjenståendeUnderskuddTidslinjer
    ).sum()

    // Til slutt oppdaterer vi differanseberegningen på småbarnstillegget med reduksjonen
    val differanseberegnetSmåbarnstilleggTidslinje =
        småbarnstilleggTidslinje.oppdaterDifferanseberegning(reduksjonSmåbarnstilleggTidslinje)

    return this.filter { !it.erSøkersAndel() } +
        differanseberegnetUtvidetBarnetrygdTidslinje.tilAndelTilkjentYtelse() +
        differanseberegnetSmåbarnstilleggTidslinje.tilAndelTilkjentYtelse()
}

fun Tidslinje<AndelTilkjentYtelse, Måned>.fordelForholdsmessigPåBarnasAndeler(
    barnasAndeler: Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>>
): Map<Aktør, Tidslinje<Int, Måned>> {
    val antallAktørerMedYtelseTidslinje =
        barnasAndeler.values.kombinerUtenNullOgIkkeTom { it.count() }

    val ytelsePerBarnTidslinje =
        this.kombinerUtenNullMed(antallAktørerMedYtelseTidslinje) { andel, antall ->
            andel.kalkulertUtbetalingsbeløp / antall
        }

    return barnasAndeler.kombinerKunVerdiMed(ytelsePerBarnTidslinje) { _, ytelsePerBarn -> ytelsePerBarn }
}

fun Tidslinje<AndelTilkjentYtelse, Måned>.oppdaterDifferanseberegning(
    differanseberegnetBeløpTidslinje: Tidslinje<Int, Måned>
): Tidslinje<AndelTilkjentYtelse, Måned> {
    return this.kombinerMed(differanseberegnetBeløpTidslinje) { andel, differanseberegning ->
        andel.oppdaterDifferanseberegning(differanseberegning?.toBigDecimal())
    }
}

fun Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>>.tilUnderskuddPåDifferanseberegningen() =
    mapValues { (_, tidslinje) ->
        tidslinje
            .mapIkkeNull { innhold -> innhold.differanseberegnetPeriodebeløp }
            .mapIkkeNull { maxOf(-it, 0) }
            .filtrer { it != null && it > 0 }
    }

fun Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>>.kunAndelerTilOgMed3År(barna: List<Person>):
    Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>> {
    val barnasErInntil3ÅrTidslinjer = barna.associate { it.aktør to erTilogMed3ÅrTidslinje(it.fødselsdato) }

    // For hvert barn kombiner andel-tidslinjen med 3-års-tidslinjen. Resultatet er andelene når barna er inntil 3 år
    return this.joinIkkeNull(barnasErInntil3ÅrTidslinjer) { andel, _ -> andel }
}

fun <K, I : Comparable<I>, T : Tidsenhet> minsteAvHver(
    aTidslinjer: Map<K, Tidslinje<I, T>>,
    bTidslinjer: Map<K, Tidslinje<I, T>>
) = aTidslinjer.joinIkkeNull(bTidslinjer) { a, b -> minOf(a, b) }

fun <K, T : Tidsenhet> Map<K, Tidslinje<Int, T>>.minus(
    bTidslinjer: Map<K, Tidslinje<Int, T>>
) = this.join(bTidslinjer) { a, b ->
    when {
        a != null && b != null -> a - b
        else -> a
    }
}

fun <K, I, T : Tidsenhet> Map<K, Tidslinje<I, T>>.filtrerHverKunVerdi(
    filter: (I) -> Boolean
) = mapValues { (_, tidslinje) -> tidslinje.filtrer { if (it != null) filter(it) else false } }

fun Map<Aktør, Tidslinje<Int, Måned>>.sum() =
    values.kombinerUtenNullOgIkkeTom { it.sum() }
