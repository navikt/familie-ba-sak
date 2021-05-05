package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.forrigeMåned
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
    val barn2 = tilfeldigPerson()

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


    // Tester for ytelse person resultater

    @Test
    fun `Skal utelede INNVILGET første gang krav for et barn fremstilles med løpende periode`() {
        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                inneværendeMåned().plusYears(2).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.SØKNAD,
                ),
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = emptyList(),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   andelBarn1)
        )

        assertEquals(1, ytelsePersonerMedResultat.size)
        assertEquals(setOf(YtelsePersonResultat.INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede INNVILGET på revurdering med nytt barn med løpende periode`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn2 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                inneværendeMåned().plusYears(1).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn2)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                ),
                YtelsePerson(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.SØKNAD,
                )
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1,
                                                                                                   andelBarn2)
        )

        assertEquals(2, ytelsePersonerMedResultat.size)
        assertEquals(emptySet<YtelsePersonResultat>(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(setOf(YtelsePersonResultat.INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede INNVILGET og OPPHØRT på revurdering med nytt barn med periode tilbake i tid (etterbetaling)`() {
        val periodeStartForRentOpphør =  inneværendeMåned().minusYears(1)
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn2 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                periodeStartForRentOpphør.toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn2)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                ),
                YtelsePerson(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.SØKNAD,
                )
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1,
                                                                                                   andelBarn2)
        )

        assertEquals(2, ytelsePersonerMedResultat.size)
        assertEquals(emptySet<YtelsePersonResultat>(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
        assertEquals(null,
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.periodeStartForRentOpphør)
        assertEquals(periodeStartForRentOpphør.plusMonths(1),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.periodeStartForRentOpphør)
    }

    @Test
    fun `Skal utelede AVSLÅTT første gang krav for barn fremstilles`() {
        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.SØKNAD,
                ),
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = emptyList(),
                                                                                           andelerTilkjentYtelse = emptyList()
        )

        assertEquals(setOf(YtelsePersonResultat.AVSLÅTT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede AVSLÅTT på søknad for nytt barn i revurdering`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                ),
                YtelsePerson(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.SØKNAD,
                )
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1)
        )

        assertEquals(emptySet<YtelsePersonResultat>(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(setOf(YtelsePersonResultat.AVSLÅTT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utelede INNVILGET, AVSLÅTT og ENDRET ved revurdering med utvidet innvilgelse og eksplisitt avslag`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(1).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(2).toString(),
                                                inneværendeMåned().plusMonths(12).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.SØKNAD_OG_TIDLIGERE,
                        resultater = setOf(YtelsePersonResultat.AVSLÅTT)
                ),
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   andelBarn1)
        )

        assertEquals(setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.AVSLÅTT, YtelsePersonResultat.ENDRET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede OPPHØRT på revurdering hvor alle andeler er annulert`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                )
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1),
                                                                                           andelerTilkjentYtelse = listOf()
        )

        assertEquals(setOf(YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal IKKE utlede noen nye resultater (fortsatt innvilget) på årlig kontroll uten endringer`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                ),
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1)
        )

        assertEquals(emptySet<YtelsePersonResultat>(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede ENDRET på årlig kontroll med ny løpende periode tilbake i tid`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                inneværendeMåned().minusMonths(12).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val andel2Barn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusMonths(10).toString(),
                                                 inneværendeMåned().plusMonths(12).toString(),
                                                 YtelseType.ORDINÆR_BARNETRYGD,
                                                 1054,
                                                 person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                ),
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   andelBarn1,
                                                                                                   andel2Barn1)
        )

        assertEquals(setOf(YtelsePersonResultat.ENDRET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede ENDRET og OPPHØRT på årlig kontroll med ny opphørt periode tilbake i tid`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                inneværendeMåned().minusMonths(12).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val andel2Barn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusMonths(10).toString(),
                                                 inneværendeMåned().forrigeMåned().toString(),
                                                 YtelseType.ORDINÆR_BARNETRYGD,
                                                 1054,
                                                 person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                ),
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   andelBarn1,
                                                                                                   andel2Barn1)
        )

        assertEquals(setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(null,
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.periodeStartForRentOpphør)
    }

    @Test
    fun `Skal utelede INNVILGET og OPPHØRT på søknad for allerede opphørt barn i revurdering`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       "2019-01",
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                "2020-01",
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.SØKNAD,
                ),
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   andelBarn1)
        )

        assertEquals(setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }


    @Test
    fun `Skal utlede INNVILGET på søknad for nytt barn i revurdering`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn2 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(2).toString(),
                                                inneværendeMåned().plusMonths(12).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn2)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                ),
                YtelsePerson(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.SØKNAD,
                )
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1,
                                                                                                   andelBarn2)
        )

        assertEquals(emptySet<YtelsePersonResultat>(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)

        assertEquals(setOf(YtelsePersonResultat.INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
    }


    @Test
    fun `Skal utlede OPPHØRT og sette korrekt periodeStartForRentOpphør for barn i revurdering med forkortet tom`() {
        val periodeStartForRentOpphør = inneværendeMåned()
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                periodeStartForRentOpphør.toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                ),
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndelBarn1),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   andelBarn1)
        )

        assertEquals(setOf(YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(periodeStartForRentOpphør.plusMonths(1),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.periodeStartForRentOpphør)
    }


    @Test
    fun `Skal utlede OPPHØRT og sette korrekt periodeStartForRentOpphør for barn i revurdering med fjernet periode på slutten`() {
        val periodeStartForRentOpphør = inneværendeMåned()
        val forrigeAndel1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                   periodeStartForRentOpphør.toString(),
                                                   YtelseType.ORDINÆR_BARNETRYGD,
                                                   1354,
                                                   person = barn1)
        val forrigeAndel2 = lagAndelTilkjentYtelse(inneværendeMåned().plusMonths(1).toString(),
                                                   inneværendeMåned().plusMonths(5).toString(),
                                                   YtelseType.ORDINÆR_BARNETRYGD,
                                                   1054,
                                                   person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                ),
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndel1, forrigeAndel2),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndel1)
        )

        assertEquals(setOf(YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(periodeStartForRentOpphør.plusMonths(1),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.periodeStartForRentOpphør)
    }

    @Test
    fun `Skal utlede OPPHØRT og sette korrekt periodeStartForRentOpphør for barn i revurdering med forkortet tom og fjernet periode på slutten`() {
        val periodeStartForRentOpphør = inneværendeMåned().minusMonths(1)
        val forrigeAndel1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                   inneværendeMåned().toString(),
                                                   YtelseType.ORDINÆR_BARNETRYGD,
                                                   1354,
                                                   person = barn1)
        val forrigeAndel2 = lagAndelTilkjentYtelse(inneværendeMåned().plusMonths(1).toString(),
                                                   inneværendeMåned().plusMonths(5).toString(),
                                                   YtelseType.ORDINÆR_BARNETRYGD,
                                                   1054,
                                                   person = barn1)


        val oppdatertAndel = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                    periodeStartForRentOpphør.toString(),
                                                    YtelseType.ORDINÆR_BARNETRYGD,
                                                    1354,
                                                    person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                ),
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndel1, forrigeAndel2),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   oppdatertAndel)
        )

        assertEquals(setOf(YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(periodeStartForRentOpphør.plusMonths(1),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.periodeStartForRentOpphør)
    }

    @Test
    fun `Skal utlede OPPHØRT og ENDRET og sette korrekt periodeStartForRentOpphør for barn med utvidet opphør`() {
        val periodeStartForRentOpphør = inneværendeMåned().minusYears(1)
        val forrigeAndel = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                  inneværendeMåned().minusYears(2).toString(),
                                                  YtelseType.ORDINÆR_BARNETRYGD,
                                                  1054,
                                                  person = barn1)


        val oppdatertAndel = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                    periodeStartForRentOpphør.toString(),
                                                    YtelseType.ORDINÆR_BARNETRYGD,
                                                    1054,
                                                    person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                ),
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.populerYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                                   forrigeAndel),
                                                                                           andelerTilkjentYtelse = listOf(
                                                                                                   oppdatertAndel)
        )

        assertEquals(setOf(YtelsePersonResultat.OPPHØRT, YtelsePersonResultat.ENDRET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(periodeStartForRentOpphør.plusMonths(1),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.periodeStartForRentOpphør)
    }
}