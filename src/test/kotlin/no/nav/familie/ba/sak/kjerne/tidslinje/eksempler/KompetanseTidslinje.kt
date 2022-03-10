package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseDto
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.PersonIdentDto
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.PeriodeInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.SelvbyggerTema
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt

class KompetanseTidslinje(
    private val kompetanseService: KompetanseService,
    private val erEøsPeriodeTidslinje: Tidslinje<Boolean>,
    private val behandlingId: Long,
    private val personIdentDto: PersonIdentDto
) : KalkulerendeTidslinje<KompetanseDto>(erEøsPeriodeTidslinje) {

    val kompetanser = kompetanseService.hentKompetanser(behandlingId)
        .filter { it.barn.contains(personIdentDto) }
        .map { Periode(Tidspunkt(it.fom!!), Tidspunkt(it.tom!!), it) }
        .let { SelvbyggerTema(it) }

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<KompetanseDto> {
        val eøsUtsnitt = erEøsPeriodeTidslinje.hentUtsnitt(tidspunkt)
        val kompetanseUtsnitt = kompetanser.hentUtsnitt(tidspunkt)
        return PeriodeInnhold(kompetanseUtsnitt.innhold, listOf(eøsUtsnitt.id))
    }
}
