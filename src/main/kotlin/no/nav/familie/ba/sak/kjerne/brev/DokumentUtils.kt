package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.http.client.RessursException
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

// 410 GONE er unikt for bruker død og ingen dødsboadresse mot Dokdist
// https://nav-it.slack.com/archives/C6W9E5GPJ/p1647956660364779?thread_ts=1647936835.099329&cid=C6W9E5GPJ
fun mottakerErDødUtenDødsboadresse(ressursException: RessursException): Boolean =
    ressursException.hentStatuskodeFraOriginalFeil() == HttpStatus.GONE

fun RessursException.hentStatuskodeFraOriginalFeil(): HttpStatus {
    val cause = this.cause
    return if (cause is HttpClientErrorException) {
        cause.statusCode
    } else {
        throw this
    }
}
