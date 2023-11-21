package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjær
import java.time.LocalDate
import java.time.YearMonth

fun List<LocalDate>.tilSammenslåttKortString(): String = Utils.slåSammen(this.sorted().map { it.tilKortString() })

fun Collection<Kompetanse>.hentIPeriode(
    fom: YearMonth?,
    tom: YearMonth?,
): Collection<Kompetanse> =
    tilSeparateTidslinjerForBarna().mapValues { (_, tidslinje) ->
        tidslinje.beskjær(
            fraOgMed = fom.tilTidspunktEllerUendeligTidlig(tom),
            tilOgMed = tom.tilTidspunktEllerUendeligSent(fom),
        )
    }.tilSkjemaer()
