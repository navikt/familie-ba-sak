package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning

import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class KompetanseTidslinjeTest {

    @Test
    fun `skal håndtere to påfølgende perioder i fremtiden, men de komprimeres ikke`() {

        val barn = lagPerson(type = PersonType.BARN)
        val kompetanse1 = Kompetanse(
            fom = YearMonth.of(2437, 2),
            tom = YearMonth.of(2438, 6),
            barnAktører = setOf(barn.aktør)
        )
        val kompetanse2 = Kompetanse(
            fom = YearMonth.of(2438, 7),
            tom = null,
            barnAktører = setOf(barn.aktør)
        )

        val kompetanseTidslinje = KompetanseTidslinje(kompetanse1, kompetanse2)
        assertEquals(2, kompetanseTidslinje.perioder().size)
        assertEquals(feb(2437), kompetanseTidslinje.fraOgMed())
        assertEquals(jul(2438).somUendeligLengeTil(), kompetanseTidslinje.tilOgMed())
    }

    @Test
    fun `skal håndtere kompetanse som mangler både fom og tom`() {

        val barn = lagPerson(type = PersonType.BARN)
        val kompetanse = Kompetanse(
            fom = null,
            tom = null,
            barnAktører = setOf(barn.aktør)
        )

        val kompetanseTidslinje = KompetanseTidslinje(kompetanse)
        assertEquals(1, kompetanseTidslinje.perioder().size)
        assertEquals(MånedTidspunkt.nå().somUendeligLengeSiden(), kompetanseTidslinje.fraOgMed())
        assertEquals(MånedTidspunkt.nå().somUendeligLengeTil(), kompetanseTidslinje.tilOgMed())
    }
}
