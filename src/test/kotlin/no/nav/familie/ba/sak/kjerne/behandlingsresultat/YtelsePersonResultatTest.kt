package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YtelsePersonResultatTest {

    val søker = tilfeldigPerson()
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()

    /**
     * INNVILGET
     */

    @Test
    fun `Skal utelede INNVILGET første gang krav for et barn fremstilles med løpende periode`() {
        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = true,
                        personType = barn1.type,
                        forrigeAndeler = emptyList(),
                        andeler = listOf(
                                lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                                          inneværendeMåned().plusYears(2).toString(),
                                                                          1054
                                )
                        )
                )
        ))

        assertEquals(1, ytelsePersonerMedResultat.size)
        assertEquals(setOf(YtelsePersonResultat.INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede INNVILGET på revurdering med nytt barn med løpende periode`() {
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                                          inneværendeMåned().plusMonths(12).toString(),
                                                                          1054
        )

        val andelBarn2 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                                   inneværendeMåned().plusYears(1).toString(),
                                                                   1054
        )


        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(forrigeAndelBarn1)
                ),
                BehandlingsresultatPerson(
                        personIdent = barn2.personIdent.ident,
                        søktForPerson = true,
                        personType = barn2.type,
                        forrigeAndeler = emptyList(),
                        andeler = listOf(andelBarn2)
                )
        ))

        assertEquals(2, ytelsePersonerMedResultat.size)
        assertEquals(emptySet<YtelsePersonResultat>(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(setOf(YtelsePersonResultat.INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede INNVILGET på søknad for nytt barn i revurdering`() {
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                          inneværendeMåned().plusMonths(12).toString(),
                                                                          1054
        )

        val andelBarn2 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(2).toString(),
                                                                   inneværendeMåned().plusMonths(12).toString(),
                                                                   1054
        )


        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(forrigeAndelBarn1)
                ),
                BehandlingsresultatPerson(
                        personIdent = barn2.personIdent.ident,
                        søktForPerson = true,
                        personType = barn2.type,
                        forrigeAndeler = emptyList(),
                        andeler = listOf(andelBarn2)
                )
        ))

        assertEquals(emptySet<YtelsePersonResultat>(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)

        assertEquals(setOf(YtelsePersonResultat.INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
    }


    /**
     * INNVILGET, OPPHØRT
     */
    @Test
    fun `Skal utlede INNVILGET og OPPHØRT på revurdering med nytt barn med periode tilbake i tid (etterbetaling)`() {
        val ytelseFørsteBarn = inneværendeMåned().plusMonths(12)
        val ytelseSluttNyttBarn = inneværendeMåned().minusYears(1)
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                                          ytelseFørsteBarn.toString(),
                                                                          1054
        )

        val andelBarn2 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                                   ytelseSluttNyttBarn.toString(),
                                                                   1054
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(forrigeAndelBarn1)
                ),
                BehandlingsresultatPerson(
                        personIdent = barn2.personIdent.ident,
                        søktForPerson = true,
                        personType = barn2.type,
                        forrigeAndeler = emptyList(),
                        andeler = listOf(andelBarn2)
                )
        ))

        assertEquals(2, ytelsePersonerMedResultat.size)
        assertEquals(emptySet<YtelsePersonResultat>(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
        assertEquals(ytelseFørsteBarn,
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.ytelseSlutt)
        assertEquals(ytelseSluttNyttBarn,
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.ytelseSlutt)
    }

    @Test
    fun `Skal utelede INNVILGET og OPPHØRT på søknad for allerede opphørt barn i revurdering`() {
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                          inneværendeMåned().minusYears(2).toString(),
                                                                          1054
        )

        val andelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                   inneværendeMåned().minusYears(1).toString(),
                                                                   1054
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = true,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(andelBarn1)
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    /**
     * INNVILGET, AVSLÅTT, ENDRET
     */
    @Test
    fun `Skal utelede INNVILGET, AVSLÅTT og ENDRET ved revurdering med utvidet innvilgelse, reduksjon tilbake i tid og eksplisitt avslag`() {
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(1).toString(),
                                                                          inneværendeMåned().plusMonths(12).toString(),
                                                                          1054
        )

        val andelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(2).toString(),
                                                                   inneværendeMåned().minusMonths(6).toString(),
                                                                   1054
        )

        val andel2Barn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusMonths(4).toString(),
                                                                    inneværendeMåned().plusMonths(12).toString(),
                                                                    1054
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = true,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(andelBarn1, andel2Barn1),
                        eksplisittAvslag = true
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.AVSLÅTT, YtelsePersonResultat.ENDRET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    /**
     * INNVILGET, AVSLÅTT
     */
    @Test
    fun `Skal utelede INNVILGET og AVSLÅTT ved revurdering med utvidet innvilgelse og eksplisitt avslag`() {
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(1).toString(),
                                                                          inneværendeMåned().plusMonths(12).toString(),
                                                                          1054
        )

        val andelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(2).toString(),
                                                                   inneværendeMåned().plusMonths(12).toString(),
                                                                   1054
        )

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = true,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(andelBarn1),
                        eksplisittAvslag = true
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.AVSLÅTT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    /**
     * AVSLÅTT
     */
    @Test
    fun `Skal utelede AVSLÅTT første gang krav for barn fremstilles`() {
        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = true,
                        personType = barn1.type,
                        forrigeAndeler = emptyList(),
                        andeler = emptyList(),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.AVSLÅTT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede AVSLÅTT og OPPHØRT på søknad for nytt barn i revurdering`() {
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                          inneværendeMåned().plusMonths(12).toString(),
                                                                          1054)

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(forrigeAndelBarn1),
                        eksplisittAvslag = false
                ),
                BehandlingsresultatPerson(
                        personIdent = barn2.personIdent.ident,
                        søktForPerson = true,
                        personType = barn2.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = emptyList(),
                        eksplisittAvslag = true
                )
        ))

        assertEquals(emptySet<YtelsePersonResultat>(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(setOf(YtelsePersonResultat.AVSLÅTT, YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
    }


    /**
     * Ingen resultater / fortsatt innvilget
     */

    @Test
    fun `Skal IKKE utlede noen nye resultater (fortsatt innvilget) på årlig kontroll uten endringer`() {
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                          inneværendeMåned().plusMonths(12).toString(),
                                                                          1054)


        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(forrigeAndelBarn1),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(emptySet<YtelsePersonResultat>(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }


    /**
     * OPPHØRT
     */

    @Test
    fun `Skal utlede OPPHØRT for barn i revurdering med forkortet tom`() {
        val ytelseSlutt = inneværendeMåned()
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                          inneværendeMåned().plusMonths(12).toString(),
                                                                          1054)

        val andelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                   ytelseSlutt.toString(),
                                                                   1054)

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(andelBarn1),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(ytelseSlutt,
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.ytelseSlutt)
    }

    @Test
    fun `Skal utlede OPPHØRT for barn i revurdering hvor alle perioder er opphørt`() {
        val forrigeAndel1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                      inneværendeMåned().toString(),
                                                                      1354)
        val forrigeAndel2 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().plusMonths(1).toString(),
                                                                      inneværendeMåned().plusMonths(5).toString(),
                                                                      1054)

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndel1, forrigeAndel2),
                        andeler = emptyList(),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(1, ytelsePersonerMedResultat.size)
        assertEquals(setOf(YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(TIDENES_MORGEN.toYearMonth(),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.ytelseSlutt)
    }

    @Test
    fun `Skal utlede OPPHØRT for barn i revurdering med fjernet periode på slutten`() {
        val ytelseSlutt = inneværendeMåned()
        val forrigeAndel1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                      ytelseSlutt.toString(),
                                                                      1354)
        val forrigeAndel2 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().plusMonths(1).toString(),
                                                                      inneværendeMåned().plusMonths(5).toString(),
                                                                      1054)

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndel1, forrigeAndel2),
                        andeler = listOf(forrigeAndel1),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(ytelseSlutt,
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.ytelseSlutt)
    }

    @Test
    fun `Skal utlede OPPHØRT for barn i revurdering med forkortet tom og fjernet periode på slutten`() {
        val ytelseSlutt = inneværendeMåned().minusMonths(1)
        val forrigeAndel1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                      inneværendeMåned().toString(),
                                                                      1354)
        val forrigeAndel2 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().plusMonths(1).toString(),
                                                                      inneværendeMåned().plusMonths(5).toString(),
                                                                      1054)


        val oppdatertAndel = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                       ytelseSlutt.toString(),
                                                                       1354)


        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndel1, forrigeAndel2),
                        andeler = listOf(oppdatertAndel),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(ytelseSlutt,
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.ytelseSlutt)
    }

    @Test
    fun `Skal utlede OPPHØRT på revurdering hvor alle andeler er annulert`() {
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                          inneværendeMåned().plusMonths(12).toString(),
                                                                          1054)

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = emptyList(),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }


    /**
     * ENDRET
     */
    @Test
    fun `Skal utlede ENDRET på årlig kontroll med ny løpende periode tilbake i tid`() {
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                          inneværendeMåned().plusMonths(12).toString(),
                                                                          1054)

        val andelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                   inneværendeMåned().minusMonths(12).toString(),
                                                                   1054)

        val andel2Barn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusMonths(10).toString(),
                                                                    inneværendeMåned().plusMonths(12).toString(),
                                                                    1054)

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(andelBarn1, andel2Barn1),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.ENDRET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede ENDRET på barn som går fra opphørt inneværende måned til løpende`() {
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                          inneværendeMåned().toString(),
                                                                          1054)

        val andelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                   inneværendeMåned().plusMonths(1).toString(),
                                                                   1054)

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(andelBarn1),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.ENDRET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede ENDRET på barn som endres til å ha delt bosted`() {
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                          inneværendeMåned().toString(),
                                                                          1054)

        val nyAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                     inneværendeMåned().minusMonths(5).toString(),
                                                                     1054)
        val nyAndelBarn2 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusMonths(4).toString(),
                                                                     inneværendeMåned().toString(),
                                                                     527)

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(nyAndelBarn1, nyAndelBarn2),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.ENDRET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    /**
     * ENDRET, OPPHØRT
     */
    @Test
    fun `Skal utlede ENDRET og OPPHØRT for barn med utvidet opphør`() {
        val ytelseSlutt = inneværendeMåned().minusYears(1)
        val forrigeAndel = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                     inneværendeMåned().minusYears(2).toString(),
                                                                     1054)


        val oppdatertAndel = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                       ytelseSlutt.toString(),
                                                                       1054)

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndel),
                        andeler = listOf(oppdatertAndel),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.OPPHØRT, YtelsePersonResultat.ENDRET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(ytelseSlutt,
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.ytelseSlutt)
    }

    @Test
    fun `Skal utlede ENDRET og OPPHØRT på årlig kontroll med ny opphørt periode tilbake i tid`() {
        val ytelseSlutt = inneværendeMåned().forrigeMåned()
        val forrigeAndelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                          inneværendeMåned().plusMonths(12).toString(),
                                                                          1054)

        val andelBarn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                                   inneværendeMåned().minusMonths(12).toString(),
                                                                   1054)

        val andel2Barn1 = lagBehandlingsresultatAndelTilkjentYtelse(inneværendeMåned().minusMonths(10).toString(),
                                                                    ytelseSlutt.toString(),
                                                                    1054)

        val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(listOf(
                BehandlingsresultatPerson(
                        personIdent = barn1.personIdent.ident,
                        søktForPerson = false,
                        personType = barn1.type,
                        forrigeAndeler = listOf(forrigeAndelBarn1),
                        andeler = listOf(andelBarn1, andel2Barn1),
                        eksplisittAvslag = false
                )
        ))

        assertEquals(setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(ytelseSlutt,
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.ytelseSlutt)
    }
}
