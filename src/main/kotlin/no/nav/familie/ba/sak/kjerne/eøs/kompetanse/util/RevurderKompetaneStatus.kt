package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus

fun Collection<Kompetanse>.vurderStatus(): Collection<Kompetanse> =
    this.map { revurderStatus(it) }

fun revurderStatus(kompetanse: Kompetanse): Kompetanse {
    kompetanse.status = kompetanse.vurderStatus()
    return kompetanse
}

fun Kompetanse.vurderStatus(): KompetanseStatus {
    val sum = (this.annenForeldersAktivitet?.let { 1 } ?: 0) +
        (this.barnetsBostedsland?.let { 1 } ?: 0) +
        (this.annenForeldersAktivitetsland?.let { 1 } ?: 0) +
        (this.resultat?.let { 1 } ?: 0) +
        (this.søkersAktivitet?.let { 1 } ?: 0)

    return when (sum) {
        5 -> KompetanseStatus.OK
        in 1..4 -> KompetanseStatus.UFULLSTENDIG
        else -> KompetanseStatus.IKKE_UTFYLT
    }
}
