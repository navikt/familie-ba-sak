package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import minsteAvHver
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.kunAndelerTilOgMed3År
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinjeForSøkersYtelse
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.tilKronerPerValutaenhet
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.tilMånedligValutabeløp
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.times
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.filtrerSekundærland
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat.NORGE_ER_PRIMÆRLAND
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat.NORGE_ER_SEKUNDÆRLAND
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerHverKunVerdi
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.joinIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerKunVerdiMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullOgIkkeTom
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.leftJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.matematikk.minus
import no.nav.familie.ba.sak.kjerne.tidslinje.matematikk.rundAvTilHeltall
import no.nav.familie.ba.sak.kjerne.tidslinje.matematikk.sum
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.lagForskjøvetTidslinjeForOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.math.BigDecimal
import java.math.MathContext

/**
 * ADVARSEL: Muterer TilkjentYtelse
 * Denne BURDE gjøres ikke-muterbar og returnere en ny instans av TilkjentYtelse
 * Muteringen skyldes at TilkjentYtelse er under JPA-kontekst og ikke "tåler" copy(andelerTilkjentYtelse = ...)
 * Starten på én løsning er at EndretUtebetalingPeriode kobles løs fra AndelTilkjentYtelse og kobles rett på behandlingen
 */
fun beregnDifferanse(
    andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    valutakurser: Collection<Valutakurs>,
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
    barna: List<Person>,
    kompetanser: Collection<Kompetanse>,
    personResultater: Set<PersonResultat> = emptySet(),
): List<AndelTilkjentYtelse> {
    // Ta bort eventuell eksisterende differanseberegning, slik at kalkulertUtbetalingsbeløp er nasjonal sats
    // Men behold funksjonelle splitter som er påført tidligere ved å beholde fom og tom på andelene
    val utvidetBarnetrygdTidslinje =
        this.tilTidslinjeForSøkersYtelse(YtelseType.UTVIDET_BARNETRYGD)
            .utenDifferanseberegning()

    val småbarnstilleggTidslinje =
        this.tilTidslinjeForSøkersYtelse(YtelseType.SMÅBARNSTILLEGG)
            .utenDifferanseberegning()

    val barnasAndelerTidslinjer = this.tilSeparateTidslinjerForBarna()

    // Finn alle andelene frem til barna er 18 år. Det vil i praksis være ALLE andelene
    // Bruk bare andelene som kvalifiserer for differanseberegning mot søkers ytelser
    val barnasRelevanteAndelerInntil18År =
        barnasAndelerTidslinjer
            .tilAndelerSomSkalDifferanseberegnesMotSøkersYtelser(kompetanser, personResultater)

    // Lag tidslinjer for hvert barn som inneholder underskuddet fra differanseberegningen på ordinær barnetrygd.
    // Resultatet er tidslinjer med underskuddet som positivt beløp der det inntreffer
    // Dette er det utenlandske beløpet som gjenstår til å redusere søkers ytelser
    val barnasUnderskuddPåDifferanseberegningTidslinjer =
        barnasAndelerTidslinjer.tilUnderskuddPåDifferanseberegningen()

    // Vi finner hvor mye hvert barn skal ha som andel av utvidet barnetrygd på hvert tidspunkt.
    // Det tilsvarer utvidet barnetrygd på et gitt tidspunkt delt på antall relevante barn som har ytelse på det tidspunktet
    val barnasDelAvUtvidetBarnetrygdTidslinjer =
        utvidetBarnetrygdTidslinje.fordelBeløpPåBarnaMedAndeler(barnasRelevanteAndelerInntil18År)

    // Vi finner den utenlandske delen av utvidet barnetrygd,
    // som det minste av barnets underskudd og dets del av utvidet barnetrygd, og summerer resultatet for alle barna
    // Runder av summen HALF_UP, som betyr en mulig ulempe for søker
    // Avrundingen er valgt for i størst mulig grad få summen til å bli lik utvidet barnetrygd når alle barnas deler blir brukt,
    // slik at utvidet barnetrrygd blir 0. Ellers ville den av og til kunne blitt 1, selv om det var ytterligere underskudd
    val utenlandskDelAvUtvidetBarnetrygdTidslinje =
        minsteAvHver(
            barnasDelAvUtvidetBarnetrygdTidslinjer,
            barnasUnderskuddPåDifferanseberegningTidslinjer,
        ).sum().rundAvTilHeltall()

    // Til slutt oppdaterer vi differanseberegningen på utvidet barnetrygd med den utenlandske delen
    val differanseberegnetUtvidetBarnetrygdTidslinje =
        utvidetBarnetrygdTidslinje.oppdaterDifferanseberegning(utenlandskDelAvUtvidetBarnetrygdTidslinje)

    // For hvert barn finner vi ut hvor mye underskudd som gjenstår etter at delen av utvidet barnetrygd er trukket fra
    val barnasGjenståendeUnderskuddTidslinjer =
        barnasUnderskuddPåDifferanseberegningTidslinjer
            .minus(barnasDelAvUtvidetBarnetrygdTidslinjer)
            .filtrerHverKunVerdi { it > BigDecimal.ZERO }

    // For hvert barn kombiner andel-tidslinjen med 3-års-tidslinjen. Resultatet er andelene når barna er inntil 3 år
    // Bruk bare andelene som kvalifiserer for differanseberegning mot søkers ytelser
    // Må ta utgangspunkt i alle andeler for være sikker på at vi vurderer sekundærlandsperioder bare når barna er inntil 3 år
    val barnasRelevanteAndelerInntil3ÅrTidslinjer =
        barnasAndelerTidslinjer
            .kunAndelerTilOgMed3År(barna)
            .tilAndelerSomSkalDifferanseberegnesMotSøkersYtelser(kompetanser, personResultater)

    // Vi finner hvor mye hvert barn skal ha som andel av småbarnstillegget på hvert tidspunkt.
    // Det tilsvarer småbarnstillegget på et gitt tidspunkt delt på antall relevante barn under 3 år som har ytelse på det tidspunktet
    val barnasDelAvSmåbarnstilleggetTidslinjer =
        småbarnstilleggTidslinje.fordelBeløpPåBarnaMedAndeler(barnasRelevanteAndelerInntil3ÅrTidslinjer)

    // Vi finner den utenlandske delen av småbarnstillegget
    // som det minste av barnets underskudd og dets del av småbarnstillegget, og summerer resultatet for alle barna.
    // Runder av HALF_UP, som betyr en mulig ulempe for søker
    // Avrundingen er valgt for i størst mulig grad få summen til å bli lik småbarnstillegget når alle barnas deler blir brukt,
    // slik at småbarnstillegget blir 0. Ellers ville den av og til kunne blitt 1, selv om det var ytterligere underskudd
    val utenlandskDelAvSmåbarnstilleggTidslinje =
        minsteAvHver(
            barnasDelAvSmåbarnstilleggetTidslinjer,
            barnasGjenståendeUnderskuddTidslinjer,
        ).sum().rundAvTilHeltall()

    // Til slutt oppdaterer vi differanseberegningen på småbarnstillegget med den utenlandske delen
    val differanseberegnetSmåbarnstilleggTidslinje =
        småbarnstilleggTidslinje.oppdaterDifferanseberegning(utenlandskDelAvSmåbarnstilleggTidslinje)

    // Returner det fulle settet av andeler, både barnas andeler og de potensielt nye andelene for søkers ytelser
    return this.filter { !it.erSøkersAndel() } +
        differanseberegnetUtvidetBarnetrygdTidslinje.tilAndelTilkjentYtelse() +
        differanseberegnetSmåbarnstilleggTidslinje.tilAndelTilkjentYtelse()
}

