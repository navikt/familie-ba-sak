package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.PeriodeKombinator

enum class KompetanseValidering {
    OK_EØS_OG_KOMPETANSE,
    FEIL_EØS_PERIODE_UTEN_KOMPETANSE,
    FEIL_KOMPETANSE_UTEN_EØS_PERIODE,
    FEIL_UKJENT,
    OK_IKKE_EØS_OG_UTEN_KOMPETANSE
}

class KompetanseValideringTidslinje(
    private val erEøsPeriodeTidslinje: Tidslinje<Boolean>,
    private val kompetanseTidslinje: Tidslinje<Kompetanse>
) : KalkulerendeTidslinje<KompetanseValidering>(
    erEøsPeriodeTidslinje,
    kompetanseTidslinje
) {
    val kombinator = KompetanseValideringKombinator()
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): KompetanseValidering {
        return kombinator.kombiner(
            kompetanseTidslinje.hentUtsnitt(tidspunkt),
            erEøsPeriodeTidslinje.hentUtsnitt(tidspunkt)
        )
    }
}

class KompetanseValideringKombinator : PeriodeKombinator<Kompetanse, Boolean, KompetanseValidering> {

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
