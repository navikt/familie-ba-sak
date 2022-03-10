package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.PeriodeInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.mapInnhold
import java.time.LocalDate

class ErBarnetUnder6ÅrTidslinje(
    private val barnetsYtelseTidslinje: Tidslinje<YtelseType>,
    private val barnetsFødselsdato: LocalDate
) : KalkulerendeTidslinje<Boolean>(barnetsYtelseTidslinje) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<Boolean> {
        val utsnitt = barnetsYtelseTidslinje.hentUtsnitt(tidspunkt)
        return utsnitt
            .takeIf { it.innhold == YtelseType.ORDINÆR_BARNETRYGD }
            ?.takeIf { tidspunkt.tilYearMonth() < barnetsFødselsdato.plusYears(6).toYearMonth() }
            ?.let { it.mapInnhold(true) }
            ?: utsnitt.mapInnhold(null as Boolean)
    }
}
