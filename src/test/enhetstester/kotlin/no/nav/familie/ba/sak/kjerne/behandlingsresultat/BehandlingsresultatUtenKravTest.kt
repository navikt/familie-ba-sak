package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BehandlingsresultatUtenKravTest {

    val søker = tilfeldigPerson()
    private val barn1Aktør = randomAktør()
    private val barn2Aktør = randomAktør()

    /**
     * Tester for caser hvor krav ikke er framstilt av søker.
     * Caser gjelder
     * - Revurdering
     * - Årsak ikke søknad
     * - To barn
     *
     * Oversikt: https://www.figma.com/file/V61gamZ8orsoGwaYk5SXyH/Diskusjonsskisser?node-id=0%3A1
     *
     * ENDRET
     * 1, 4, 5, 6, 7, 10
     *
     * OPPHØRT
     * 3, 11
     *
     * ENDRET_OG_OPPHØRT
     * 2, 8, 9
     */

    @Test
    fun `Case 1 ENDRET - Én får forkortet med fom-dato og en får forkortet med tom-dato`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            listOf(
                YtelsePerson(
                    aktør = barn1Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = inneværendeMåned().minusMonths(1),
                ),
                YtelsePerson(
                    aktør = barn2Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.ENDRET_UTBETALING),
                    ytelseSlutt = inneværendeMåned().plusMonths(1),
                ),
            )
        )
        assertEquals(Behandlingsresultat.ENDRET_UTBETALING, behandlingsresultat)
    }

    @Test
    fun `Case 2 ENDRET_OG_OPPHØRT - begge forkortet med ny lik tom-dato, én også med fom-dato`() {
        val likOpphørsdato = inneværendeMåned().minusMonths(1)
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            listOf(
                YtelsePerson(
                    aktør = barn2Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = likOpphørsdato
                ),
                YtelsePerson(
                    aktør = barn1Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.ENDRET_UTBETALING, YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = likOpphørsdato
                )
            )
        )
        assertEquals(Behandlingsresultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Case 3 OPPHØRT - begge forkortet med ny lik tom-dato`() {
        val likOpphørsdato = inneværendeMåned().minusMonths(1)
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            listOf(
                YtelsePerson(
                    aktør = barn2Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = likOpphørsdato
                ),
                YtelsePerson(
                    aktør = barn1Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = likOpphørsdato
                )
            )
        )
        assertEquals(Behandlingsresultat.OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Case 4 - ENDRET - kun én person endret`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            listOf(
                YtelsePerson(
                    aktør = barn2Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.ENDRET_UTBETALING, YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = inneværendeMåned().minusMonths(1)
                ),
                YtelsePerson(
                    aktør = barn1Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = emptySet(),
                    ytelseSlutt = inneværendeMåned().plusMonths(1),
                )
            )
        )
        assertEquals(Behandlingsresultat.ENDRET_UTBETALING, behandlingsresultat)
    }

    @Test
    fun `Case 5 ENDRET - Begge får et likt hull i perioden`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            listOf(
                YtelsePerson(
                    aktør = barn2Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.ENDRET_UTEN_UTBETALING),
                    ytelseSlutt = inneværendeMåned().plusMonths(1),
                ),
                YtelsePerson(
                    aktør = barn1Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.ENDRET_UTEN_UTBETALING),
                    ytelseSlutt = inneværendeMåned().plusMonths(1),
                )
            )
        )
        assertEquals(Behandlingsresultat.ENDRET_UTEN_UTBETALING, behandlingsresultat)
    }

    @Test
    fun `Case 6 ENDRET - én opphører pga forkortet tom-dato `() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            listOf(
                YtelsePerson(
                    aktør = barn2Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = emptySet(),
                    ytelseSlutt = inneværendeMåned().plusMonths(1)
                ),
                YtelsePerson(
                    aktør = barn1Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = inneværendeMåned()
                )
            )
        )
        assertEquals(Behandlingsresultat.ENDRET_UTBETALING, behandlingsresultat)
    }

    @Test
    fun `Case 8 ENDRET_OG_OPPHØRT - Begge får utvidet opphøret med lik tom-dato`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            listOf(
                YtelsePerson(
                    aktør = barn2Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.ENDRET_UTBETALING, YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = inneværendeMåned()
                ),
                YtelsePerson(
                    aktør = barn1Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.ENDRET_UTBETALING, YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = inneværendeMåned()
                )
            )
        )
        assertEquals(Behandlingsresultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Case 9 ENDRET_OG_OPPHØRT - Begge går fra løpende til opphørt pga nye ulike tom-datoer`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            listOf(
                YtelsePerson(
                    aktør = barn2Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = inneværendeMåned().minusMonths(2)
                ),
                YtelsePerson(
                    aktør = barn1Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = inneværendeMåned()
                )
            )
        )
        assertEquals(Behandlingsresultat.ENDRET_OG_OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `Case 10 ENDRET - Én av personene får utvidet pga ny fom-dato`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            listOf(
                YtelsePerson(
                    aktør = barn2Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.ENDRET_UTBETALING),
                    ytelseSlutt = inneværendeMåned().plusMonths(1),
                ),
                YtelsePerson(
                    aktør = barn1Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = emptySet(),
                    ytelseSlutt = inneværendeMåned().plusMonths(1),
                )
            )
        )
        assertEquals(Behandlingsresultat.ENDRET_UTBETALING, behandlingsresultat)
    }

    @Test
    fun `Case 11 OPPHØRT - Alt er opphørt`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            listOf(
                YtelsePerson(
                    aktør = barn2Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = TIDENES_MORGEN.toYearMonth()
                ),
                YtelsePerson(
                    aktør = barn1Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(YtelsePersonResultat.OPPHØRT),
                    ytelseSlutt = TIDENES_MORGEN.toYearMonth()
                )
            )
        )
        assertEquals(Behandlingsresultat.OPPHØRT, behandlingsresultat)
    }

    @Test
    fun `FORTSATT_INNVILGET - ingen endringer`() {
        val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
            listOf(
                YtelsePerson(
                    aktør = barn2Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(),
                    ytelseSlutt = inneværendeMåned().plusMonths(1)
                ),
                YtelsePerson(
                    aktør = barn1Aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                    resultater = setOf(),
                    ytelseSlutt = inneværendeMåned().plusMonths(1)
                )
            )
        )

        assertEquals(Behandlingsresultat.FORTSATT_INNVILGET, behandlingsresultat)
    }
}
