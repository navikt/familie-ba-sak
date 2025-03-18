package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.tidslinje.PRAKTISK_SENESTE_DAG
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class SkjemaTidslinjeTest {
    @Test
    fun `skal håndtere to påfølgende perioder i fremtiden`() {
        val barn = lagPerson(type = PersonType.BARN)
        val kompetanse1 =
            Kompetanse(
                fom = YearMonth.of(2437, 2),
                tom = YearMonth.of(2438, 6),
                barnAktører = setOf(barn.aktør),
            )
        val kompetanse2 =
            Kompetanse(
                fom = YearMonth.of(2438, 7),
                tom = null,
                barnAktører = setOf(barn.aktør),
            )

        val kompetanseTidslinje = listOf(kompetanse1, kompetanse2).tilTidslinje()
        assertEquals(1, kompetanseTidslinje.tilPerioder().size)
        assertEquals(1.feb(2437), kompetanseTidslinje.startsTidspunkt)
        assertEquals(PRAKTISK_SENESTE_DAG, kompetanseTidslinje.kalkulerSluttTidspunkt())
    }

    @Test
    fun `skal håndtere kompetanse som mangler både fom og tom`() {
        val barn = lagPerson(type = PersonType.BARN)
        val kompetanse =
            Kompetanse(
                fom = null,
                tom = null,
                barnAktører = setOf(barn.aktør),
            )

        val kompetanseTidslinje = kompetanse.tilTidslinje()
        assertEquals(1, kompetanseTidslinje.tilPerioder().size)
        assertEquals(PRAKTISK_TIDLIGSTE_DAG, kompetanseTidslinje.startsTidspunkt)
        assertEquals(PRAKTISK_SENESTE_DAG, kompetanseTidslinje.kalkulerSluttTidspunkt())
    }
}
