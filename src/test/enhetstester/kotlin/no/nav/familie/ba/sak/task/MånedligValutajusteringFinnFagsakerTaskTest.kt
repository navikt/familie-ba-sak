package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class MånedligValutajusteringFinnFagsakerTaskTest {
    @Test
    fun `erSekundærlandIMåned skal bli true om vi har sekundærland i justeringsmåned`() {
        val justeringsmåned = YearMonth.now()

        val kompetanser =
            listOf(
                lagKompetanse(
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    fom = justeringsmåned,
                    tom = justeringsmåned,
                ),
            )

        val erSekundærlandIMåned = MånedligValutajusteringFinnFagsakerTask.erSekundærlandIMåned(kompetanser, justeringsmåned)

        assertThat(erSekundærlandIMåned).isTrue()
    }

    @Test
    fun `erSekundærlandIMåned skal bli false om vi ikke har sekundærland i justeringsmåned`() {
        val justeringsmåned = YearMonth.now()

        val kompetanser =
            listOf(
                lagKompetanse(
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    fom = justeringsmåned.minusYears(1),
                    tom = justeringsmåned.minusMonths(1),
                ),
            )

        val erSekundærlandIMåned = MånedligValutajusteringFinnFagsakerTask.erSekundærlandIMåned(kompetanser, justeringsmåned)

        assertThat(erSekundærlandIMåned).isFalse()
    }
}
