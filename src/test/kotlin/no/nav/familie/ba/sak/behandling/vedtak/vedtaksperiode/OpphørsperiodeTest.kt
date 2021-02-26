package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpphørsperiodeTest {

    val søker = tilfeldigPerson()
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()

    val personopplysningGrunnlag = PersonopplysningGrunnlag(
            behandlingId = 0L,
            personer = mutableSetOf(søker, barn1, barn2)
    )

    @Test
    fun `Skal utlede opphørsperiode mellom oppfylte perioder`() {

        val periodeTomFørsteAndel = inneværendeMåned().minusYears(2)
        val periodeFomAndreAndel = inneværendeMåned().minusYears(1)
        val periodeTomAndreAndel = inneværendeMåned().minusMonths(10)
        val periodeFomSisteAndel = inneværendeMåned().minusMonths(4)
        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                periodeTomFørsteAndel.toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val andel2Barn1 = lagAndelTilkjentYtelse(periodeFomAndreAndel.toString(),
                                                 periodeTomAndreAndel.toString(),
                                                 YtelseType.ORDINÆR_BARNETRYGD,
                                                 1054,
                                                 person = barn1)

        val andel3Barn1 = lagAndelTilkjentYtelse(periodeFomSisteAndel.toString(),
                                                 inneværendeMåned().plusMonths(12).toString(),
                                                 YtelseType.ORDINÆR_BARNETRYGD,
                                                 1054,
                                                 person = barn1)

        val opphørsperioder = mapTilOpphørsperioder(
                andelerTilkjentYtelse = listOf(andelBarn1, andel2Barn1, andel3Barn1),
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        assertEquals(2, opphørsperioder.size)
        assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(periodeFomAndreAndel.forrigeMåned(), opphørsperioder[0].periodeTom?.toYearMonth())

        assertEquals(periodeTomAndreAndel.nesteMåned(), opphørsperioder[1].periodeFom.toYearMonth())
        assertEquals(periodeFomSisteAndel.forrigeMåned(), opphørsperioder[1].periodeTom?.toYearMonth())
    }

    @Test
    fun `Skal utlede opphørsperiode når siste utbetalingsperiode er før neste måned`() {

        val periodeTomFørsteAndel = inneværendeMåned().minusYears(1)
        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                periodeTomFørsteAndel.toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val opphørsperioder = mapTilOpphørsperioder(
                andelerTilkjentYtelse = listOf(andelBarn1),
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        assertEquals(1, opphørsperioder.size)
        assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(null, opphørsperioder[0].periodeTom)
    }

    @Test
    fun `Skal utlede opphørsperiode fra neste måned når siste utbetalingsperiode er inneværende måned`() {

        val periodeTomFørsteAndel = inneværendeMåned()
        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                periodeTomFørsteAndel.toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val opphørsperioder = mapTilOpphørsperioder(
                andelerTilkjentYtelse = listOf(andelBarn1),
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        assertEquals(1, opphørsperioder.size)
        assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(null, opphørsperioder[0].periodeTom)
    }

    @Test
    fun `Skal utlede opphørsperiode når ytelsen reduseres i revurdering`() {

        val reduksjonFom = inneværendeMåned().minusYears(5)
        val reduksjonTom = inneværendeMåned().minusYears(3)
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn1 = lagAndelTilkjentYtelse(reduksjonTom.toString(),
                                                inneværendeMåned().plusMonths(12).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val opphørsperioder = mapTilOpphørsperioder(
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1),
                andelerTilkjentYtelse = listOf(andelBarn1),
                personopplysningGrunnlag = personopplysningGrunnlag,
                forrigePersonopplysningGrunnlag = personopplysningGrunnlag
        )

        assertEquals(1, opphørsperioder.size)
        assertEquals(reduksjonFom, opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(reduksjonTom.forrigeMåned(), opphørsperioder[0].periodeTom?.toYearMonth())
    }

    @Test
    fun `Skal utlede opphørsperiode når ytelsen reduseres i revurdering og ytelsen ikke lenger er løpende`() {

        val reduksjonFom = inneværendeMåned()
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                reduksjonFom.toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val opphørsperioder = mapTilOpphørsperioder(
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1),
                andelerTilkjentYtelse = listOf(andelBarn1),
                personopplysningGrunnlag = personopplysningGrunnlag,
                forrigePersonopplysningGrunnlag = personopplysningGrunnlag
        )

        assertEquals(1, opphørsperioder.size)
        assertEquals(reduksjonFom.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(null, opphørsperioder[0].periodeTom?.toYearMonth())
    }
}