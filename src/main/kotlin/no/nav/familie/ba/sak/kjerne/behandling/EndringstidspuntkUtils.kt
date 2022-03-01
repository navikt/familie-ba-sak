package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.PeriodeVilkår
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

fun finnEndringstidspunkt(
    nyVilkårsvurdering: Vilkårsvurdering,
    gammelVilkårsvurdering: Vilkårsvurdering,
    nyeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    gamleAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    nyttPersonopplysningGrunnlag: PersonopplysningGrunnlag,
    gammeltPersonopplysningGrunnlag: PersonopplysningGrunnlag
): LocalDate {

    val nyeVilkårsperioder =
        nyVilkårsvurdering.hentInnvilgedePerioder(nyttPersonopplysningGrunnlag).let { it.first + it.second }
    val gamleVilkårsperioder =
        gammelVilkårsvurdering.hentInnvilgedePerioder(gammeltPersonopplysningGrunnlag).let { it.first + it.second }

    val tidligesteEndringIVilkår = finnTidligesteEndringIVilkår(nyeVilkårsperioder, gamleVilkårsperioder)

    val tidligesteEndringIAndeler =
        finnTidligesteEndringIAndelTilkjentYtelse(nyeAndelerTilkjentYtelse, gamleAndelerTilkjentYtelse)

    val tidligesteEndringEndretUtbetalinger =
        finnTidligesteEndringIEndredeUtbetalinger(
            nyeAndelerTilkjentYtelse.flatMap { it.endretUtbetalingAndeler },
            gamleAndelerTilkjentYtelse.flatMap { it.endretUtbetalingAndeler }
        )

    return listOfNotNull(
        tidligesteEndringIAndeler,
        tidligesteEndringIVilkår,
        tidligesteEndringEndretUtbetalinger
    ).minByOrNull { it } ?: TIDENES_ENDE
}

private fun finnTidligesteEndringIAndelTilkjentYtelse(
    nyeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    gamleAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
): LocalDate? {
    val eldsteDiffFraGamleAndeler = gamleAndelerTilkjentYtelse.finnTidligesteForskjell(nyeAndelerTilkjentYtelse)

    val eldsteDiffFraNyeAndeler =
        nyeAndelerTilkjentYtelse.finnTidligesteForskjell(
            andreAndeler = gamleAndelerTilkjentYtelse,
            før = eldsteDiffFraGamleAndeler ?: TIDENES_ENDE,
        )

    return listOfNotNull(eldsteDiffFraNyeAndeler, eldsteDiffFraGamleAndeler).minByOrNull { it }
}

private fun List<AndelTilkjentYtelse>.finnTidligesteForskjell(
    andreAndeler: List<AndelTilkjentYtelse>,
    før: LocalDate? = TIDENES_ENDE
) = this
    .sortedBy { it.stønadFom }
    .filter { it.stønadFom.førsteDagIInneværendeMåned().isBefore((før)) }
    .firstNotNullOfOrNull { andel ->
        when {
            !andreAndeler.any { it.erFunksjoneltLikUtenomTomDato(andel) } ->
                andel.stønadFom.førsteDagIInneværendeMåned()
            !andreAndeler.any { it.erFunksjoneltLik(andel) } ->
                andel.stønadTom.sisteDagIInneværendeMåned()
            else -> null
        }
    }

private fun finnTidligesteEndringIEndredeUtbetalinger(
    nyeEndretUtbetalingsandeler: List<EndretUtbetalingAndel>,
    gamleEndretUtbetalingsandeler: List<EndretUtbetalingAndel>,
): LocalDate? {
    val eldsteDiffFraGamleAndeler = gamleEndretUtbetalingsandeler.finnTidligesteForskjell(nyeEndretUtbetalingsandeler)

    val eldsteDiffFraNyeAndeler =
        nyeEndretUtbetalingsandeler.finnTidligesteForskjell(
            andreEndretUtbetalinger = gamleEndretUtbetalingsandeler,
            før = eldsteDiffFraGamleAndeler ?: TIDENES_ENDE,
        )

    return listOfNotNull(eldsteDiffFraNyeAndeler, eldsteDiffFraGamleAndeler).minByOrNull { it }
}

@JvmName("finnTidligesteForskjellEndretUtbetalingAndel")
private fun List<EndretUtbetalingAndel>.finnTidligesteForskjell(
    andreEndretUtbetalinger: List<EndretUtbetalingAndel>,
    før: LocalDate = TIDENES_ENDE
) = this
    .sortedBy { it.fom }
    .filter { it.fom!!.førsteDagIInneværendeMåned().isBefore((før)) }
    .firstNotNullOfOrNull { andel ->
        when {
            !andreEndretUtbetalinger.any { it.erFunksjoneltLikUtenomTomDato(andel) } ->
                andel.fom!!.førsteDagIInneværendeMåned()
            !andreEndretUtbetalinger.any { it.erFunksjoneltLik(andel) } ->
                andel.tom!!.sisteDagIInneværendeMåned()
            else -> null
        }
    }

private fun finnTidligesteEndringIVilkår(
    nyeVilkårsperioder: List<PeriodeResultat>,
    gamleVilkårsperioder: List<PeriodeResultat>
): LocalDate? {
    val tidligesteDiffFraGamleVilkårsperioder = gamleVilkårsperioder.finnTidligesteForskjell(nyeVilkårsperioder)

    val tidligesteDiffFranyeVilkårsperioder =
        nyeVilkårsperioder.finnTidligesteForskjell(
            andrePerioderesultater = gamleVilkårsperioder,
            før = tidligesteDiffFraGamleVilkårsperioder ?: TIDENES_ENDE,
        )

    // Andeler tilkjent ytelse er forskøvet en månde etter vilkårene. Legger derfor til en måned.
    return listOfNotNull(tidligesteDiffFranyeVilkårsperioder, tidligesteDiffFraGamleVilkårsperioder)
        .minByOrNull { it }
        ?.plusMonths(1)
}

