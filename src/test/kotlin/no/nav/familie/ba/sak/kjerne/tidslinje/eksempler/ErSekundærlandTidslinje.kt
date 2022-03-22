package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.ToveisKombinator
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import java.time.YearMonth

class ErSekundærlandTidslinje(
    val kompetanseTidslinje: Tidslinje<Kompetanse, YearMonth>,
    val validertKompetanseTidsline: Tidslinje<KompetanseValidering, YearMonth>
) : TidslinjeSomStykkerOppTiden<Boolean, YearMonth>(
    kompetanseTidslinje, validertKompetanseTidsline
) {
    val erSekundærlandKombinator = ErSekundærlandKombinator()

    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<YearMonth>): Boolean? {
        return erSekundærlandKombinator.kombiner(
            kompetanseTidslinje.hentUtsnitt(tidspunkt),
            validertKompetanseTidsline.hentUtsnitt(tidspunkt)
        )
    }
}

class ErSekundærlandKombinator : ToveisKombinator<Kompetanse, KompetanseValidering, Boolean> {
    override fun kombiner(kompetanse: Kompetanse?, validering: KompetanseValidering?): Boolean {
        val kompetanse = kompetanse
        val validertKompetanse = validering

        val erValidert = validertKompetanse == KompetanseValidering.OK_EØS_OG_KOMPETANSE
        val erSekundærland = kompetanse?.sekundærland == "NORGE"

        return erValidert && erSekundærland
    }
}
