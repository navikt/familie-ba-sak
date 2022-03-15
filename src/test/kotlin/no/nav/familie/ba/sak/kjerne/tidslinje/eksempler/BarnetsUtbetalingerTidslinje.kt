package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.hentUtsnitt

class BarnetsUtbetalingerTidslinje(
    private val barnetsYtelseTidslinje: Tidslinje<YtelseType>,
    private val erBarnetUnder6ÅrTidslinje: Tidslinje<Boolean>
) : KalkulerendeTidslinje<Int>(barnetsYtelseTidslinje, erBarnetUnder6ÅrTidslinje) {

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): Int {
        val erUnder6År = erBarnetUnder6ÅrTidslinje.hentUtsnitt(tidspunkt)
        val erOrdinærBarnetrygd = barnetsYtelseTidslinje.hentUtsnitt(tidspunkt)
        val satstype = when {
            erUnder6År == true && erOrdinærBarnetrygd == YtelseType.ORDINÆR_BARNETRYGD -> SatsType.TILLEGG_ORBA
            erOrdinærBarnetrygd == YtelseType.ORDINÆR_BARNETRYGD -> SatsType.ORBA
            else -> null
        }

        val sats =
            satstype?.let { SatsService.hentGyldigSatsFor(it, tidspunkt.tilYearMonth(), tidspunkt.tilYearMonth()) }
                ?.first()?.sats ?: 0

        return sats
    }
}
