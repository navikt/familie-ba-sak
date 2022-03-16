package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.PeriodeKombinator
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment

class ErSekundærlandTidslinje(
    val kompetanseTidslinje: Tidslinje<Kompetanse>,
    val validertKompetanseTidsline: Tidslinje<KompetanseValidering>
) : KalkulerendeTidslinje<Boolean>(
    kompetanseTidslinje, validertKompetanseTidsline
) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): Boolean? {
        val kompetanse = kompetanseTidslinje.hentUtsnitt(tidspunkt)
        val validertKompetanse = validertKompetanseTidsline.hentUtsnitt(tidspunkt)

        val erValidert = validertKompetanse == KompetanseValidering.OK_EØS_OG_KOMPETANSE
        val erSekundærland = kompetanse?.sekundærland == "Norge"

        return erValidert && erSekundærland
    }
}

class ErSekundærlandKombinator : PeriodeKombinator<Kompetanse, KompetanseValidering, Boolean> {
    override fun combine(
        intervall: LocalDateInterval?,
        kompetanseSegment: LocalDateSegment<Kompetanse>?,
        valideringSegment: LocalDateSegment<KompetanseValidering>?
    ): LocalDateSegment<Boolean> {
        val kompetanse = kompetanseSegment?.value
        val validertKompetanse = valideringSegment?.value

        val erValidert = validertKompetanse == KompetanseValidering.OK_EØS_OG_KOMPETANSE
        val erSekundærland = kompetanse?.sekundærland == "Norge"

        return LocalDateSegment(intervall, erValidert && erSekundærland)
    }
}