/**
 * Funksjon som sjekker at hvert barns andel i en periode er vurdert med sekundærland-kompetanser eller sekundærlands-kompetanser og primærlands-kompetanser der barnets BOR_HOS_SØKER vilkår har utdypende vilkårsvurdering satt til enten BARN_BOR_I_EØS_MED_ANNEN_FORELDER eller BARN_BOR_I_STORBRITANNIA_MED_ANNEN_FORELDER,
 * Hvis ja beholdes andelene for alle barna med sekundærland-kompetanser
 * Hvis nei fjernes alle andelene, slik at perioden ikke har noen andeler
 */
fun Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>>.tilAndelerSomSkalDifferanseberegnesMotSøkersYtelser(
    kompetanser: Collection<Kompetanse>,
    personResultater: Set<PersonResultat>,
): Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>> {
    val barnasKompetanseTidslinjer = kompetanser.tilSeparateTidslinjerForBarna()

    val barnasKompetanseTidslinjerAvgrensetAvAndelsTidslinjer =
        this.joinIkkeNull(barnasKompetanseTidslinjer) { andel, kompetanse ->
            kompetanse
        }

    // Finner alle barns sekundærlandsperioder
    val sekundærlandsbarnTidslinje = barnasKompetanseTidslinjerAvgrensetAvAndelsTidslinjer.filtrerSekundærland()

    // Finner alle rene sekundærlandsperioder og alle sekundærland- og primærlandsperioder hvor barn bor i EØS eller Storbritannia med annen forelder.
    val helePeriodenErSekundærlandEllerPrimærlandMedBorIEØSEllerStorbritanniaMedAnnenForelderTidslinje = barnasKompetanseTidslinjerAvgrensetAvAndelsTidslinjer.tilSekundærlandsbarnEllerPrimærlandsbarnMedBorIEØSEllerStorbritanniaMedAnnenForelderTidslinje(personResultater)

    // Finner alle sekundærlandsbarn som faller innenfor periodene som skal differanseberegnes mot søkers ytelser. Det er kun sekundærlandsbarn som skal påvirke differanseberegning til søker.
    val barnSomSkalPåvirkeSøkersYtelserTidslinje =
        sekundærlandsbarnTidslinje.kombinerKunVerdiMed(helePeriodenErSekundærlandEllerPrimærlandMedBorIEØSEllerStorbritanniaMedAnnenForelderTidslinje) { _, helePeriodenErSekundærlandEllerPrimærMedBorIEØSEllerStorbritanniaMedAnnenForelder ->
            helePeriodenErSekundærlandEllerPrimærMedBorIEØSEllerStorbritanniaMedAnnenForelder
        }

    // Plukker ut andelene til sekundærlandsbarna som skal påvirke differanseberegning til søker.
    return this.joinIkkeNull(barnSomSkalPåvirkeSøkersYtelserTidslinje) { andel, erBarnSomSkalPåvirkeSøkersYtelser ->
        andel.takeIf { erBarnSomSkalPåvirkeSøkersYtelser }
    }
}

