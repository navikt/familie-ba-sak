package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.clearStaticMockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.YearMonth
import java.util.stream.Stream

class BehandlingsresultatUtilsTest {

    val søker = tilfeldigPerson()

    private val barn1Aktør = randomAktør()

    @BeforeEach
    fun reset() {
        clearStaticMockk(YearMonth::class)
    }

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

    @ParameterizedTest(name = "Søknadsresultat {0}, Endringsresultat {1} og Opphørsresultat {2} skal kombineres til behandlingsresultat {3}")
    @MethodSource("hentKombinasjonerOgBehandlingsResultat")
    internal fun `Kombiner resultater - skal kombinere til riktig behandlingsresultat gitt forskjellige kombinasjoner av resultater`(
        søknadsresultat: Søknadsresultat?,
        endringsresultat: Endringsresultat,
        opphørsresultat: Opphørsresultat,
        behandlingsresultat: Behandlingsresultat
    ) {
        val kombinertResultat = BehandlingsresultatUtils.kombinerResultaterTilBehandlingsresultat(
            søknadsresultat,
            endringsresultat,
            opphørsresultat
        )

        assertEquals(kombinertResultat, behandlingsresultat)
    }

    companion object {
        @JvmStatic
        fun hentKombinasjonerOgBehandlingsResultat() =
            Stream.of(
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.FORTSATT_INNVILGET),
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.FORTSATT_OPPHØRT),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.INNVILGET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.INNVILGET),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.INNVILGET),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.AVSLÅTT_OG_ENDRET),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.AVSLÅTT_OG_ENDRET),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.AVSLÅTT_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.AVSLÅTT),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.AVSLÅTT),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET),
                Arguments.of(null, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.ENDRET_OG_OPPHØRT),
                Arguments.of(null, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.ENDRET_UTBETALING),
                Arguments.of(null, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.ENDRET_UTBETALING),
                Arguments.of(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.OPPHØRT),
                Arguments.of(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.FORTSATT_OPPHØRT),
                Arguments.of(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.FORTSATT_INNVILGET)
            )
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
}
