package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.PeriodeInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt

class BarnetsUtbetalingerTidslinje(
    private val unikId: String,
    private val barnetsYtelseTidslinje: Tidslinje<YtelseType>,
    private val erBarnetUnder6ÅrTidslinje: Tidslinje<Boolean>
) : KalkulerendeTidslinje<Int>(barnetsYtelseTidslinje, erBarnetUnder6ÅrTidslinje) {

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<Int> {
        val erUnder6ÅrFragment = erBarnetUnder6ÅrTidslinje.hentUtsnitt(tidspunkt)
        val erOrdinærBarnetrygdFragment = barnetsYtelseTidslinje.hentUtsnitt(tidspunkt)
        val satstype = when {
            erUnder6ÅrFragment.innhold == true && erOrdinærBarnetrygdFragment.innhold == YtelseType.ORDINÆR_BARNETRYGD -> SatsType.TILLEGG_ORBA
            erOrdinærBarnetrygdFragment.innhold == YtelseType.ORDINÆR_BARNETRYGD -> SatsType.ORBA
            else -> null
        }

        val sats =
            satstype?.let { SatsService.hentGyldigSatsFor(it, tidspunkt.tilYearMonth(), tidspunkt.tilYearMonth()) }
                ?.first()?.sats ?: 0

        return PeriodeInnhold(
            innhold = sats,
            avhengerAv = listOf(erUnder6ÅrFragment.id, erOrdinærBarnetrygdFragment.id)
        )
    }
}
