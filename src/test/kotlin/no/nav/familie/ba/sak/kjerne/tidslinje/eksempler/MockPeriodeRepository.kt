package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.PeriodeRepository

class MockPerideRepository : PeriodeRepository {
    override fun hentPerioder(
        tidslinjeId: String,
        akseptertInnhold: List<String>?
    ): Iterable<Periode<String>> {
        TODO("Not yet implemented")
    }

    override fun lagrePerioder(
        tidslinjeId: String,
        perioder: Iterable<Periode<String>>
    ): Iterable<Periode<String>> {
        TODO("Not yet implemented")
    }
}
