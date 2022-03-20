package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.SnittTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import java.time.LocalDate

class ErBarnetUnder6ÅrTidslinje(
    private val barnetsYtelseTidslinje: Tidslinje<YtelseType>,
    private val barnetsFødselsdato: LocalDate
) : SnittTidslinje<Boolean>(barnetsYtelseTidslinje) {
    override fun beregnSnitt(tidspunkt: Tidspunkt): Boolean? {
        val barnetsYtelse = barnetsYtelseTidslinje.hentUtsnitt(tidspunkt)
        return barnetsYtelse
            .takeIf { it == YtelseType.ORDINÆR_BARNETRYGD }
            ?.takeIf { tidspunkt.tilYearMonth() < barnetsFødselsdato.plusYears(6).toYearMonth() }
            ?.let { true }
    }
}
