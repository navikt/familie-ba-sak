package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.YtelsePersonUtils.erAndelMedEndretBeløp
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

class YtelsePersonUtilsTest {

    val søker = tilfeldigPerson(personType = PersonType.SØKER)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()

    @Test
    fun `Skal kun finne personer framstilt krav for`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                aktør = barn1.aktør,
                søktForPerson = true,
                personType = barn1.type,
                forrigeAndeler = emptyList(),
                andeler = emptyList()
            )
        ).map { it.utledYtelsePerson() }

        assertEquals(1, ytelsePersoner.size)
        assertEquals(barn1.aktør, ytelsePersoner.first().aktør)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, ytelsePersoner.first().ytelseType)
        assertTrue(ytelsePersoner.first().erFramstiltKravForIInneværendeBehandling())
    }

    @Test
    fun `Skal kun finne endringsytelsePersoner`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                aktør = barn1.aktør,
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
        assertEquals(barn1.aktør, ytelsePersoner.first().aktør)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, ytelsePersoner.first().ytelseType)
        assertFalse(ytelsePersoner.first().erFramstiltKravForIInneværendeBehandling())
    }

    @Test
    fun `Skal finne 2 ytelsePersoner ved utvidet`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                aktør = barn1.aktør,
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
                aktør = søker.aktør,
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
        assertTrue(ytelsePersoner.any { it.aktør == barn1.aktør && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && !it.erFramstiltKravForIInneværendeBehandling() })
        assertTrue(ytelsePersoner.any { it.aktør == søker.aktør && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erFramstiltKravForIInneværendeBehandling() })
    }

    @Test
    fun `Skal finne 1 av 2 endringsytelsePersoner og 1 søknadsytelsePersoner`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                aktør = barn1.aktør,
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
                aktør = søker.aktør,
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
        assertTrue(ytelsePersoner.any { it.aktør == barn1.aktør && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && it.erFramstiltKravForIInneværendeBehandling() })
        assertTrue(ytelsePersoner.any { it.aktør == søker.aktør && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erFramstiltKravForIInneværendeBehandling() })
    }

    @Test
    fun `Skal utlede krav for person som ikke finnes i søknad, men har andeler fra tidligere`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                aktør = barn2.aktør,
                søktForPerson = true,
                personType = barn2.type,
                forrigeAndeler = emptyList(),
                andeler = emptyList()
            ),
            BehandlingsresultatPerson(
                aktør = barn1.aktør,
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
        assertTrue(
            ytelsePersoner.any {
                it.aktør == barn1.aktør && it.kravOpprinnelse == listOf(
                    KravOpprinnelse.TIDLIGERE
                ) && !it.erFramstiltKravForIInneværendeBehandling()
            }
        )
        assertTrue(
            ytelsePersoner.any {
                it.aktør == barn2.aktør && it.kravOpprinnelse == listOf(
                    KravOpprinnelse.INNEVÆRENDE
                ) && it.erFramstiltKravForIInneværendeBehandling()
            }
        )
    }

    @Test
    fun `Skal gi false dersom det ikke er en andel som får beløpet sitt endret`() {
        val beløp1 = 1054
        val beløp2 = 527

        val måned1 = YearMonth.of(2020, 1)
        val måned2 = YearMonth.of(2020, 2)
        val måned3 = YearMonth.of(2020, 3)
        val måned4 = YearMonth.of(2020, 4)

        assertFalse(
            erAndelMedEndretBeløp(
                listOf(BehandlingsresultatAndelTilkjentYtelse(måned1, måned2, beløp1)),
                listOf(BehandlingsresultatAndelTilkjentYtelse(måned3, måned4, beløp2))
            )
        )

        assertFalse(
            erAndelMedEndretBeløp(
                listOf(BehandlingsresultatAndelTilkjentYtelse(måned1, måned2, beløp1)),
                listOf(BehandlingsresultatAndelTilkjentYtelse(måned1, måned3, beløp1))
            )
        )
    }

    @Test
    fun `Skal gi true dersom det er en andel som får beløpet sitt endret`() {
        val beløp1 = 1054
        val beløp2 = 527

        val måned1 = YearMonth.of(2020, 1)
        val måned2 = YearMonth.of(2020, 2)
        val måned3 = YearMonth.of(2020, 3)
        val måned4 = YearMonth.of(2020, 4)

        assertTrue(
            erAndelMedEndretBeløp(
                listOf(BehandlingsresultatAndelTilkjentYtelse(måned1, måned2, beløp1)),
                listOf(BehandlingsresultatAndelTilkjentYtelse(måned1, måned2, beløp2))
            )
        )

        assertTrue(
            erAndelMedEndretBeløp(
                listOf(BehandlingsresultatAndelTilkjentYtelse(måned1, måned4, beløp1)),
                listOf(BehandlingsresultatAndelTilkjentYtelse(måned2, måned3, beløp2))
            )
        )

        assertTrue(
            erAndelMedEndretBeløp(
                listOf(BehandlingsresultatAndelTilkjentYtelse(måned2, måned3, beløp1)),
                listOf(BehandlingsresultatAndelTilkjentYtelse(måned1, måned4, beløp2))
            )
        )
    }

    @Test
    fun `skal gi opphør når forrigebehandling,nybehandling har samme tidslinje og er opphørt med brukerens dødsfall`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                aktør = barn1.aktør,
                søktForPerson = false,
                personType = barn1.type,
                forrigeAndeler = listOf(
                    lagBehandlingsresultatAndelTilkjentYtelse(
                        inneværendeMåned().minusYears(3).toString(),
                        inneværendeMåned().forrigeMåned().toString(),
                        1054
                    )
                ),
                andeler = listOf(
                    lagBehandlingsresultatAndelTilkjentYtelse(
                        inneværendeMåned().minusYears(3).toString(),
                        inneværendeMåned().forrigeMåned().toString(),
                        1054
                    )
                )
            )
        )
        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(ytelsePersoner)

        assertTrue { ytelsePersonerMedResultat.any { it.resultater.contains(YtelsePersonResultat.FORTSATT_OPPHØRT) } }
    }

    @Test
    fun `skal gi opphør når forrigebehandling,nybehandling har opphørt med brukerens dødsfall`() {
        val ytelsePersoner = listOf(
            BehandlingsresultatPerson(
                aktør = barn1.aktør,
                søktForPerson = false,
                personType = barn1.type,
                forrigeAndeler = listOf(
                    lagBehandlingsresultatAndelTilkjentYtelse(
                        inneværendeMåned().minusYears(3).toString(),
                        inneværendeMåned().plusYears(15).toString(),
                        1054
                    )
                ),
                andeler = listOf(
                    lagBehandlingsresultatAndelTilkjentYtelse(
                        inneværendeMåned().minusYears(3).toString(),
                        inneværendeMåned().forrigeMåned().toString(),
                        1054
                    )
                )
            )
        )
        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(ytelsePersoner)

        assertTrue { ytelsePersonerMedResultat.any { it.resultater.contains(YtelsePersonResultat.OPPHØRT) } }
    }
}
