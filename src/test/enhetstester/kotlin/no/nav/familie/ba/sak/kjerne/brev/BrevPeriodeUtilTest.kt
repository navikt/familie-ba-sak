package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class BrevPeriodeUtilTest {
    @Test
    fun `Skal plukke ut kompetansene i perioden`() {
        val barnAktør1 = Aktør(aktørId = "1111111111111")
        val barnAktør2 = Aktør(aktørId = "2222222222222")

        val periode1 = MånedPeriode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        val periode2 = MånedPeriode(YearMonth.of(2021, 2), YearMonth.of(2021, 3))
        val periode3 = MånedPeriode(YearMonth.of(2021, 5), YearMonth.of(2021, 5))

        val kompetanse1 =
            lagKompetanse(fom = periode1.fom, tom = periode1.tom, barnAktører = setOf(barnAktør1))
        val kompetanse2 =
            lagKompetanse(
                fom = periode2.fom,
                tom = periode2.tom,
                barnAktører = setOf(barnAktør1),
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
            )
        val kompetanse3 =
            lagKompetanse(
                fom = periode2.fom,
                tom = periode3.tom,
                barnAktører = setOf(barnAktør2),
                søkersAktivitet = KompetanseAktivitet.INAKTIV,
            )
        val kompetanse4 =
            lagKompetanse(fom = periode3.fom, tom = periode3.tom, barnAktører = setOf(barnAktør1))

        Assertions.assertEquals(
            listOf(kompetanse1, kompetanse2, kompetanse3.copy(tom = periode2.tom)),
            listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)
                .hentIPeriode(periode1.fom, periode2.tom),
        )
    }
}
