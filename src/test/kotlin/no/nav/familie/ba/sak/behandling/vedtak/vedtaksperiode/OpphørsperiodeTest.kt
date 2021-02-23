package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.YtelseType
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
    fun `Skal utlede opphørsperiode mellom to oppfylte perioder`() {

        val periodeTomFørsteAndel = inneværendeMåned().minusYears(2)
        val periodeFomSisteAndel = inneværendeMåned().minusYears(1)
        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                periodeTomFørsteAndel.toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val andel2Barn1 = lagAndelTilkjentYtelse(periodeFomSisteAndel.toString(),
                                                 inneværendeMåned().plusMonths(12).toString(),
                                                 YtelseType.ORDINÆR_BARNETRYGD,
                                                 1054,
                                                 person = barn1)

        val opphørsperioder = finnOpphørsperioder(
                forrigeAndelerTilkjentYtelse = emptyList(),
                andelerTilkjentYtelse = listOf(andelBarn1, andel2Barn1),
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        assertEquals(1, opphørsperioder.size)
        assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(periodeFomSisteAndel.forrigeMåned(), opphørsperioder[0].periodeTom.toYearMonth())
    }
}