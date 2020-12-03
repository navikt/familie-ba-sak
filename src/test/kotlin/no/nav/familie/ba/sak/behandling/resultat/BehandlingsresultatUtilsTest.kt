package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.resultat.BehandlingsresultatUtils
import no.nav.familie.ba.sak.behandling.resultat.Krav
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BehandlingsresultatUtilssTest {

    @Test
    fun `Skal kun finne søknadskrav`() {
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søknadDTO = lagSøknadDTO(
                søkerIdent = søker.personIdent.ident,
                barnasIdenter = listOf(barn1.personIdent.ident)
        )

        val krav = BehandlingsresultatUtils.utledKrav(
                søknadDTO = søknadDTO,
                forrigeAndelerTilkjentYtelse = emptyList()
        )

        assertEquals(1, krav.size)
        assertEquals(barn1.personIdent.ident, krav.first().personIdent)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, krav.first().ytelseType)
        assertTrue(krav.first().erSøknadskrav)
    }

    @Test
    fun `Skal kun finne endringskrav`() {
        val barn1 = tilfeldigPerson()

        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                       "2020-01",
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val krav = BehandlingsresultatUtils.utledKrav(
                søknadDTO = null,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1)
        )

        assertEquals(1, krav.size)
        assertEquals(barn1.personIdent.ident, krav.first().personIdent)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, krav.first().ytelseType)
        assertFalse(krav.first().erSøknadskrav)
    }

    @Test
    fun `Skal finne 2 endringskrav på samme barn`() {
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

        val krav = BehandlingsresultatUtils.utledKrav(
                søknadDTO = null,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1Ordinær, forrigeAndelBarn1Utvidet)
        )

        assertEquals(2, krav.size)
        assertTrue(krav.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && !it.erSøknadskrav })
        assertTrue(krav.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erSøknadskrav })
    }

    @Test
    fun `Skal finne 1 av 2 endringskrav og 1 søknadskrav`() {
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

        val krav = BehandlingsresultatUtils.utledKrav(
                søknadDTO = søknadDTO,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1Ordinær, forrigeAndelBarn1Utvidet)
        )

        assertEquals(2, krav.size)
        assertTrue(krav.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && it.erSøknadskrav })
        assertTrue(krav.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erSøknadskrav })
    }


    @Test
    fun `Skal utelede innvilget første gang kravet for barn fremstilles`() {
        val barn1 = tilfeldigPerson()

        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                "2020-01",
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val krav = listOf(
                Krav(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøknadskrav = true
                ),
        )

        val kravMedResultat = BehandlingsresultatUtils.utledKravMedResultat(krav = krav,
                                                                            forrigeAndelerTilkjentYtelse = emptyList(),
                                                                            andelerTilkjentYtelse = listOf(andelBarn1)
        )

        assertEquals(1, kravMedResultat.size)
        assertEquals(listOf(BehandlingResultatType.INNVILGET, BehandlingResultatType.OPPHØRT).sorted(),
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper?.sorted())
    }

    @Test
    fun `Skal utlede innvilget første gang kravet for barn nr2 fremstilles i en revurdering`() {
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

        val krav = listOf(
                Krav(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøknadskrav = false
                ),
                Krav(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøknadskrav = true
                )
        )

        val kravMedResultat = BehandlingsresultatUtils.utledKravMedResultat(krav = krav,
                                                                            forrigeAndelerTilkjentYtelse = listOf(
                                                                                    forrigeAndelBarn1),
                                                                            andelerTilkjentYtelse = listOf(andelBarn1,
                                                                                                           andelBarn2)
        )

        assertEquals(2, kravMedResultat.size)
        assertEquals(listOf(BehandlingResultatType.OPPHØRT),
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)

        assertEquals(listOf(BehandlingResultatType.INNVILGET, BehandlingResultatType.OPPHØRT).sorted(),
                     kravMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultatTyper?.sorted())
    }

    @Test
    fun `Skal utelede avslag første gang kravet for barn fremstilles`() {
        val barn1 = tilfeldigPerson()

        val krav = listOf(
                Krav(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøknadskrav = true
                ),
        )

        val kravMedResultat = BehandlingsresultatUtils.utledKravMedResultat(krav = krav,
                                                                            forrigeAndelerTilkjentYtelse = emptyList(),
                                                                            andelerTilkjentYtelse = emptyList()
        )

        assertEquals(listOf(BehandlingResultatType.AVSLÅTT),
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)
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

        val krav = listOf(
                Krav(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøknadskrav = false
                ),
                Krav(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøknadskrav = true
                )
        )

        val kravMedResultat = BehandlingsresultatUtils.utledKravMedResultat(krav = krav,
                                                                            forrigeAndelerTilkjentYtelse = listOf(
                                                                                    forrigeAndelBarn1, forrigeAndelBarn2),
                                                                            andelerTilkjentYtelse = listOf(forrigeAndelBarn1,
                                                                                                           forrigeAndelBarn2)
        )

        assertEquals(listOf(BehandlingResultatType.FORTSATT_INNVILGET),
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)
        assertEquals(listOf(BehandlingResultatType.AVSLÅTT),
                     kravMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultatTyper)
    }

    @Test
    fun `Skal utlede fortsatt innvilget på årlig kontroll uten endringer`() {
        val barn1 = tilfeldigPerson()

        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val krav = listOf(
                Krav(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøknadskrav = false
                ),
        )

        val kravMedResultat = BehandlingsresultatUtils.utledKravMedResultat(krav = krav,
                                                                            forrigeAndelerTilkjentYtelse = listOf(
                                                                                    forrigeAndelBarn1),
                                                                            andelerTilkjentYtelse = listOf(forrigeAndelBarn1)
        )

        assertEquals(listOf(BehandlingResultatType.FORTSATT_INNVILGET),
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)
    }

    @Test
    fun `Skal utelede innvilget andre gang kravet for barn fremstilles`() {
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

        val krav = listOf(
                Krav(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøknadskrav = true
                ),
        )

        val kravMedResultat = BehandlingsresultatUtils.utledKravMedResultat(krav = krav,
                                                                            forrigeAndelerTilkjentYtelse = listOf(
                                                                                    forrigeAndelBarn1),
                                                                            andelerTilkjentYtelse = listOf(andelBarn1)
        )

        assertEquals(listOf(BehandlingResultatType.INNVILGET, BehandlingResultatType.OPPHØRT).sorted(),
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper?.sorted())
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

        val krav = listOf(
                Krav(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøknadskrav = false
                ),
                Krav(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøknadskrav = true
                )
        )

        val kravMedResultat = BehandlingsresultatUtils.utledKravMedResultat(krav = krav,
                                                                            forrigeAndelerTilkjentYtelse = listOf(
                                                                                    forrigeAndelBarn1),
                                                                            andelerTilkjentYtelse = listOf(forrigeAndelBarn1,
                                                                                                           andelBarn2)
        )

        assertEquals(listOf(BehandlingResultatType.FORTSATT_INNVILGET),
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)

        assertEquals(listOf(BehandlingResultatType.INNVILGET),
                     kravMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultatTyper)
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

        val krav = listOf(
                Krav(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erSøknadskrav = false
                ),
        )

        val kravMedResultat = BehandlingsresultatUtils.utledKravMedResultat(krav = krav,
                                                                            forrigeAndelerTilkjentYtelse = listOf(
                                                                                    forrigeAndelBarn1),
                                                                            andelerTilkjentYtelse = listOf(andelBarn1)
        )

        assertEquals(listOf(BehandlingResultatType.OPPHØRT),
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatTyper)
    }
}