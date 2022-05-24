package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassTil
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned

internal fun tilpassUtenlandskePeriodebeløpTilKompetanser(
    forrigeUtenlandskePeriodebeløp: Iterable<UtenlandskPeriodebeløp>,
    gjeldendeKompetanser: Iterable<Kompetanse>
): Collection<UtenlandskPeriodebeløp> {
    val barnasKompetanseTidslinjer = gjeldendeKompetanser
        .tilSeparateTidslinjerForBarna()
        .filtrerSekundærland()

    return forrigeUtenlandskePeriodebeløp.tilSeparateTidslinjerForBarna()
        .tilpassTil(barnasKompetanseTidslinjer) { upb, _ -> upb ?: UtenlandskPeriodebeløp.NULL }
        .tilSkjemaer()
}

fun Map<Aktør, Tidslinje<Kompetanse, Måned>>.filtrerSekundærland() =
    this.mapValues { (_, tidslinje) -> tidslinje.filtrer { it?.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND } }
