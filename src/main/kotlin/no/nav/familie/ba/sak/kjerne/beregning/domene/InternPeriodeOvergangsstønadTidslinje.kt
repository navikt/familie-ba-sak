package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil

open class InternPeriodeOvergangsstønadTidslinje(
    private val internePeriodeOvergangsstønader: List<InternPeriodeOvergangsstønad>,
) : Tidslinje<InternPeriodeOvergangsstønad, Dag>() {

    override fun lagPerioder(): List<Periode<InternPeriodeOvergangsstønad, Dag>> {
        val erOvergangsstønadForMerEnnEnPerson = internePeriodeOvergangsstønader.map { it.personIdent }.toSet().size > 1
        if (erOvergangsstønadForMerEnnEnPerson) throw Feil("Fant overgangsstønad for mer enn en person.")

        return internePeriodeOvergangsstønader.map {
            Periode(
                fraOgMed = it.fomDato.tilTidspunktEllerUendeligLengeSiden(),
                tilOgMed = it.tomDato.tilTidspunktEllerUendeligLengeTil(),
                innhold = it
            )
        }
    }
}

fun Tidslinje<InternPeriodeOvergangsstønad, Dag>.lagInternePerioderOvergangsstønad(): List<InternPeriodeOvergangsstønad> =
    this.perioder().mapNotNull {
        it.innhold?.copy(
            fomDato = it.fraOgMed.tilFørsteDagIMåneden().tilLocalDate(),
            tomDato = it.tilOgMed.tilSisteDagIMåneden().tilLocalDate()
        )
    }
