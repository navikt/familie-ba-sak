package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingsresultatUtilsTest {

    val søker = tilfeldigPerson()
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()

    val barn1Ident = randomFnr()
    val barn2Ident = randomFnr()


    @Test
    fun `Scenarie 1, endret og fortsatt innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.FORTSATT_INNVILGET)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.FORTSATT_INNVILGET)
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
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = inneværendeMåned().minusMonths(1)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = null
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Scenarie 3, opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = inneværendeMåned().minusMonths(1)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = inneværendeMåned().minusMonths(1)
                        )
                )
        )
        assertEquals(BehandlingResultat.OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Scenarie 4, endret og fortsatt innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = null
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.FORTSATT_INNVILGET),
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
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.FORTSATT_INNVILGET),
                                periodeStartForRentOpphør = null
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.FORTSATT_INNVILGET),
                                periodeStartForRentOpphør = null
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    // TODO: hvorfor blir ikke denne endret og opphørt?
    @Test
    fun `Scenarie 6, opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = inneværendeMåned().plusMonths(1)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = inneværendeMåned().plusMonths(1)
                        )
                )
        )
        assertEquals(BehandlingResultat.OPPHØRT, behandlingsresultat)
    }

    // TODO: testen sier fortsatt innvilget, mens vi asserter på opphørt - hva er riktig??
    @Test
    fun `Scenarie 8, endret og fortsatt innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = null
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = null
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    // TODO: samme her, hvorfor sier testen en ting, mens vi asserter på noe annet?
    @Test
    fun `Scenarie 9, endret og fortsatt innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = inneværendeMåned().minusMonths(9)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
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
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.FORTSATT_INNVILGET),
                                periodeStartForRentOpphør = null
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.FORTSATT_INNVILGET),
                                periodeStartForRentOpphør = null
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    // Tester for utleding av behandlingsresultat basert på ytelsepersoner
    @Test
    fun `Skal utlede innvilget med kun ny innvilgede resultater`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.FORTSATT_INNVILGET)
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
    fun `Innvilgelse på revurdering vurderes til innvilget`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.FORTSATT_INNVILGET),
                                periodeStartForRentOpphør = null
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.INNVILGET),
                                periodeStartForRentOpphør = null
                        ),
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
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.REDUSERT)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.REDUSERT)
                        )
                )
        )

        assertEquals(BehandlingResultat.INNVILGET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Skal utlede fortsatt innvilget når det ikke er endringer`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.FORTSATT_INNVILGET)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.FORTSATT_INNVILGET)
                        )
                )
        )

        assertEquals(BehandlingResultat.FORTSATT_INNVILGET, behandlingsresultat)
    }

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
                                resultater = setOf(YtelsePersonResultat.FORTSATT_INNVILGET),
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
    fun `Avslag og endring på revurdering vurderes til endring`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.FORTSATT_INNVILGET),
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
    fun `Avslag og opphørt på revurdering vurderes til avslått og opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = inneværendeMåned() // TODO er dette riktig bruk av felt?
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


    // Avklaring: er dette litt rart? Vi sier fortsatt innvilget, men det er egentlig på fagsaknivå?
    // I denne behandlingen opphører vi og innvilger vi for forskjellige barn
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
                                resultater = setOf(YtelsePersonResultat.REDUSERT)
                        )
                )
        )
        // TODO avklare om dette er riktig
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede endring når ett barn har resultat redusert`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.FORTSATT_INNVILGET)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.REDUSERT)
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede endring når det det er endring tilbake i tid på ett barn`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.FORTSATT_INNVILGET)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.FORTSATT_INNVILGET)
                        )
                )
        )

        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Skal utlede endret og opphørt når det det er endring tilbake i tid på barna som fører til opphør`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = inneværendeMåned()
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = inneværendeMåned().plusMonths(1)
                        )
                )
        )

        assertEquals(BehandlingResultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Skal utlede opphørt når 2 barn blir redusert til samme måned`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = inneværendeMåned().plusMonths(1)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = false,
                                resultater = setOf(YtelsePersonResultat.REDUSERT),
                                periodeStartForRentOpphør = inneværendeMåned().plusMonths(1)
                        )
                )
        )
        assertEquals(BehandlingResultat.OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Skal utlede innvilget med 2 barn hvor 1 barn kun har etterbetaling`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                erFramstiltKravForINåværendeBehandling = true,
                                resultater = setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.REDUSERT)
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

    // Tester for utleding av krav
    @Test
    fun `Skal kun finne søknadsytelsePersoner`() {
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
        assertTrue(ytelsePersoner.first().erFramstiltKravForINåværendeBehandling)
    }

    @Test
    fun `Skal kun finne endringsytelsePersoner`() {
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
        assertFalse(ytelsePersoner.first().erFramstiltKravForINåværendeBehandling)
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

        val ytelsePersoner = BehandlingsresultatUtils.utledKrav(
                søknadDTO = null,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1Ordinær, forrigeAndelBarn1Utvidet)
        )

        assertEquals(2, ytelsePersoner.size)
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && !it.erFramstiltKravForINåværendeBehandling })
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erFramstiltKravForINåværendeBehandling })
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

        val ytelsePersoner = BehandlingsresultatUtils.utledKrav(
                søknadDTO = søknadDTO,
                forrigeAndelerTilkjentYtelse = listOf(forrigeAndelBarn1Ordinær, forrigeAndelBarn1Utvidet)
        )

        assertEquals(2, ytelsePersoner.size)
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.ORDINÆR_BARNETRYGD && it.erFramstiltKravForINåværendeBehandling })
        assertTrue(ytelsePersoner.any { it.personIdent == barn1.personIdent.ident && it.ytelseType == YtelseType.UTVIDET_BARNETRYGD && !it.erFramstiltKravForINåværendeBehandling })
    }


    // Tester for ytelse person resultater
    @Test
    fun `Skal utelede innvilget første gang barnet blir vurdert`() {
        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                inneværendeMåned().plusYears(2).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = true
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = emptyList(),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1)
        )

        assertEquals(1, ytelsePersonerMedResultat.size)
        assertEquals(setOf(YtelsePersonResultat.INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utelede innvilget og redusert første gang barnet blir vurdert med kun etterbetaling`() {
        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                inneværendeMåned().plusYears(2).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = true
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = emptyList(),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1)
        )

        assertEquals(1, ytelsePersonerMedResultat.size)
        assertEquals(setOf(YtelsePersonResultat.INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede innvilget første gang barn nr2 blir vurdert i en revurdering`() {
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
                        erFramstiltKravForINåværendeBehandling = false
                ),
                YtelsePerson(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = true
                )
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1,
                                                                                                        andelBarn2)
        )

        assertEquals(2, ytelsePersonerMedResultat.size)
        assertEquals(setOf(YtelsePersonResultat.FORTSATT_INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)

        assertEquals(setOf(YtelsePersonResultat.INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede innvilget og redusert første gang barn nr2 blir vurdert i en revurdering med kun etterbetaling`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn2 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(3).toString(),
                                                inneværendeMåned().minusYears(1).toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn2)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = false
                ),
                YtelsePerson(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = true
                )
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1,
                                                                                                        andelBarn2)
        )

        assertEquals(2, ytelsePersonerMedResultat.size)
        assertEquals(setOf(YtelsePersonResultat.FORTSATT_INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)

        assertEquals(setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.REDUSERT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede redusert når barn nr1 blir redusert i en revurdering`() {
        val reduksjonsmåned = inneværendeMåned()
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(5).toString(),
                                                reduksjonsmåned.toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = false
                )
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1)
        )

        assertEquals(1, ytelsePersonerMedResultat.size)
        assertEquals(setOf(YtelsePersonResultat.REDUSERT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(reduksjonsmåned.plusMonths(1),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.periodeStartForRentOpphør)
    }

    @Test
    fun `Skal utelede avslag første gang ytelsePersoneret for barn fremstilles`() {
        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = true
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = emptyList(),
                                                                                                andelerTilkjentYtelse = emptyList()
        )

        assertEquals(setOf(YtelsePersonResultat.AVSLÅTT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede avslag på søknad for nytt barn i revurdering`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = false
                ),
                YtelsePerson(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = true
                )
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1)
        )

        assertEquals(setOf(YtelsePersonResultat.FORTSATT_INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(setOf(YtelsePersonResultat.AVSLÅTT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede redusert på revurdering hvor alle andeler er annulert`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = false
                )
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf()
        )

        assertEquals(setOf(YtelsePersonResultat.REDUSERT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede fortsatt innvilget på årlig kontroll uten endringer`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = false
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1)
        )

        assertEquals(setOf(YtelsePersonResultat.FORTSATT_INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede endring på årlig kontroll med liten endring tilbake i tid`() {
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
                        erFramstiltKravForINåværendeBehandling = false
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1,
                                                                                                        andel2Barn1)
        )

        assertEquals(setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.FORTSATT_INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }

    @Test
    fun `Skal utlede endring og opphør på årlig kontroll med liten endring tilbake i tid og opphør`() {
        val reduksjonsmåned = inneværendeMåned().forrigeMåned()
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
                                                 reduksjonsmåned.toString(),
                                                 YtelseType.ORDINÆR_BARNETRYGD,
                                                 1054,
                                                 person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = false
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1,
                                                                                                        andel2Barn1)
        )

        assertEquals(setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.REDUSERT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
        assertEquals(null,
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.periodeStartForRentOpphør)
    }

    @Test
    fun `Skal utelede innvilget andre gang ytelsePersoneret for barn fremstilles`() {
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
                        erFramstiltKravForINåværendeBehandling = true
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1)
        )

        assertEquals(setOf(YtelsePersonResultat.INNVILGET, YtelsePersonResultat.REDUSERT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }


    @Test
    fun `Skal utlede innvilget på søknad for nytt barn i revurdering`() {
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
                        erFramstiltKravForINåværendeBehandling = false
                ),
                YtelsePerson(
                        personIdent = barn2.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = true
                )
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1,
                                                                                                        andelBarn2)
        )

        assertEquals(setOf(YtelsePersonResultat.FORTSATT_INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)

        assertEquals(setOf(YtelsePersonResultat.INNVILGET),
                     ytelsePersonerMedResultat.find { it.personIdent == barn2.personIdent.ident }?.resultater)
    }


    @Test
    fun `Skal utlede opphør på endring for barn i revurdering`() {
        val forrigeAndelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                       inneværendeMåned().plusMonths(12).toString(),
                                                       YtelseType.ORDINÆR_BARNETRYGD,
                                                       1054,
                                                       person = barn1)

        // Opphør ytelsen for barnet 1 måned tilbake i tid
        val andelBarn1 = lagAndelTilkjentYtelse(inneværendeMåned().minusYears(4).toString(),
                                                inneværendeMåned().toString(),
                                                YtelseType.ORDINÆR_BARNETRYGD,
                                                1054,
                                                person = barn1)

        val ytelsePersoner = listOf(
                YtelsePerson(
                        personIdent = barn1.personIdent.ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        erFramstiltKravForINåværendeBehandling = false
                ),
        )

        val ytelsePersonerMedResultat = BehandlingsresultatUtils.utledYtelsePersonerMedResultat(ytelsePersoner = ytelsePersoner,
                                                                                                forrigeAndelerTilkjentYtelse = listOf(
                                                                                                        forrigeAndelBarn1),
                                                                                                andelerTilkjentYtelse = listOf(
                                                                                                        andelBarn1)
        )

        assertEquals(setOf(YtelsePersonResultat.REDUSERT),
                     ytelsePersonerMedResultat.find { it.personIdent == barn1.personIdent.ident }?.resultater)
    }
}