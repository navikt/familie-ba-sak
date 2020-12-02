package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.tilfeldigPerson
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BehandlingsresultatUtilsTest {

    @Test
    fun `Skal kun finne søknadskrav`() {
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søknadDTO = lagSøknadDTO(
                søkerIdent = søker.personIdent.ident,
                barnasIdenter = listOf(barn1.personIdent.ident)
        )

        val krav = BehandlingsresultatUtil.utledKrav(
                søknadDTO = søknadDTO,
                forrigeAndelerTilkjentYtelse = emptyList()
        )

        assertEquals(1, krav.size)
        assertEquals(barn1.personIdent.ident, krav.first().personIdent)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, krav.first().ytelseType)
        assertTrue(krav.first().søknadskrav)
    }

    @Test
    fun `Skal kun finne endringskrav`() {
        val barn1 = tilfeldigPerson()

        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                       "2020-01",
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val krav = BehandlingsresultatUtil.utledKrav(
                søknadDTO = null,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1)
        )

        assertEquals(1, krav.size)
        assertEquals(barn1.personIdent.ident, krav.first().personIdent)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, krav.first().ytelseType)
        assertFalse(krav.first().søknadskrav)
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

        val krav = BehandlingsresultatUtil.utledKrav(
                søknadDTO = null,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1Ordinær, forrigeAndelBarn1Utvidet)
        )

        assertEquals(2, krav.size)
        assertTrue(krav.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && !it.søknadskrav })
        assertTrue(krav.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.søknadskrav })
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

        val krav = BehandlingsresultatUtil.utledKrav(
                søknadDTO = søknadDTO,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1Ordinær, forrigeAndelBarn1Utvidet)
        )

        assertEquals(2, krav.size)
        assertTrue(krav.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && it.søknadskrav })
        assertTrue(krav.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.søknadskrav })
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
                        søknadskrav = true
                ),
        )

        val kravMedResultat = BehandlingsresultatUtil.utledKravMedResultat(krav = krav,
                                                                           forrigeAndelerTilkjentYtelse = emptyList(),
                                                                           andelerTilkjentYtelse = listOf(andelBarn1)
        )

        assertEquals(BehandlingResultatType.INNVILGET,
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatType)
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
                        søknadskrav = true
                ),
        )

        val kravMedResultat = BehandlingsresultatUtil.utledKravMedResultat(krav = krav,
                                                                           forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1),
                                                                           andelerTilkjentYtelse = listOf(andelBarn1)
        )

        assertEquals(BehandlingResultatType.INNVILGET,
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatType)
    }

    @Test
    fun `Skal utelede avslag første gang kravet for barn fremstilles`() {
        val barn1 = tilfeldigPerson()

        val krav = listOf(
                Krav(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        søknadskrav = true
                ),
        )

        val kravMedResultat = BehandlingsresultatUtil.utledKravMedResultat(krav = krav,
                                                                           forrigeAndelerTilkjentYtelse = emptyList(),
                                                                           andelerTilkjentYtelse = emptyList()
        )

        assertEquals(BehandlingResultatType.AVSLÅTT,
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatType)
    }

    @Test
    fun `Skal utlede innvilget på søknad for nytt barn i revurdering`() {
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()

        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       "2019-01",
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn2 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(2).toString(),
                                                "2020-01",
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn2)

        val krav = listOf(
                Krav(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        søknadskrav = false
                ),
                Krav(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        søknadskrav = true
                )
        )

        val kravMedResultat = BehandlingsresultatUtil.utledKravMedResultat(krav = krav,
                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                   forrigeAndelBarn1),
                                                                           andelerTilkjentYtelse = listOf(forrigeAndelBarn1,
                                                                                                          andelBarn2)
        )

        assertEquals(BehandlingResultatType.INGEN_ENDRING,
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatType)
        assertEquals(BehandlingResultatType.INNVILGET,
                     kravMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultatType)
    }

    @Test
    fun `Skal utlede avslag på søknad for nytt barn i revurdering`() {
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()

        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       "2019-01",
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
                        søknadskrav = false
                ),
                Krav(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        søknadskrav = true
                )
        )

        val kravMedResultat = BehandlingsresultatUtil.utledKravMedResultat(krav = krav,
                                                                           forrigeAndelerTilkjentYtelse = listOf(
                                                                                   forrigeAndelBarn1, forrigeAndelBarn2),
                                                                           andelerTilkjentYtelse = listOf(forrigeAndelBarn1,
                                                                                                          forrigeAndelBarn2)
        )

        assertEquals(BehandlingResultatType.INGEN_ENDRING,
                     kravMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultatType)
        assertEquals(BehandlingResultatType.AVSLÅTT,
                     kravMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultatType)
    }
}