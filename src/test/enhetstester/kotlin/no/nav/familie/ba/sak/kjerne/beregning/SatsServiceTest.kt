package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.spyk
import no.nav.familie.ba.sak.common.dato
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.kjerne.beregning.domene.Sats
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerNull
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SatsServiceTest {

    private val satsService = spyk(SatsService)

    private val MAX_GYLDIG_FRA_OG_MED = årMnd("2020-05")

    @Test
    fun `Skal bruke sats for satstype som er løpende`() {
        stubSatsRepo(
            SatsType.ORBA,
            TestKrPeriode(1054, "2018-04-01", null)
        )

        val beløpperioder =
            satsService.hentGyldigSatsFor(SatsType.ORBA, årMnd("2020-04"), årMnd("2038-03"), MAX_GYLDIG_FRA_OG_MED)

        Assertions.assertEquals(1, beløpperioder.size)

        assertSatsperioder(TestKrPeriode(1054, "2020-04", "2038-03"), beløpperioder[0])
    }

    @Test
    fun `Skal ikke rgi beløpsperioder utenfor`() {
        stubSatsRepo(
            SatsType.ORBA,
            TestKrPeriode(1054, "2018-04-01", null)
        )

        val beløpperioder =
            satsService.hentGyldigSatsFor(SatsType.ORBA, årMnd("2020-04"), årMnd("2038-03"), MAX_GYLDIG_FRA_OG_MED)

        Assertions.assertEquals(1, beløpperioder.size)

        assertSatsperioder(TestKrPeriode(1054, "2020-04", "2038-03"), beløpperioder[0])
    }

    @Test
    fun `Skal bruke gjeldende, ikke avlsuttet sats selv om annen sats finnes for perioden`() {
        stubSatsRepo(
            SatsType.ORBA,
            TestKrPeriode(1054, "2018-04-01", null),
            TestKrPeriode(1354, "2020-07-01", null)
        )

        val beløpperioder =
            satsService.hentGyldigSatsFor(SatsType.ORBA, årMnd("2020-04"), årMnd("2038-03"), MAX_GYLDIG_FRA_OG_MED)

        Assertions.assertEquals(1, beløpperioder.size)
        assertSatsperioder(TestKrPeriode(1054, "2020-04", "2038-03"), beløpperioder[0])
    }

    @Test
    fun `Skal stoppe når sats utløper`() {
        stubSatsRepo(
            SatsType.ORBA,
            TestKrPeriode(1054, "2018-04-01", "2023-06-30")
        )

        val beløpperioder =
            satsService.hentGyldigSatsFor(SatsType.ORBA, årMnd("2020-04"), årMnd("2038-03"), MAX_GYLDIG_FRA_OG_MED)

        Assertions.assertEquals(1, beløpperioder.size)
        assertSatsperioder(TestKrPeriode(1054, "2020-04", "2023-06"), beløpperioder[0])
    }

    @Test
    fun `Skal stoppe når sats utløper, også om det finnes ny sats etter cut-of`() {
        stubSatsRepo(
            SatsType.ORBA,
            TestKrPeriode(1054, "2019-03-01", "2020-08-31"),
            TestKrPeriode(1054, "2020-09-01", null)
        )

        val beløpperioder =
            satsService.hentGyldigSatsFor(SatsType.ORBA, årMnd("2020-04"), årMnd("2038-03"), MAX_GYLDIG_FRA_OG_MED)

        Assertions.assertEquals(1, beløpperioder.size)
        assertSatsperioder(TestKrPeriode(1054, "2020-04", "2020-08"), beløpperioder[0])
    }

    @Test
    fun `Skal splitte for flere satser i fortiden`() {
        stubSatsRepo(
            SatsType.ORBA,
            TestKrPeriode(970, "2015-04-01", "2019-02-28"),
            TestKrPeriode(1054, "2019-03-01", null)
        )

        val beløpperioder =
            satsService.hentGyldigSatsFor(SatsType.ORBA, årMnd("2017-04"), årMnd("2035-03"), MAX_GYLDIG_FRA_OG_MED)

        Assertions.assertEquals(2, beløpperioder.size)
        assertSatsperioder(TestKrPeriode(970, "2017-04", "2019-02"), beløpperioder[0])
        assertSatsperioder(TestKrPeriode(1054, "2019-03", "2035-03"), beløpperioder[1])
    }

    @Test
    fun `Skal splitte for flere satser i fortiden, men ikke for fremtiden`() {
        stubSatsRepo(
            SatsType.ORBA,
            TestKrPeriode(970, "2015-04-01", "2019-02-28"),
            TestKrPeriode(1054, "2019-03-01", "2020-08-31"),
            TestKrPeriode(1354, "2020-09-01", null)
        )

        val beløpperioder =
            satsService.hentGyldigSatsFor(SatsType.ORBA, årMnd("2017-04"), årMnd("2035-03"), MAX_GYLDIG_FRA_OG_MED)

        Assertions.assertEquals(2, beløpperioder.size)
        assertSatsperioder(TestKrPeriode(970, "2017-04", "2019-02"), beløpperioder[0])
        assertSatsperioder(TestKrPeriode(1054, "2019-03", "2020-08"), beløpperioder[1])
    }

    @Test
    fun `Skal gi tomme beløpsperioder om det ikke er overlapp mellom perioder for sats og søknad`() {
        stubSatsRepo(
            SatsType.ORBA,
            TestKrPeriode(970, "2015-04-01", "2019-02-28")
        )

        val beløpperioder =
            satsService.hentGyldigSatsFor(SatsType.ORBA, årMnd("2019-03"), årMnd("2035-03"), MAX_GYLDIG_FRA_OG_MED)

        Assertions.assertEquals(0, beløpperioder.size)
    }

    @Test
    fun `Skal gi tomme beløpsperioder om det sats har fom etter tom (altså ugyldig)`() {
        stubSatsRepo(
            SatsType.ORBA,
            TestKrPeriode(970, "2020-04-01", "2019-02-28")
        )

        val beløpperioder =
            satsService.hentGyldigSatsFor(SatsType.ORBA, årMnd("2019-03"), årMnd("2035-03"), MAX_GYLDIG_FRA_OG_MED)

        Assertions.assertEquals(0, beløpperioder.size)
    }

    @Test
    fun `Skal gi tomme beløpsperioder om det søknad har fom etter tom (altså ugyldig)`() {
        stubSatsRepo(
            SatsType.ORBA,
            TestKrPeriode(1054, "2019-03-01", null)
        )

        val beløpperioder =
            satsService.hentGyldigSatsFor(SatsType.ORBA, årMnd("2023-03"), årMnd("2020-03"), MAX_GYLDIG_FRA_OG_MED)

        Assertions.assertEquals(0, beløpperioder.size)
    }

    @Test
    fun `Skal gi tomme beløpsperioder om satstype ikke finnes`() {
        stubSatsRepo(SatsType.ORBA)

        val beløpperioder =
            satsService.hentGyldigSatsFor(SatsType.ORBA, årMnd("2020-03"), årMnd("2038-02"), MAX_GYLDIG_FRA_OG_MED)

        Assertions.assertEquals(0, beløpperioder.size)
    }

    @Test
    fun `Skal opprette korrekt tidslinje for ordinær barnetrygd for barn`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2017, 4, 6))

        val ordinærTidslinje = lagOrdinærTidslinje(barn)
        val ordinærePerioder = ordinærTidslinje.perioder().toList()

        Assertions.assertEquals(6, ordinærePerioder.size)

        assertPeriode(TestKrPeriode(beløp = 970, fom = "2017-04", tom = "2019-02"), ordinærePerioder[0])
        assertPeriode(TestKrPeriode(beløp = 1054, fom = "2019-03", tom = "2020-08"), ordinærePerioder[1])
        assertPeriode(TestKrPeriode(beløp = 1354, fom = "2020-09", tom = "2021-08"), ordinærePerioder[2])
        assertPeriode(TestKrPeriode(beløp = 1654, fom = "2021-09", tom = "2021-12"), ordinærePerioder[3])
        assertPeriode(TestKrPeriode(beløp = 1676, fom = "2022-01", tom = "2023-03"), ordinærePerioder[4])
        assertPeriode(TestKrPeriode(beløp = 1054, fom = "2023-04", tom = null), ordinærePerioder[5])
    }

    private fun assertPeriode(forventet: TestKrPeriode, faktisk: no.nav.familie.ba.sak.kjerne.tidslinje.Periode<Int, Måned>) {
        Assertions.assertEquals(forventet.beløp, faktisk.innhold, "Forskjell i beløp")
        Assertions.assertEquals(forventet.fom?.let { årMnd(it) }, faktisk.fraOgMed.tilYearMonthEllerNull(), "Forskjell i fra-og-med")
        Assertions.assertEquals(forventet.tom?.let { årMnd(it) }, faktisk.tilOgMed.tilYearMonthEllerNull(), "Forskjell i til-og-med")
    }

    private fun assertSatsperioder(forventet: TestKrPeriode, faktisk: SatsService.SatsPeriode) {
        Assertions.assertEquals(forventet.beløp, faktisk.sats, "Forskjell i beløp")
        Assertions.assertEquals(forventet.fom?.let { årMnd(it) }, faktisk.fraOgMed, "Forskjell i fra-og-med")
        Assertions.assertEquals(forventet.tom?.let { årMnd(it) }, faktisk.tilOgMed, "Forskjell i til-og-med")
    }

    private fun stubSatsRepo(type: SatsType, vararg satser: TestKrPeriode) {
        every { satsService["finnAlleSatserFor"](type) } returns
            satser.asList()
                .map {
                    Sats(
                        type,
                        it.beløp,
                        it.fom?.let { s -> dato(s) } ?: LocalDate.MIN,
                        it.tom?.let { s -> dato(s) } ?: LocalDate.MAX
                    )
                }
    }

    private data class TestKrPeriode(
        val beløp: Int,
        val fom: String?,
        val tom: String?
    )
}
