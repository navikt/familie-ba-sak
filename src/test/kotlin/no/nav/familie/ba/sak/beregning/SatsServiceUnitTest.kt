package no.nav.familie.ba.sak.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.beregning.domene.Sats
import no.nav.familie.ba.sak.beregning.domene.SatsRepository
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.common.dato
import no.nav.familie.ba.sak.common.årMnd
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SatsServiceUnitTest {


    val satsRepository = mockk<SatsRepository>()
    val satsService = SatsService(satsRepository)

    val ignorerFra_2020_05 = årMnd("2020-05")

    @Test
    fun `Skal bruke sats for satstype som er løpende`() {
        stubSatsRepo(SatsType.ORBA,
                     TestKrPeriode(1054, "2018-04-01", null)
        )

        val beløpperioder =
                satsService.hentGyldigSatsFor(SatsType.ORBA, dato("2020-04-01"), dato("2038-03-31"), ignorerFra_2020_05)

        Assertions.assertEquals(1, beløpperioder.size)

        assertSatsperioder(TestKrPeriode(1054, "2020-04", "2038-03"), beløpperioder[0])
    }

    @Test
    fun `Skal ikke rgi beløpsperioder utenfor`() {
        stubSatsRepo(SatsType.ORBA,
                     TestKrPeriode(1054, "2018-04-01", null)
        )

        val beløpperioder =
                satsService.hentGyldigSatsFor(SatsType.ORBA, dato("2020-04-01"), dato("2038-03-31"), ignorerFra_2020_05)

        Assertions.assertEquals(1, beløpperioder.size)

        assertSatsperioder(TestKrPeriode(1054, "2020-04", "2038-03"), beløpperioder[0])
    }


    @Test
    fun `Skal bruke gjeldende, ikke avlsuttet sats selv om annen sats finnes for perioden`() {
        stubSatsRepo(SatsType.ORBA,
                     TestKrPeriode(1054, "2018-04-01", null),
                     TestKrPeriode(1354, "2020-07-01", null)
        )

        val beløpperioder =
                satsService.hentGyldigSatsFor(SatsType.ORBA, dato("2020-04-01"), dato("2038-03-31"), ignorerFra_2020_05)

        Assertions.assertEquals(1, beløpperioder.size)
        assertSatsperioder(TestKrPeriode(1054, "2020-04", "2038-03"), beløpperioder[0])
    }

    @Test
    fun `Skal stoppe når sats utløper`() {
        stubSatsRepo(SatsType.ORBA,
                     TestKrPeriode(1054, "2018-04-01", "2023-06-30")
        )

        val beløpperioder =
                satsService.hentGyldigSatsFor(SatsType.ORBA, dato("2020-04-01"), dato("2038-03-31"), ignorerFra_2020_05)

        Assertions.assertEquals(1, beløpperioder.size)
        assertSatsperioder(TestKrPeriode(1054, "2020-04", "2023-06"), beløpperioder[0])
     }

    @Test
    fun `Skal stoppe når sats utløper, også om det finnes ny sats etter cut-of`() {
        stubSatsRepo(SatsType.ORBA,
                     TestKrPeriode(1054, "2019-03-01", "2020-08-31"),
                     TestKrPeriode(1054, "2020-09-01", null)
        )

        val beløpperioder =
                satsService.hentGyldigSatsFor(SatsType.ORBA, dato("2020-04-01"), dato("2038-03-31"), ignorerFra_2020_05)

        Assertions.assertEquals(1, beløpperioder.size)
        assertSatsperioder(TestKrPeriode(1054, "2020-04", "2020-08"), beløpperioder[0])
    }

    @Test
    fun `Skal splitte for flere satser i fortiden`() {
        stubSatsRepo(SatsType.ORBA,
                     TestKrPeriode(970, "2015-04-01", "2019-02-28"),
                     TestKrPeriode(1054, "2019-03-01", null)
        )

        val beløpperioder =
                satsService.hentGyldigSatsFor(SatsType.ORBA, dato("2017-04-01"), dato("2035-03-31"), ignorerFra_2020_05)

        Assertions.assertEquals(2, beløpperioder.size)
        assertSatsperioder(TestKrPeriode(970, "2017-04", "2019-02"), beløpperioder[0])
        assertSatsperioder(TestKrPeriode(1054, "2019-03", "2035-03"), beløpperioder[1])

    }

    @Test
    fun `Skal splitte for flere satser i fortiden, men ikke for fremtiden`() {
        stubSatsRepo(SatsType.ORBA,
                     TestKrPeriode(970, "2015-04-01", "2019-02-28"),
                     TestKrPeriode(1054, "2019-03-01", "2020-08-31"),
                     TestKrPeriode(1354, "2020-09-01", null)
        )

        val beløpperioder =
                satsService.hentGyldigSatsFor(SatsType.ORBA, dato("2017-04-01"), dato("2035-03-31"), ignorerFra_2020_05)

        Assertions.assertEquals(2, beløpperioder.size)
        assertSatsperioder(TestKrPeriode(970, "2017-04", "2019-02"), beløpperioder[0])
        assertSatsperioder(TestKrPeriode(1054, "2019-03", "2020-08"), beløpperioder[1])
    }

    @Test
    fun `Skal gi tomme beløpsperioder om det ikke er overlapp mellom perioder for sats og søknad`() {
        stubSatsRepo(SatsType.ORBA,
                     TestKrPeriode(970, "2015-04-01", "2019-02-28")
        )

        val beløpperioder =
                satsService.hentGyldigSatsFor(SatsType.ORBA, dato("2019-03-01"), dato("2035-03-31"), ignorerFra_2020_05)

        Assertions.assertEquals(0, beløpperioder.size)
    }

    @Test
    fun `Skal gi tomme beløpsperioder om det sats har fom etter tom (altså ugyldig)`() {
        stubSatsRepo(SatsType.ORBA,
                     TestKrPeriode(970, "2020-04-01", "2019-02-28")
        )

        val beløpperioder =
                satsService.hentGyldigSatsFor(SatsType.ORBA, dato("2019-03-01"), dato("2035-03-31"), ignorerFra_2020_05)

        Assertions.assertEquals(0, beløpperioder.size)
    }

    @Test
    fun `Skal gi tomme beløpsperioder om det søknad har fom etter tom (altså ugyldig)`() {
        stubSatsRepo(SatsType.ORBA,
                     TestKrPeriode(1054, "2019-03-01", null)
        )

        val beløpperioder =
                satsService.hentGyldigSatsFor(SatsType.ORBA, dato("2023-03-01"), dato("2020-03-31"), ignorerFra_2020_05)

        Assertions.assertEquals(0, beløpperioder.size)
    }

    @Test
    fun `Skal gi tomme beløpsperioder om satstype ikke finnes`() {
        stubSatsRepo(SatsType.ORBA)

        val beløpperioder =
                satsService.hentGyldigSatsFor(SatsType.ORBA, dato("2020-03-01"), dato("2038-02-28"), ignorerFra_2020_05)

        Assertions.assertEquals(0, beløpperioder.size)
    }

    private fun assertSatsperioder(forventet: TestKrPeriode, faktisk: SatsService.BeløpPeriode) {
        Assertions.assertEquals(forventet.beløp, faktisk.beløp, "Forskjell i beløp")
        Assertions.assertEquals(forventet.fom?.let { årMnd(it) }, faktisk.fraOgMed, "Forskjell i fra-og-med")
        Assertions.assertEquals(forventet.tom?.let { årMnd(it) }, faktisk.tilOgMed, "Forskjell i til-og-med")
    }

    private fun stubSatsRepo(type: SatsType, vararg satser: TestKrPeriode): Unit {
        every { satsRepository.finnAlleSatserFor(type) } returns
                satser.asList().map { Sats(0, type, it.beløp, it.fom?.let { s -> dato(s) }, it.tom?.let { s -> dato(s) }) }
    }

    private data class TestKrPeriode(
            val beløp: Int,
            val fom: String?,
            val tom: String?
    )

}