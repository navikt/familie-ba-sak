package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingsresultatUtilsTest {

    val søker = tilfeldigPerson()

    val barn1Ident = randomFnr()
    val barn2Ident = randomFnr()
    val defaultYtelseSluttForLøpende = inneværendeMåned().plusMonths(1)
    val defaultYtelseSluttForAvslått = TIDENES_MORGEN.toYearMonth()


    // Innvilget
    @Test
    fun `Skal utlede innvilget med 1 barn med løpende utbetaling`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede innvilget med 2 barn hvor 1 barn kun har etterbetaling`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede innvilget med kun ny innvilgede resultater`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede innvilget og opphør med kun ny innvilgede resultater tilbake i tid`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Skal utlede endring når det ett barn har resultat redusert og ett barn har fått innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.INNVILGET_OG_ENDRET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede innvilget, endret og opphør med innvilget søknad og opphørt ytelse med andre endringer`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET_ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    // Delvis innvilget
    @Test
    fun `Skal utlede delvis innvilget med 1 barn med løpende utbetaling`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )

        assertEquals(BehandlingResultat.DELVIS_INNVILGET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede delvis innvilget på revurdering med både innvilgede og avslåtte perioder`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )

        assertEquals(BehandlingResultat.DELVIS_INNVILGET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede delvis innvilget og opphørt med 2 barn hvor 1 barn kun har etterbetaling`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått
                        )
                )
        )

        assertEquals(BehandlingResultat.DELVIS_INNVILGET_OG_OPPHØRT, behandlingsresultat)
    }


    @Test
    fun `Skal utlede delvis innvilget og opphør med kun ny innvilgede resultater tilbake i tid`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET,
                                                   YtelsePersonResultat.AVSLÅTT,
                                                   YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
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
    fun `Skal utlede endring når det ett barn har resultat redusert og ett barn har fått delvis innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForLøpende
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.DELVIS_INNVILGET_OG_ENDRET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede delvis innvilget, endret og opphør med delvis innvilget søknad og opphørt ytelse med andre endringer`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.INNVILGET,
                                                   YtelsePersonResultat.AVSLÅTT,
                                                   YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )

        assertEquals(BehandlingResultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    // Avslått
    @Test
    fun `Avslag på førstegangsbehandling vurderes til avslått`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått
                        ),
                )
        )
        assertEquals(BehandlingResultat.AVSLÅTT, behandlingsresultat)
    }

    @Test
    fun `Avslag på revurdering vurderes til avslått`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(),
                                ytelseSlutt = defaultYtelseSluttForAvslått
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått
                        ),
                )
        )
        assertEquals(BehandlingResultat.AVSLÅTT, behandlingsresultat)
    }

    @Test
    fun `Avslag og opphør på revurdering vurderes til avslått og opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått,
                        ),
                )
        )
        assertEquals(BehandlingResultat.AVSLÅTT_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Avslag og endring på revurdering vurderes til endring`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått,
                        ),
                )
        )
        assertEquals(BehandlingResultat.AVSLÅTT_OG_ENDRET, behandlingsresultat)
    }

    @Test
    fun `Avslag, endring og opphør på revurdering vurderes til avslått, endret og opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                ytelseSlutt = defaultYtelseSluttForAvslått,
                        ),
                )
        )
        assertEquals(BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT, behandlingsresultat)
    }


    // Endringer uten søknad
    @Test
    fun `Scenarie 1, endret`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Scenarie 2, endret og opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Scenarie 4, endret`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Scenarie 5, endret`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Scenarie 6, opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Scenarie 8, endret og opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Scenarie 9, endret og opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned().minusMonths(9)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Scenarie 10, endret`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(),
                                ytelseSlutt = defaultYtelseSluttForLøpende,
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede opphørt når 2 barn blir redusert til samme måned`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Skal utlede endring når ett barn har resultat opphør`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(),
                                ytelseSlutt = defaultYtelseSluttForLøpende
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede fortsatt innvilget når det ikke er endringer`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(),
                                ytelseSlutt = defaultYtelseSluttForLøpende
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(),
                                ytelseSlutt = defaultYtelseSluttForLøpende
                        )
                )
        )

        assertEquals(BehandlingResultat.FORTSATT_INNVILGET, behandlingsresultat)
    }

    @Test
    fun `Skal kaste feil dersom det finnes uvurderte ytelsepersoner`() {
        val feil = assertThrows<Feil> {
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                    listOf(
                            YtelsePerson(
                                    personIdent = barn1Ident,
                                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                    kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                    resultater = setOf(YtelsePersonResultat.IKKE_VURDERT)
                            )
                    )
            )
        }

        assertEquals("Minst én ytelseperson er ikke vurdert", feil.message)
    }

    @Test
    fun `Skal kaste feil dersom sammensetningen av resultater ikke er støttet`() {
        val feil = assertThrows<Feil> {
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                    listOf(
                            YtelsePerson(
                                    personIdent = barn1Ident,
                                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                    kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                    resultater = setOf(YtelsePersonResultat.ENDRET),
                                    ytelseSlutt = defaultYtelseSluttForLøpende,
                            ),
                            YtelsePerson(
                                    personIdent = barn1Ident,
                                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                    kravOpprinnelse = KravOpprinnelse.SØKNAD,
                                    resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                    ytelseSlutt = defaultYtelseSluttForAvslått,
                            )
                    )
            )
        }

        assertTrue(feil.message?.contains("Behandlingsresultatet er ikke støttet i løsningen")!!)
    }

    @Test
    fun `Kaster feil ved ugyldig resultat på førstegangsbehandling`() {
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        setOf(BehandlingResultat.AVSLÅTT_OG_OPPHØRT,
              BehandlingResultat.ENDRET,
              BehandlingResultat.ENDRET_OG_OPPHØRT,
              BehandlingResultat.OPPHØRT,
              BehandlingResultat.FORTSATT_INNVILGET,
              BehandlingResultat.IKKE_VURDERT).forEach {

            val feil = assertThrows<FunksjonellFeil> {
                BehandlingsresultatUtils.validerBehandlingsresultat(behandling, it)
            }
            assertTrue(feil.message?.contains("ugyldig") ?: false)
        }
    }

    @Test
    fun `Kaster feil ved ugyldig resultat på revurdering`() {
        val behandling = lagBehandling(behandlingType = BehandlingType.REVURDERING)

        val feil = assertThrows<FunksjonellFeil> {
            BehandlingsresultatUtils.validerBehandlingsresultat(behandling, BehandlingResultat.IKKE_VURDERT)
        }
        assertTrue(feil.message?.contains("ugyldig") ?: false)
    }

}