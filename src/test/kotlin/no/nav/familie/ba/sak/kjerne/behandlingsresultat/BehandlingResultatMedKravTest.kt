package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BehandlingResultatMedKravTest {

    /**
     * Tester for caser hvor krav er framstilt av søker
     */

    val søker = tilfeldigPerson()

    val barn1Ident = randomFnr()
    val barn2Ident = randomFnr()
    val defaultYtelseSluttForLøpende = inneværendeMåned().plusMonths(1)
    val defaultYtelseSluttForAvslått = TIDENES_MORGEN.toYearMonth()

    // Innvilget
    @Test
    fun `INNVILGET - Skal utlede innvilget med 1 barn med løpende utbetaling`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET, behandlingsresultat)
    }

    @Test
    fun `INNVILGET - Skal utlede innvilget med 2 barn hvor 1 barn kun har etterbetaling`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET, behandlingsresultat)
    }

    @Test
    fun `INNVILGET - kun ny innvilgede resultater`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET, behandlingsresultat)
    }

    @Test
    fun `INNVILGET_OG_OPPHØRT - kun ny innvilgede resultater tilbake i tid`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `INNVILGET_OG_ENDRET - ett barn har resultat redusert og ett barn har fått innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.INNVILGET_OG_ENDRET, behandlingsresultat)
    }

    @Test
    fun `INNVILGET_OG_ENDRET - tidligere barn og nytt innvilget barn får opphør fra forskjellige datoer`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned(),
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned().minusMonths(1)
                        )
                )
        )
        assertEquals(BehandlingResultat.INNVILGET_OG_ENDRET, behandlingsresultat)
    }

    @Test
    fun `INNVILGET_ENDRET_OG_OPPHØRT - innvilget søknad og opphørt ytelse med andre endringer`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET_ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    // Delvis innvilget
    @Test
    fun `DELVIS_INNVILGET - 1 barn med løpende utbetaling`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )

        assertEquals(BehandlingResultat.DELVIS_INNVILGET, behandlingsresultat)
    }

    @Test
    fun `DELVIS_INNVILGET - revurdering med både innvilgede og avslåtte perioder`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )

        assertEquals(BehandlingResultat.DELVIS_INNVILGET, behandlingsresultat)
    }

    @Test
    fun `DELVIS_INNVILGET_OG_OPPHØRT - 2 barn hvor 1 barn kun har etterbetaling`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått
                        )
                )
        )

        assertEquals(BehandlingResultat.DELVIS_INNVILGET_OG_OPPHØRT, behandlingsresultat)
    }


    @Test
    fun `DELVIS_INNVILGET_OG_OPPHØRT - kun ny innvilgede resultater tilbake i tid`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET,
                                                   YtelsePersonResultat.AVSLÅTT,
                                                   YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET,
                                                   YtelsePersonResultat.AVSLÅTT,
                                                   YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )

        assertEquals(BehandlingResultat.DELVIS_INNVILGET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `DELVIS_INNVILGET_OG_ENDRET - ett barn har resultat redusert og ett barn har fått delvis innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForLøpende
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.DELVIS_INNVILGET_OG_ENDRET, behandlingsresultat)
    }

    @Test
    fun `DELVIS_INNVILGET_ENDRET_OG_OPPHØRT - delvis innvilget søknad og opphørt ytelse med andre endringer`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET,
                                                   YtelsePersonResultat.AVSLÅTT,
                                                   YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )

        assertEquals(BehandlingResultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `DELVIS_INNVILGET_ENDRET_OG_OPPHØRT - delvis innvilget søknad, et barn med forkortet opphørsdato`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.INNVILGET,
                                                   YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )

        assertEquals(BehandlingResultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    // Avslått
    @Test
    fun `AVSLÅTT - førstegangsbehandling vurderes til avslått`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått
                        ),
                )
        )
        assertEquals(BehandlingResultat.AVSLÅTT, behandlingsresultat)
    }

    @Test
    fun `AVSLÅTT - revurdering vurderes til avslått`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(),
                                ytelseSlutt = defaultYtelseSluttForAvslått
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått
                        ),
                )
        )
        assertEquals(BehandlingResultat.AVSLÅTT, behandlingsresultat)
    }

    @Test
    fun `AVSLÅTT_OG_OPPHØRT - revurdering vurderes til avslått og opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått,
                        ),
                )
        )
        assertEquals(BehandlingResultat.AVSLÅTT_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `AVSLÅTT_OG_ENDRET - revurdering vurderes til endring`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått,
                        ),
                )
        )
        assertEquals(BehandlingResultat.AVSLÅTT_OG_ENDRET, behandlingsresultat)
    }

    @Test
    fun `AVSLÅTT_ENDRET_OG_OPPHØRT -  revurdering vurderes til avslått, endret og opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått,
                        ),
                )
        )
        assertEquals(BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `FORTSATT_INNVILGET - søknad uten endring`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE, KravOpprinnelse.INNEVÆRENDE),
                                resultater = setOf(),
                                ytelseSlutt = inneværendeMåned()
                        ),
                )
        )
        assertEquals(BehandlingResultat.FORTSATT_INNVILGET, behandlingsresultat)
    }
}