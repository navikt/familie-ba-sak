package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BehandlingsresultatUtilssTest {

    @Test
    fun `Skal kun finne søknadsytelsePersoner`() {
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søknadDTO = lagSøknadDTO(
                søkerIdent = søker.personIdent.ident,
                barnasIdenter = listOf(barn1.personIdent.ident)
        )

        val ytelsePersoner = BehandlingsresultatUtils.utledKrav(
                søknadDTO = søknadDTO,
                forrigeAndelerTilkjentYtelse = emptyList()
        )

        assertEquals(1, ytelsePersoner.size)
        assertEquals(barn1.personIdent.ident, ytelsePersoner.first().personIdent)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, ytelsePersoner.first().ytelseType)
        assertTrue(ytelsePersoner.first().erSøktOmINåværendeBehandling)
    }

    @Test
    fun `Skal kun finne endringsytelsePersoner`() {
        val barn1 = tilfeldigPerson()

        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                       "2020-01",
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val ytelsePersoner = BehandlingsresultatUtils.utledKrav(
                søknadDTO = null,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1)
        )

        assertEquals(1, ytelsePersoner.size)
        assertEquals(barn1.personIdent.ident, ytelsePersoner.first().personIdent)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, ytelsePersoner.first().ytelseType)
        assertFalse(ytelsePersoner.first().erSøktOmINåværendeBehandling)
    }

    @Test
    fun `Skal finne 2 endringsytelsePersoner på samme barn`() {
        val barn1 = tilfeldigPerson()

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

        val ytelsePersoner = BehandlingsresultatUtils.utledKrav(
                søknadDTO = null,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1Ordinær, forrigeAndelBarn1Utvidet)
        )

        assertEquals(2, ytelsePersoner.size)
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && !it.erSøktOmINåværendeBehandling })
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erSøktOmINåværendeBehandling })
    }

    @Test
    fun `Skal finne 1 av 2 endringsytelsePersoner og 1 søknadsytelsePersoner`() {
        val barn1 = tilfeldigPerson()
        val søker = tilfeldigPerson()
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

        val ytelsePersoner = BehandlingsresultatUtils.utledKrav(
                søknadDTO = søknadDTO,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1Ordinær, forrigeAndelBarn1Utvidet)
        )

        assertEquals(2, ytelsePersoner.size)
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && it.erSøktOmINåværendeBehandling })
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erSøktOmINåværendeBehandling })
    }


    @Test
    fun `Skal utelede innvilget første gang ytelsePersoneret for barn fremstilles`() {
        val barn1 = tilfeldigPerson()

        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                "2020-01",
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøktOmINåværendeBehandling = true
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = emptyList(),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1)
        )

        assertEquals(1, ytelsePersonerMedResultat.size)
        assertEquals(listOf(BehandlingResultatType.INNVILGET, BehandlingResultatType.OPPHØRT).sorted(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper?.sorted())
    }

    @Test
    fun `Skal utlede innvilget første gang ytelsePersoneret for barn nr2 fremstilles i en revurdering`() {
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()

        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                inneværendeMåned().minusMonths(1).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val andelBarn2 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                inneværendeMåned().minusMonths(1).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn2)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøktOmINåværendeBehandling = false
                ),
                YtelsePerson(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøktOmINåværendeBehandling = true
                )
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1,
                                                                                                        andelBarn2)
        )

        assertEquals(2, ytelsePersonerMedResultat.size)
        assertEquals(listOf(BehandlingResultatType.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)

        assertEquals(listOf(BehandlingResultatType.INNVILGET, BehandlingResultatType.OPPHØRT).sorted(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultatTyper?.sorted())
    }

    @Test
    fun `Skal utelede avslag første gang ytelsePersoneret for barn fremstilles`() {
        val barn1 = tilfeldigPerson()

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøktOmINåværendeBehandling = true
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = emptyList(),
                                                                                                andelerTilkjentYtelse = emptyList()
        )

        assertEquals(listOf(BehandlingResultatType.AVSLÅTT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)
    }

    @Test
    fun `Skal utlede avslag på søknad for nytt barn i revurdering`() {
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()

        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val forrigeAndelBarn2 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       "2019-01",
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn2)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøktOmINåværendeBehandling = false
                ),
                YtelsePerson(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøktOmINåværendeBehandling = true
                )
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1,
                                                                                                        forrigeAndelBarn2),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1,
                                                                                                        forrigeAndelBarn2)
        )

        assertEquals(listOf(BehandlingResultatType.FORTSATT_INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)
        assertEquals(listOf(BehandlingResultatType.AVSLÅTT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultatTyper)
    }

    @Test
    fun `Skal utlede fortsatt innvilget på årlig kontroll uten endringer`() {
        val barn1 = tilfeldigPerson()

        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøktOmINåværendeBehandling = false
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1)
        )

        assertEquals(listOf(BehandlingResultatType.FORTSATT_INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)
    }

    @Test
    fun `Skal utlede endring på årlig kontroll med liten endring tilbake i tid`() {
        val barn1 = tilfeldigPerson()

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
                        erSøktOmINåværendeBehandling = false
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1,
                                                                                                        andel2Barn1)
        )

        assertEquals(listOf(BehandlingResultatType.ENDRING, BehandlingResultatType.FORTSATT_INNVILGET).sorted(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper?.sorted())
    }

    @Test
    fun `Skal utlede endring og opphør på årlig kontroll med liten endring tilbake i tid og opphør`() {
        val barn1 = tilfeldigPerson()

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
                        erSøktOmINåværendeBehandling = false
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1,
                                                                                                        andel2Barn1)
        )

        assertEquals(listOf(BehandlingResultatType.ENDRING, BehandlingResultatType.OPPHØRT).sorted(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper?.sorted())
    }

    @Test
    fun `Skal utelede innvilget andre gang ytelsePersoneret for barn fremstilles`() {
        val barn1 = tilfeldigPerson()

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
                        erSøktOmINåværendeBehandling = true
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1)
        )

        assertEquals(listOf(BehandlingResultatType.INNVILGET, BehandlingResultatType.OPPHØRT).sorted(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper?.sorted())
    }


    @Test
    fun `Skal utlede innvilget på søknad for nytt barn i revurdering`() {
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()

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
                        erSøktOmINåværendeBehandling = false
                ),
                YtelsePerson(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøktOmINåværendeBehandling = true
                )
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1,
                                                                                                        andelBarn2)
        )

        assertEquals(listOf(BehandlingResultatType.FORTSATT_INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)

        assertEquals(listOf(BehandlingResultatType.INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultatTyper)
    }


    @Test
    fun `Skal utlede opphør på endring for barn i revurdering`() {
        val barn1 = tilfeldigPerson()

        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        // Opphør ytelsen for barnet 1 måned tilbake i tid
        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                inneværendeMåned().forrigeMåned().toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøktOmINåværendeBehandling = false
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1)
        )

        assertEquals(listOf(BehandlingResultatType.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)
    }
}