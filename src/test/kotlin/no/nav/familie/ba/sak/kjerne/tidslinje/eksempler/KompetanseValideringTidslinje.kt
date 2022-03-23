package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.ToveisKombinator
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

enum class KompetanseValidering {
    OK_EØS_OG_KOMPETANSE,
    FEIL_EØS_PERIODE_UTEN_KOMPETANSE,
    FEIL_KOMPETANSE_UTEN_EØS_PERIODE,
    FEIL_UKJENT,
    OK_IKKE_EØS_OG_UTEN_KOMPETANSE
}

class KompetanseValideringTidslinje(
    private val erEøsPeriodeTidslinje: Tidslinje<Boolean, Måned>,
    private val kompetanseTidslinje: Tidslinje<Kompetanse, Måned>
) : TidslinjeSomStykkerOppTiden<KompetanseValidering, Måned>(
    erEøsPeriodeTidslinje,
    kompetanseTidslinje
) {
    val kombinator = KompetanseValideringKombinator()
    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<Måned>): KompetanseValidering {
        return kombinator.kombiner(
            kompetanseTidslinje.hentUtsnitt(tidspunkt),
            erEøsPeriodeTidslinje.hentUtsnitt(tidspunkt)
        )
    }
}

class KompetanseValideringKombinator : ToveisKombinator<Kompetanse, Boolean, KompetanseValidering> {

    override fun kombiner(kompetanse: Kompetanse?, erEøs: Boolean?): KompetanseValidering {
        val erEøsPeriode = erEøs ?: false
        val kompetanseStatus = kompetanse?.status ?: KompetanseStatus.IKKE_UTFYLT

        val validering = when {
            erEøsPeriode && kompetanseStatus == KompetanseStatus.OK ->
                KompetanseValidering.OK_EØS_OG_KOMPETANSE
            !erEøsPeriode && kompetanseStatus != KompetanseStatus.IKKE_UTFYLT ->
                KompetanseValidering.FEIL_KOMPETANSE_UTEN_EØS_PERIODE
            erEøsPeriode && kompetanseStatus != KompetanseStatus.OK ->
                KompetanseValidering.FEIL_EØS_PERIODE_UTEN_KOMPETANSE
            !erEøsPeriode && kompetanseStatus == KompetanseStatus.IKKE_UTFYLT ->
                KompetanseValidering.OK_IKKE_EØS_OG_UTEN_KOMPETANSE
            else -> KompetanseValidering.FEIL_UKJENT
        }

        return validering
    }
}
