package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.PeriodeKombinator

class ErSekundærlandTidslinje(
    val kompetanseTidslinje: Tidslinje<Kompetanse>,
    val validertKompetanseTidsline: Tidslinje<KompetanseValidering>
) : KalkulerendeTidslinje<Boolean>(
    kompetanseTidslinje, validertKompetanseTidsline
) {
    val erSekundærlandKombinator = ErSekundærlandKombinator()

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): Boolean? {
        return erSekundærlandKombinator.kombiner(
            kompetanseTidslinje.hentUtsnitt(tidspunkt),
            validertKompetanseTidsline.hentUtsnitt(tidspunkt)
        )
    }
}

class ErSekundærlandKombinator : PeriodeKombinator<Kompetanse, KompetanseValidering, Boolean> {
    override fun kombiner(kompetanse: Kompetanse?, validering: KompetanseValidering?): Boolean {
        val kompetanse = kompetanse
        val validertKompetanse = validering

        val erValidert = validertKompetanse == KompetanseValidering.OK_EØS_OG_KOMPETANSE
        val erSekundærland = kompetanse?.sekundærland == "Norge"

        return erValidert && erSekundærland
    }
}
