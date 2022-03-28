package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus

fun Collection<Kompetanse>.vurderStatus(): Collection<Kompetanse> =
    this.map { revurderStatus(it) }

fun revurderStatus(kompetanse: Kompetanse): Kompetanse {
    val sum = (kompetanse.annenForeldersAktivitet?.let { 1 } ?: 0) +
        (kompetanse.barnetsBostedsland?.let { 1 } ?: 0) +
        (kompetanse.primærland?.let { 1 } ?: 0) +
        (kompetanse.sekundærland?.let { 1 } ?: 0) +
        (kompetanse.søkersAktivitet?.let { 1 } ?: 0)

    val nyStatus = when (sum) {
        5 -> KompetanseStatus.OK
        in 1..4 -> KompetanseStatus.UFULLSTENDIG
        else -> KompetanseStatus.IKKE_UTFYLT
    }

    kompetanse.status = nyStatus
    return kompetanse
}
