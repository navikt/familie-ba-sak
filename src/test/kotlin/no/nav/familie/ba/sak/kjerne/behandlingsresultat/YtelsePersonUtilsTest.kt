package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YtelsePersonUtilsTest {

    val søker = tilfeldigPerson(personType = PersonType.SØKER)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()

    @Test
    fun `Skal kun finne personer framstilt krav for`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                personIdent = barn1.personIdent.ident,
                søktForPerson = true,
                personType = barn1.type,
                forrigeAndeler = emptyList(),
                andeler = emptyList()
            )
        ).map { it.utledYtelsePerson() }

        assertEquals(1, ytelsePersoner.size)
        assertEquals(barn1.personIdent.ident, ytelsePersoner.first().personIdent)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, ytelsePersoner.first().ytelseType)
        assertTrue(ytelsePersoner.first().erFramstiltKravForIInneværendeBehandling())
    }

    @Test
    fun `Skal kun finne endringsytelsePersoner`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                personIdent = barn1.personIdent.ident,
                søktForPerson = false,
                personType = barn1.type,
                forrigeAndeler = listOf(
                    lagBehandlingsresultatAndelTilkjentYtelse(
                        fom = inneværendeMåned().minusYears(3).toString(),
                        tom = "2020-01",
                        kalkulertUtbetalingsbeløp = 1054,
                    )
                ),
                andeler = emptyList()
            )
        ).map { it.utledYtelsePerson() }

        assertEquals(1, ytelsePersoner.size)
        assertEquals(barn1.personIdent.ident, ytelsePersoner.first().personIdent)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, ytelsePersoner.first().ytelseType)
        assertFalse(ytelsePersoner.first().erFramstiltKravForIInneværendeBehandling())
    }

    @Test
    fun `Skal finne 2 ytelsePersoner ved utvidet`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                personIdent = barn1.personIdent.ident,
                søktForPerson = false,
                personType = barn1.type,
                forrigeAndeler = listOf(
                    lagBehandlingsresultatAndelTilkjentYtelse(
                        inneværendeMåned().minusYears(3).toString(),
                        inneværendeMåned().minusYears(1).toString(),
                        1054
                    )
                ),
                andeler = emptyList()
            ),
            BehandlingsresultatPerson(
                personIdent = søker.personIdent.ident,
                personType = søker.type,
                søktForPerson = false,
                forrigeAndeler = listOf(
                    lagBehandlingsresultatAndelTilkjentYtelse(
                        inneværendeMåned().minusYears(3)
                            .toString(),
                        inneværendeMåned().minusYears(1).toString(),
                        1054
                    )
                ),
                andeler = emptyList()
            )
        ).map { it.utledYtelsePerson() }

        assertEquals(2, ytelsePersoner.size)
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && !it.erFramstiltKravForIInneværendeBehandling() })
        assertTrue(ytelsePersoner.any { it.personIdent == søker.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erFramstiltKravForIInneværendeBehandling() })
    }

    @Test
    fun `Skal finne 1 av 2 endringsytelsePersoner og 1 søknadsytelsePersoner`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                personIdent = barn1.personIdent.ident,
                søktForPerson = true,
                personType = barn1.type,
                forrigeAndeler = listOf(
                    lagBehandlingsresultatAndelTilkjentYtelse(
                        inneværendeMåned().minusYears(3).toString(),
                        "2020-01",
                        1054
                    )
                ),
                andeler = emptyList()
            ),
            BehandlingsresultatPerson(
                personIdent = søker.personIdent.ident,
                personType = søker.type,
                søktForPerson = false,
                forrigeAndeler = listOf(
                    lagBehandlingsresultatAndelTilkjentYtelse(
                        inneværendeMåned().minusYears(3).toString(),
                        "2020-01",
                        1054
                    )
                ),
                andeler = emptyList()
            )
        ).map { it.utledYtelsePerson() }

        assertEquals(2, ytelsePersoner.size)
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && it.erFramstiltKravForIInneværendeBehandling() })
        assertTrue(ytelsePersoner.any { it.personIdent == søker.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erFramstiltKravForIInneværendeBehandling() })
    }

    @Test
    fun `Skal utlede krav for person som ikke finnes i søknad, men har andeler fra tidligere`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                personIdent = barn2.personIdent.ident,
                søktForPerson = true,
                personType = barn2.type,
                forrigeAndeler = emptyList(),
                andeler = emptyList()
            ),
            BehandlingsresultatPerson(
                personIdent = barn1.personIdent.ident,
                søktForPerson = false,
                personType = barn1.type,
                forrigeAndeler = listOf(
                    lagBehandlingsresultatAndelTilkjentYtelse(
                        inneværendeMåned().minusYears(3).toString(),
                        "2020-01",
                        1054
                    )
                ),
                andeler = emptyList()
            )
        ).map { it.utledYtelsePerson() }

        assertEquals(2, ytelsePersoner.size)
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.kravOpprinnelse == listOf(KravOpprinnelse.TIDLIGERE) && !it.erFramstiltKravForIInneværendeBehandling() })
        assertTrue(ytelsePersoner.any { it.personIdent == barn2.personIdent.ident && it.kravOpprinnelse == listOf(KravOpprinnelse.INNEVÆRENDE) && it.erFramstiltKravForIInneværendeBehandling() })
    }
}