fun Map<Aktør, Tidslinje<Kompetanse, Måned>>.tilSekundærlandsbarnEllerPrimærlandsbarnMedBorIEØSEllerStorbritanniaMedAnnenForelderTidslinje(personResultater: Set<PersonResultat>): Tidslinje<Boolean, Måned> {
    val barnBorIEØSEllerStorbritanniaMedAnnenForelderTidslinjer = personResultater.finnPerioderBarnBorIEØSEllerStorbritanniaMedAnnenForelder()

    return this.leftJoin(barnBorIEØSEllerStorbritanniaMedAnnenForelderTidslinjer) { kompetanse, borIEØSEllerStorbritanniaMedAnnenForelder ->
        when {
            kompetanse != null ->
                kompetanse.resultat == NORGE_ER_SEKUNDÆRLAND ||
                    kompetanse.resultat == NORGE_ER_PRIMÆRLAND && borIEØSEllerStorbritanniaMedAnnenForelder == true

            else -> false
        }
    }.values.kombinerUtenNullOgIkkeTom { erSekundærlandEllerPrimærMedBorIEØSEllerStorbritanniaMedAnnenForelderListe -> erSekundærlandEllerPrimærMedBorIEØSEllerStorbritanniaMedAnnenForelderListe.all { it } }
}

fun Set<PersonResultat>.finnPerioderBarnBorIEØSEllerStorbritanniaMedAnnenForelder(): Map<Aktør, Tidslinje<Boolean, Måned>> =
    associate { personResultat ->
        personResultat.aktør to
            personResultat.vilkårResultater.lagForskjøvetTidslinjeForOppfylteVilkår(Vilkår.BOR_MED_SØKER).map { vilkårResultat ->
                vilkårResultat?.utdypendeVilkårsvurderinger?.any {
                    it in
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_EØS_MED_ANNEN_FORELDER,
                            UtdypendeVilkårsvurdering.BARN_BOR_I_STORBRITANNIA_MED_ANNEN_FORELDER,
                        )
                }
            }.filtrerIkkeNull()
    }

fun Tidslinje<AndelTilkjentYtelse, Måned>.fordelBeløpPåBarnaMedAndeler(
    barnasAndeler: Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>>,
): Map<Aktør, Tidslinje<BigDecimal, Måned>> {
    val antallAktørerMedYtelseTidslinje =
        barnasAndeler.values.kombinerUtenNullOgIkkeTom { it.count() }

    val ytelsePerBarnTidslinje =
        this.kombinerUtenNullMed(antallAktørerMedYtelseTidslinje) { andel, antall ->
            andel.kalkulertUtbetalingsbeløp.toBigDecimal().divide(antall.toBigDecimal(), MathContext.DECIMAL32)
        }

    return barnasAndeler.kombinerKunVerdiMed(ytelsePerBarnTidslinje) { _, ytelsePerBarn -> ytelsePerBarn }
}
