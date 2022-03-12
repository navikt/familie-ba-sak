package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.PeriodeInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.mapInnhold

class ErSekundærlandTidslinje(
    val kompetanseTidslinje: Tidslinje<Kompetanse>
) : KalkulerendeTidslinje<Boolean>(kompetanseTidslinje) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<Boolean> {
        val periodeFragment = kompetanseTidslinje.hentUtsnitt(tidspunkt)
        return periodeFragment.mapInnhold(periodeFragment.innhold?.sekundærland == "Norge")
    }
}
