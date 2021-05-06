package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.tilfeldigPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YtelsePersonUtilsTest {

    val søker = tilfeldigPerson()
    val barn1 = tilfeldigPerson()

    @Test
    fun `Skal kun finne søknadsytelsePersoner`() {
        val søknadDTO = lagSøknadDTO(
                søkerIdent = søker.personIdent.ident,
                barnasIdenter = listOf(barn1.personIdent.ident)
        )

        val ytelsePersoner = YtelsePersonUtils.utledKrav(
                søknadDTO = søknadDTO,
                forrigeAndelerTilkjentYtelse = emptyList()
        )

        assertEquals(1, ytelsePersoner.size)
        assertEquals(barn1.personIdent.ident, ytelsePersoner.first().personIdent)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, ytelsePersoner.first().ytelseType)
        assertTrue(ytelsePersoner.first().erFramstiltKravForINåværendeBehandling())
    }

    @Test
    fun `Skal kun finne endringsytelsePersoner`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                       "2020-01",
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val ytelsePersoner = YtelsePersonUtils.utledKrav(
                søknadDTO = null,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1)
        )

        assertEquals(1, ytelsePersoner.size)
        assertEquals(barn1.personIdent.ident, ytelsePersoner.first().personIdent)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, ytelsePersoner.first().ytelseType)
        assertFalse(ytelsePersoner.first().erFramstiltKravForINåværendeBehandling())
    }

    @Test
    fun `Skal finne 2 endringsytelsePersoner på samme barn`() {
        val forrigeAndelBarn1Ordinær = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                              "2020-01",
                                                              YtelseType.ORDINÆR_BARNETRYGD,
                                                              1054,
                                                              person = barn1)

        val forrigeAndelBarn1Utvidet = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                              "2020-01",
                                                              YtelseType.UTVIDET_BARNETRYGD,
                                                              1054,
                                                              person = barn1)

        val ytelsePersoner = YtelsePersonUtils.utledKrav(
                søknadDTO = null,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1Ordinær, forrigeAndelBarn1Utvidet)
        )

        assertEquals(2, ytelsePersoner.size)
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && !it.erFramstiltKravForINåværendeBehandling() })
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erFramstiltKravForINåværendeBehandling() })
    }

    @Test
    fun `Skal finne 1 av 2 endringsytelsePersoner og 1 søknadsytelsePersoner`() {
        val søknadDTO = lagSøknadDTO(
                søkerIdent = søker.personIdent.ident,
                barnasIdenter = listOf(barn1.personIdent.ident)
        )

        val forrigeAndelBarn1Ordinær = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                              "2020-01",
                                                              YtelseType.ORDINÆR_BARNETRYGD,
                                                              1054,
                                                              person = barn1)

        val forrigeAndelBarn1Utvidet = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                              "2020-01",
                                                              YtelseType.UTVIDET_BARNETRYGD,
                                                              1054,
                                                              person = barn1)

        val ytelsePersoner = YtelsePersonUtils.utledKrav(
                søknadDTO = søknadDTO,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1Ordinær, forrigeAndelBarn1Utvidet)
        )

        assertEquals(2, ytelsePersoner.size)
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && it.erFramstiltKravForINåværendeBehandling() })
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erFramstiltKravForINåværendeBehandling() })
    }
}