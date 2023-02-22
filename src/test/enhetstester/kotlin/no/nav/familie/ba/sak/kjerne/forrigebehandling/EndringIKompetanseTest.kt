package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class EndringIKompetanseTest {

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
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = jan22,
                tom = mai22
            )

        val perioderMedEndring = EndringIKompetanseUtil.lagEndringIKompetanseTidslinje(
            nåværendeKompetanser = listOf(forrigeKompetanse.copy().apply { behandlingId = nåværendeBehandling.id }),
            forrigeKompetanser = listOf(forrigeKompetanse)
        ).perioder().filter { it.innhold == true }

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
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = jan22,
                tom = mai22
            )

        val perioderMedEndring = EndringIKompetanseUtil.lagEndringIKompetanseTidslinje(
            nåværendeKompetanser = listOf(
                forrigeKompetanse.copy(søkersAktivitetsland = "DK").apply { behandlingId = nåværendeBehandling.id }
            ),
            forrigeKompetanser = listOf(forrigeKompetanse)
        ).perioder().filter { it.innhold == true }

        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(jan22, perioderMedEndring.single().fraOgMed.tilYearMonth())
        Assertions.assertEquals(mai22, perioderMedEndring.single().tilOgMed.tilYearMonth())
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
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null
            )

        val perioderMedEndring = EndringIKompetanseUtil.lagEndringIKompetanseTidslinje(
            nåværendeKompetanser = listOf(
                forrigeKompetanse.copy(fom = YearMonth.now().minusMonths(10))
                    .apply { behandlingId = nåværendeBehandling.id }
            ),
            forrigeKompetanser = listOf(forrigeKompetanse)
        ).perioder().filter { it.innhold == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())
    }
}
