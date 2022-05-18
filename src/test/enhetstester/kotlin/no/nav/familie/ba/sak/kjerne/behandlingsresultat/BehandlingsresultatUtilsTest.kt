package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class BehandlingsresultatUtilsTest {

    val søker = tilfeldigPerson()

    private val barn1Aktør = randomAktørId()

    @Test
    fun `Skal kaste feil dersom det finnes uvurderte ytelsepersoner`() {
        val feil = assertThrows<Feil> {
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                    YtelsePerson(
                        aktør = barn1Aktør,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                        resultater = setOf(YtelsePersonResultat.IKKE_VURDERT)
                    )
                )
            )
        }

        assertEquals("Minst én ytelseperson er ikke vurdert", feil.message)
    }

    @Test
    fun `Kaster feil ved ugyldig resultat på førstegangsbehandling`() {
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        setOf(
            Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
            Behandlingsresultat.ENDRET_UTEN_UTBETALING,
            Behandlingsresultat.ENDRET_OG_OPPHØRT,
            Behandlingsresultat.OPPHØRT,
            Behandlingsresultat.FORTSATT_INNVILGET,
            Behandlingsresultat.IKKE_VURDERT
        ).forEach {

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
            BehandlingsresultatUtils.validerBehandlingsresultat(behandling, Behandlingsresultat.IKKE_VURDERT)
        }
        assertTrue(feil.message?.contains("ugyldig") ?: false)
    }

    @Test
    fun `skal returnere AVSLÅTT_OG_OPPHØRT behandlingsresultat`() {

        val personer = listOf(
            lagYtelsePerson(YtelsePersonResultat.AVSLÅTT),
            lagYtelsePerson(YtelsePersonResultat.OPPHØRT),
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT),
        )

        assertEquals(
            Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `skal returnere AVSLÅTT_OG_OPPHØRT behandlingsresultat når et barn har fortsatt opphørt og søker har avslått`() {

        val personer = listOf(
            lagYtelsePerson(YtelsePersonResultat.AVSLÅTT),
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT),
        )

        assertEquals(
            Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `skal returnere ENDRET_OG_OPPHØRT behandlingsresultat`() {

        val personer = listOf(
            lagYtelsePerson(YtelsePersonResultat.ENDRET_UTBETALING),
            lagYtelsePerson(YtelsePersonResultat.OPPHØRT),
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT),
        )

        assertEquals(
            Behandlingsresultat.ENDRET_OG_OPPHØRT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `skal returnere AVSLÅTT_ENDRET_OG_OPPHØRT behandlingsresultat`() {

        val personer = listOf(
            lagYtelsePerson(YtelsePersonResultat.ENDRET_UTBETALING),
            lagYtelsePerson(YtelsePersonResultat.AVSLÅTT),
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT),
        )

        assertEquals(
            Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }
}

private fun lagYtelsePerson(
    resultat: YtelsePersonResultat,
    ytelseSlutt: YearMonth? = YearMonth.now().minusMonths(2)
): YtelsePerson {
    val ytelseType = YtelseType.ORDINÆR_BARNETRYGD
    val kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE, KravOpprinnelse.INNEVÆRENDE)
    return YtelsePerson(
        aktør = randomAktørId(),
        ytelseType = ytelseType,
        kravOpprinnelse = kravOpprinnelse,
        resultater = setOf(resultat),
        ytelseSlutt = ytelseSlutt
    )
}
