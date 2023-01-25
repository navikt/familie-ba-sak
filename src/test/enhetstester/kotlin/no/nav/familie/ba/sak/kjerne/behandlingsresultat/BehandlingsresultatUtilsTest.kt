package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatUtils.kombinerSøknadsresultater
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

class BehandlingsresultatUtilsTest {

    val søker = tilfeldigPerson()

    private val barn1Aktør = randomAktør()

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
                validerBehandlingsresultat(behandling, it)
            }
            assertTrue(feil.message?.contains("ugyldig") ?: false)
        }
    }

    @Test
    fun `Kaster feil ved ugyldig resultat på revurdering`() {
        val behandling = lagBehandling(behandlingType = BehandlingType.REVURDERING)

        val feil = assertThrows<FunksjonellFeil> {
            validerBehandlingsresultat(behandling, Behandlingsresultat.IKKE_VURDERT)
        }
        assertTrue(feil.message?.contains("ugyldig") ?: false)
    }

    @Test
    fun `skal returnere AVSLÅTT_OG_OPPHØRT behandlingsresultat`() {
        val personer = listOf(
            lagYtelsePerson(YtelsePersonResultat.AVSLÅTT),
            lagYtelsePerson(YtelsePersonResultat.OPPHØRT),
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT)
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
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT)
        )

        assertEquals(
            Behandlingsresultat.AVSLÅTT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `skal returnere ENDRET_OG_OPPHØRT behandlingsresultat`() {
        val personer = listOf(
            lagYtelsePerson(YtelsePersonResultat.ENDRET_UTBETALING),
            lagYtelsePerson(YtelsePersonResultat.OPPHØRT),
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT)
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
            lagYtelsePerson(YtelsePersonResultat.FORTSATT_OPPHØRT)
        )

        assertEquals(
            Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `Skal returnere FORTSATT_OPPHØRT behandlingsresultat hvis ytelsen opphører på forskjellig tidspunkt`() {
        val personer = listOf(
            lagYtelsePerson(
                resultat = YtelsePersonResultat.FORTSATT_OPPHØRT,
                ytelseSlutt = YearMonth.now().minusMonths(1)
            ),
            lagYtelsePerson(
                resultat = YtelsePersonResultat.FORTSATT_OPPHØRT,
                ytelseSlutt = YearMonth.now()
            )
        )

        assertEquals(
            Behandlingsresultat.FORTSATT_OPPHØRT,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `både ENDRET_UTBETALING og ENDRET_UTEN_UTBETALING som samlede resultater gir behandlingsresultat ENDRET_UTBETALING`() {
        val personer = listOf(
            lagYtelsePerson(
                resultat = YtelsePersonResultat.ENDRET_UTBETALING,
                ytelseSlutt = YearMonth.now().minusMonths(1)
            ),
            lagYtelsePerson(
                resultat = YtelsePersonResultat.ENDRET_UTEN_UTBETALING,
                ytelseSlutt = YearMonth.now()
            )
        )

        assertEquals(
            Behandlingsresultat.ENDRET_UTBETALING,
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(personer)
        )
    }

    @Test
    fun `kombinerSøknadsresultater skal kaste feil dersom lista ikke inneholder noe som helst`() {
        val listeMedIngenSøknadsresultat = listOf<BehandlingsresultatUtils.Søknadsresultat>()

        val feil = assertThrows<Feil> { listeMedIngenSøknadsresultat.kombinerSøknadsresultater() }

        assertThat(feil.message, Is("Klarer ikke utlede søknadsresultat"))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingsresultatUtils.Søknadsresultat::class)
    fun `kombinerSøknadsresultater skal alltid returnere innholdet hvis det bare 1 resultat i lista`(søknadsresultat: BehandlingsresultatUtils.Søknadsresultat) {
        val listeMedSøknadsresultat = listOf(søknadsresultat)

        val kombinertResultat = listeMedSøknadsresultat.kombinerSøknadsresultater()

        assertThat(kombinertResultat, Is(søknadsresultat))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingsresultatUtils.Søknadsresultat::class, names = ["INNVILGET", "AVSLÅTT"])
    fun `kombinerSøknadsresultater skal ignorere INGEN_RELEVANTE_ENDRINGER dersom den er paret opp med INNVILGET eller AVSLÅTT`(søknadsresultat: BehandlingsresultatUtils.Søknadsresultat) {
        val listeMedSøknadsresultat = listOf(BehandlingsresultatUtils.Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, søknadsresultat)

        val kombinertResultat = listeMedSøknadsresultat.kombinerSøknadsresultater()

        assertThat(kombinertResultat, Is(søknadsresultat))
    }

    @Test
    fun `kombinerSøknadsresultater skal returnere DELVIS_INNVILGET dersom lista består av INNVILGET OG AVSLÅTT`() {
        val listeMedSøknadsresultat = listOf(BehandlingsresultatUtils.Søknadsresultat.INNVILGET, BehandlingsresultatUtils.Søknadsresultat.AVSLÅTT)

        val kombinertResultat = listeMedSøknadsresultat.kombinerSøknadsresultater()

        assertThat(kombinertResultat, Is(BehandlingsresultatUtils.Søknadsresultat.DELVIS_INNVILGET))
    }
}

private fun lagYtelsePerson(
    resultat: YtelsePersonResultat,
    ytelseSlutt: YearMonth? = YearMonth.now().minusMonths(2)
): YtelsePerson {
    val ytelseType = YtelseType.ORDINÆR_BARNETRYGD
    val kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE, KravOpprinnelse.INNEVÆRENDE)
    return YtelsePerson(
        aktør = randomAktør(),
        ytelseType = ytelseType,
        kravOpprinnelse = kravOpprinnelse,
        resultater = setOf(resultat),
        ytelseSlutt = ytelseSlutt
    )
}
