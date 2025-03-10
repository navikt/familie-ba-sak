package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagKompetanse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class EndringIKompetanseUtilTest {
    private val barn1Aktør = randomAktør()
    val jan22 = YearMonth.of(2022, 1)
    val mai22 = YearMonth.of(2022, 5)

    @Test
    fun `Endring i kompetanse - skal ikke returnere noen endrede perioder når ingenting endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = jan22,
                tom = mai22,
            )

        val nåværendeKompetanse = forrigeKompetanse.copy().apply { behandlingId = nåværendeBehandling.id }

        val perioderMedEndring =
            EndringIKompetanseUtil
                .lagEndringIKompetanseForPersonTidslinje(
                    nåværendeKompetanserForPerson = listOf(nåværendeKompetanse),
                    forrigeKompetanserForPerson = listOf(forrigeKompetanse),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i kompetanse - skal returnere endret periode når søkers aktivitetsland endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = jan22,
                tom = mai22,
            )

        val nåværendeKompetanse =
            forrigeKompetanse.copy(søkersAktivitetsland = "DK").apply { behandlingId = nåværendeBehandling.id }

        val perioderMedEndring =
            EndringIKompetanseUtil
                .lagEndringIKompetanseForPersonTidslinje(
                    nåværendeKompetanserForPerson =
                        listOf(
                            nåværendeKompetanse,
                        ),
                    forrigeKompetanserForPerson = listOf(forrigeKompetanse),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(jan22, perioderMedEndring.single().fom?.toYearMonth())
        Assertions.assertEquals(mai22, perioderMedEndring.single().tom?.toYearMonth())
    }

    @Test
    fun `Endring i kompetanse - skal ikke lage endret periode når det kun blir lagt på en ekstra kompetanseperiode`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val nåværendeKompetanse =
            forrigeKompetanse
                .copy(fom = YearMonth.now().minusMonths(10))
                .apply { behandlingId = nåværendeBehandling.id }

        val perioderMedEndring =
            EndringIKompetanseUtil
                .lagEndringIKompetanseForPersonTidslinje(
                    nåværendeKompetanserForPerson =
                        listOf(
                            nåværendeKompetanse,
                        ),
                    forrigeKompetanserForPerson = listOf(forrigeKompetanse),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i kompetanse - skal ikke lage endret periode når forrige kompetanse ikke er utfylt (pga migrering+ evt autovedtak)`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = null,
                søkersAktivitet = null,
                søkersAktivitetsland = null,
                annenForeldersAktivitet = null,
                annenForeldersAktivitetsland = null,
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val nåværendeKompetanse =
            lagKompetanse(
                behandlingId = nåværendeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val perioderMedEndring =
            EndringIKompetanseUtil
                .lagEndringIKompetanseForPersonTidslinje(
                    nåværendeKompetanserForPerson =
                        listOf(
                            nåværendeKompetanse,
                        ),
                    forrigeKompetanserForPerson = listOf(forrigeKompetanse),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())
    }
}
