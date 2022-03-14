package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.PeriodeInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.erSantHvis

class ErSekundærlandTidslinje(
    val kompetanseTidslinje: Tidslinje<Kompetanse>,
    val validertKompetanseTidsline: Tidslinje<KompetanseValidering>
) : KalkulerendeTidslinje<Boolean>(
    kompetanseTidslinje, validertKompetanseTidsline
) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<Boolean> {
        val kompetanseUtsnitt = kompetanseTidslinje.hentUtsnitt(tidspunkt)
        val validertKompetanseUtsnitt = validertKompetanseTidsline.hentUtsnitt(tidspunkt)

        val erValidert = validertKompetanseUtsnitt.erSantHvis { it == KompetanseValidering.OK }
        val erSekundærland = kompetanseUtsnitt.erSantHvis { it.sekundærland == "Norge" }
        return PeriodeInnhold(
            erValidert && erSekundærland,
            kompetanseUtsnitt, validertKompetanseUtsnitt
        )
    }
}
