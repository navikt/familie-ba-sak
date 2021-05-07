package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EndretOpphørtCaserTest {

    val søker = tilfeldigPerson()
    val barn1Ident = randomFnr()
    val barn2Ident = randomFnr()

    /**
     * Caser gjelder
     * - Revurdering
     * - Årsak nye opplysninger // TODO mer generell?
     * - To barn med årsak nye opplysninger
     *
     * Oversikt: https://www.figma.com/file/V61gamZ8orsoGwaYk5SXyH/Diskusjonsskisser?node-id=0%3A1
     *
     * ENDRET
     * 1, 4, 5, 7, 10
     *
     * OPPHØRT
     * 3 (6, 11)
     *
     * ENDRET_OG_OPPHØRT
     * 2, 8, 9
     */

    @Test
    fun `Case 1 ENDRET - Én får forkortet med fom-dato og en får forkortet med tom-dato`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned().minusMonths(1),
                        ),
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                ytelseSlutt = inneværendeMåned().plusMonths(1),
                        ),
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Case 2 ENDRET_OG_OPPHØRT - begge forkortet med ny lik tom-dato, én også med fom-dato`() {
        val likOpphørsdato = inneværendeMåned().minusMonths(1)
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = likOpphørsdato
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = likOpphørsdato
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Case 3 OPPHØRT - begge forkortet med ny lik tom-dato`() {
        val likOpphørsdato = inneværendeMåned().minusMonths(1)
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = likOpphørsdato
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = likOpphørsdato
                        )
                )
        )
        assertEquals(BehandlingResultat.OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Case 4 - ENDRET - kun én person endret`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET, YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned().minusMonths(1)
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = emptySet(),
                                ytelseSlutt = inneværendeMåned().plusMonths(1),
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Case 5 ENDRET - Begge får et likt hull i perioden`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                ytelseSlutt = inneværendeMåned().plusMonths(1),
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                ytelseSlutt = inneværendeMåned().plusMonths(1),
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }

    @Test
    fun `Case 6 OPPHØRT - Begge opphører`() { // TODO: Fjerne? Likt som case 3 hvor begge går fra løpende til opphørt. Samme gjelder case 11.
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
    fun `Case 8 ENDRET_OG_OPPHØRT - Begge får utvidet opphøret med lik tom-dato`() {
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
    fun `Case 9 ENDRET_OG_OPPHØRT - Begge går fra løpende til opphørt pga nye ulike tom-datoer`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.OPPHØRT),
                                ytelseSlutt = inneværendeMåned().minusMonths(2)
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
    fun `Case 10 ENDRET - Én av personene får utvidet pga ny fom-dato`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = setOf(YtelsePersonResultat.ENDRET),
                                ytelseSlutt = inneværendeMåned().plusMonths(1),
                        ),
                        YtelsePerson(
                                personIdent = barn1Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = emptySet(),
                                ytelseSlutt = inneværendeMåned().plusMonths(1),
                        )
                )
        )
        assertEquals(BehandlingResultat.ENDRET, behandlingsresultat)
    }


    // TODO NYTT CASE?
    @Test
    fun `NYTT CASE - som nr 4, men kun opphør`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                        YtelsePerson(
                                personIdent = barn2Ident,
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kravOpprinnelse = KravOpprinnelse.TIDLIGERE,
                                resultater = emptySet(),
                                ytelseSlutt = inneværendeMåned().plusMonths(1)
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
}