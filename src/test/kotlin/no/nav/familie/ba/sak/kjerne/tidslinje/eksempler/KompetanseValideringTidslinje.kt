package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.hentUtsnitt

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
    erEøsPeriodeTidslinje,
    kompetanseTidslinje
) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): KompetanseValidering {
        val erEøsPeriode = erEøsPeriodeTidslinje.hentUtsnitt(tidspunkt) ?: false
        val kompetanseStatus = kompetanseTidslinje.hentUtsnitt(tidspunkt)?.status ?: KompetanseStatus.IKKE_UTFYLT

        val validering = when {
            erEøsPeriode && kompetanseStatus == KompetanseStatus.OK ->
                KompetanseValidering.OK
            !erEøsPeriode && kompetanseStatus != KompetanseStatus.IKKE_UTFYLT ->
                KompetanseValidering.KOMPETANSE_UTEN_EØS_PERIODE
            erEøsPeriode && kompetanseStatus != KompetanseStatus.OK ->
                KompetanseValidering.EØS_PERIODE_UTEN_KOMPETANSE
            else -> KompetanseValidering.UKJENT_FEIL
        }

        return validering
    }
}
