package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.PeriodeInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.IngenTidslinjeRepository

enum class KompetanseValidering {
    OK,
    EØS_PERIODE_UTEN_KOMPETANSE,
    KOMPETANSE_UTEN_EØS_PERIODE,
    UKJENT_FEIL
}

class KompetanseValideringTidslinje(
    private val erEøsPeriodeTidslinje: Tidslinje<Boolean>,
    private val kompetanseTidslinje: Tidslinje<Kompetanse>
) : KalkulerendeTidslinje<KompetanseValidering>(
    IngenTidslinjeRepository(),
    erEøsPeriodeTidslinje,
    kompetanseTidslinje
) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<KompetanseValidering> {
        val erEøsPeriodeUtsnitt = erEøsPeriodeTidslinje.hentUtsnitt(tidspunkt)
        val kompetanseUtsnitt = kompetanseTidslinje.hentUtsnitt(tidspunkt)

        val erEøsPeriode = erEøsPeriodeUtsnitt.innhold ?: false
        val kompetanseStatus = kompetanseUtsnitt.innhold?.status ?: KompetanseStatus.IKKE_UTFYLT

        val validering = when {
            erEøsPeriode && kompetanseStatus == KompetanseStatus.OK ->
                KompetanseValidering.OK
            !erEøsPeriode && kompetanseStatus != KompetanseStatus.IKKE_UTFYLT ->
                KompetanseValidering.KOMPETANSE_UTEN_EØS_PERIODE
            erEøsPeriode && kompetanseStatus != KompetanseStatus.OK ->
                KompetanseValidering.EØS_PERIODE_UTEN_KOMPETANSE
            else -> KompetanseValidering.UKJENT_FEIL
        }

        return PeriodeInnhold(validering, erEøsPeriodeUtsnitt, kompetanseUtsnitt)
    }
}
