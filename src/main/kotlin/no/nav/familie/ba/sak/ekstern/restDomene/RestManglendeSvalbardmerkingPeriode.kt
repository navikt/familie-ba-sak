package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.Utils.tilEtterfølgendePar
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse.GrOppholdsadresse
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import java.time.LocalDate

data class RestManglendeSvalbardmerkingPeriode(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class RestManglendeSvalbardmerking(
    val ident: String,
    val manglendeSvalbardmerkingPerioder: List<RestManglendeSvalbardmerkingPeriode>,
)

fun List<GrOppholdsadresse>.tilSvalbardOppholdTidslinje(): Tidslinje<GrOppholdsadresse> =
    this
        .filter { it.erPåSvalbard() }
        .sortedBy { it.periode?.fom }
        .tilEtterfølgendePar { grOppholdsadresse, nesteGrOppholdsadresse ->
            Periode(
                verdi = grOppholdsadresse,
                fom = grOppholdsadresse.periode?.fom,
                tom = grOppholdsadresse.periode?.tom ?: nesteGrOppholdsadresse?.periode?.fom?.minusDays(1),
            )
        }.tilTidslinje()
