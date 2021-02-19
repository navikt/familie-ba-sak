package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingsresultatUtilsTest {

    val søker = tilfeldigPerson()

    val barn1Ident = randomFnr()
    val barn2Ident = randomFnr()


    // Innvilget
    @Test
    fun `Skal utlede innvilget med 1 barn med løpende utbetaling`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.INNVILGET)
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
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.INNVILGET)
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
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.INNVILGET)
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
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
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
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.INNVILGET)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
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
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET_ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    // Avslått
    @Test
    fun `Avslag på førstegangsbehandling vurderes til avslått`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                periodeStartForRentOpphør = null
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
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(),
                                periodeStartForRentOpphør = null
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                periodeStartForRentOpphør = null
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
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                periodeStartForRentOpphør = null
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
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                periodeStartForRentOpphør = null
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                periodeStartForRentOpphør = null
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
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                                periodeStartForRentOpphør = null
                        ),
                )
        )
        assertEquals(BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT, behandlingsresultat)
    }


    // Endringer uten søknad
    @Test
    fun `Scenarie 1, endret og fortsatt innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf()
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
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned().minusMonths(1)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Scenarie 4, endret og fortsatt innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(),
                                periodeStartForRentOpphør = null
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Scenarie 5, endret og fortsatt innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                periodeStartForRentOpphør = null
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                periodeStartForRentOpphør = null
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
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned().plusMonths(1)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned().plusMonths(1)
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Scenarie 8, endret og fortsatt innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Scenarie 9, endret og fortsatt innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned().minusMonths(9)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Scenarie 10, endret og fortsatt innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                periodeStartForRentOpphør = null
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(),
                                periodeStartForRentOpphør = null
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
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned().plusMonths(1)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned().plusMonths(1)
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
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                periodeStartForRentOpphør = inneværendeMåned()
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
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf()
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
                                    erFramstiltKravForINåværendeBehandling = false,
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
                                    erFramstiltKravForINåværendeBehandling = true,
                                    resultater = setOf(YtelsePersonResultat.ENDRET)
                            ),
                            YtelsePerson(
                                    personIdent = barn1Ident,
                                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                    erFramstiltKravForINåværendeBehandling = true,
                                    resultater = setOf(YtelsePersonResultat.AVSLÅTT)
                            )
                    )
            )
        }

        assertTrue(feil.message?.contains("Behandlingsresultatet er ikke støttet i løsningen")!!)
    }

    @Test
    fun `Skal kaste feil dersom ytelseperson har resultat opphør uten periodeStartForRentOpphør satt`() {
        val feil = assertThrows<Feil> {
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                    listOf(
                            YtelsePerson(
                                    personIdent = barn1Ident,
                                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                    erFramstiltKravForINåværendeBehandling = false,
                                    resultater = setOf(YtelsePersonResultat.OPPHØRT)
                            )
                    )
            )
        }

        assertTrue(feil.message?.contains("Minst én ytelseperson har fått opphør som resultat uten å ha periodeStartForRentOpphør satt")!!)
    }
}