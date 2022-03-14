package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.hentUtsnitt

class ErSekundærlandTidslinje(
    val kompetanseTidslinje: Tidslinje<Kompetanse>,
    val validertKompetanseTidsline: Tidslinje<KompetanseValidering>
) : KalkulerendeTidslinje<Boolean>(
    kompetanseTidslinje, validertKompetanseTidsline
) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): Boolean? {
        val kompetanse = kompetanseTidslinje.hentUtsnitt(tidspunkt)
        val validertKompetanse = validertKompetanseTidsline.hentUtsnitt(tidspunkt)

        val erValidert = validertKompetanse == KompetanseValidering.OK
        val erSekundærland = kompetanse?.sekundærland == "Norge"

        return erValidert && erSekundærland
    }
}