@JvmName("finnTidligesteForskjellPeriodeResultat")
private fun List<PeriodeResultat>.finnTidligesteForskjell(
    andrePerioderesultater: List<PeriodeResultat>,
    før: LocalDate = TIDENES_ENDE
) = this
    .sortedBy { it.periodeFom }
    .filter { it.periodeFom!!.førsteDagIInneværendeMåned().isBefore((før)) }
    .firstNotNullOfOrNull { andel ->
        when {
            !andrePerioderesultater.any { it.erFunksjoneltLikUtenomTomDato(andel) } ->
                andel.periodeFom ?: TIDENES_MORGEN
            !andrePerioderesultater.any { it.erFunksjoneltLik(andel) } ->
                andel.periodeTom
            else -> null
        }
    }

private fun Set<PeriodeVilkår>.erFunksjoneltLikUtenomDatoer(andrePeriodeVilkårResultater: Set<PeriodeVilkår>): Boolean {
    return this.size == andrePeriodeVilkårResultater.size &&
        this.all { periodeVilkårResultat ->
            andrePeriodeVilkårResultater.any { it.erFunksjoneltLikUtenomDatoer(periodeVilkårResultat) }
        }
}

private fun PeriodeVilkår.erFunksjoneltLikUtenomDatoer(annenPeriodeVilkår: PeriodeVilkår): Boolean {
    return this.vilkårType == annenPeriodeVilkår.vilkårType &&
        this.resultat == annenPeriodeVilkår.resultat &&
        this.utdypendeVilkårsvurderinger.all { annenPeriodeVilkår.utdypendeVilkårsvurderinger.contains(it) }
}

fun AndelTilkjentYtelse.erFunksjoneltLik(annenAndelTilkjentYtelse: AndelTilkjentYtelse): Boolean =
    this.aktør.aktørId == annenAndelTilkjentYtelse.aktør.aktørId &&
        this.kalkulertUtbetalingsbeløp == annenAndelTilkjentYtelse.kalkulertUtbetalingsbeløp &&
        this.stønadFom == annenAndelTilkjentYtelse.stønadFom &&
        this.stønadTom == annenAndelTilkjentYtelse.stønadTom &&
        this.type == annenAndelTilkjentYtelse.type &&
        this.sats == annenAndelTilkjentYtelse.sats &&
        this.prosent == annenAndelTilkjentYtelse.prosent

fun AndelTilkjentYtelse.erFunksjoneltLikUtenomTomDato(annenAndelTilkjentYtelse: AndelTilkjentYtelse): Boolean =
    this.aktør.aktørId == annenAndelTilkjentYtelse.aktør.aktørId &&
        this.kalkulertUtbetalingsbeløp == annenAndelTilkjentYtelse.kalkulertUtbetalingsbeløp &&
        this.stønadFom == annenAndelTilkjentYtelse.stønadFom &&
        this.type == annenAndelTilkjentYtelse.type &&
        this.sats == annenAndelTilkjentYtelse.sats &&
        this.prosent == annenAndelTilkjentYtelse.prosent

private fun PeriodeResultat.erFunksjoneltLik(annenPeriode: PeriodeResultat): Boolean {
    return this.periodeFom == annenPeriode.periodeFom &&
        this.periodeTom == annenPeriode.periodeTom &&
        this.aktør.aktørId == annenPeriode.aktør.aktørId &&
        // Har allerede tatt høyde for datoer i PeriodeResultat sin periode
        this.vilkårResultater.erFunksjoneltLikUtenomDatoer(annenPeriode.vilkårResultater)
}

private fun PeriodeResultat.erFunksjoneltLikUtenomTomDato(annenPeriode: PeriodeResultat): Boolean {
    return this.periodeFom == annenPeriode.periodeFom &&
        this.aktør.aktørId == annenPeriode.aktør.aktørId &&
        // Har allerede tatt høyde for fom datoer i PeriodeResultat sin periode
        this.vilkårResultater.erFunksjoneltLikUtenomDatoer(annenPeriode.vilkårResultater)
}

fun EndretUtbetalingAndel.erFunksjoneltLik(annenEndretUtbetalingAndel: EndretUtbetalingAndel): Boolean =
    this.person?.aktør?.aktørId == annenEndretUtbetalingAndel.person?.aktør?.aktørId &&
        this.prosent == annenEndretUtbetalingAndel.prosent &&
        this.fom == annenEndretUtbetalingAndel.fom &&
        this.tom == annenEndretUtbetalingAndel.tom &&
        this.årsak == annenEndretUtbetalingAndel.årsak &&
        this.avtaletidspunktDeltBosted == annenEndretUtbetalingAndel.avtaletidspunktDeltBosted &&
        this.søknadstidspunkt == annenEndretUtbetalingAndel.søknadstidspunkt

fun EndretUtbetalingAndel.erFunksjoneltLikUtenomTomDato(annenEndretUtbetalingAndel: EndretUtbetalingAndel): Boolean =
    this.person?.aktør?.aktørId == annenEndretUtbetalingAndel.person?.aktør?.aktørId &&
        this.prosent == annenEndretUtbetalingAndel.prosent &&
        this.fom == annenEndretUtbetalingAndel.fom &&
        this.årsak == annenEndretUtbetalingAndel.årsak &&
        this.avtaletidspunktDeltBosted == annenEndretUtbetalingAndel.avtaletidspunktDeltBosted &&
        this.søknadstidspunkt == annenEndretUtbetalingAndel.søknadstidspunkt
