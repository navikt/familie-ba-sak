package no.nav.familie.ba.sak.kjerne.brev

import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException

// 410 GONE er unikt for bruker død og ingen dødsboadresse mot Dokdist
// https://nav-it.slack.com/archives/C6W9E5GPJ/p1647956660364779?thread_ts=1647936835.099329&cid=C6W9E5GPJ
fun mottakerErDødUtenDødsboadresse(exception: RestClientResponseException): Boolean = exception.statusCode == HttpStatus.GONE

// 400 BAD_REQUEST + kanal print er eneste måten å vite at bruker ikke er digital og har ukjent adresse fra Dokdist
// https://nav-it.slack.com/archives/C6W9E5GPJ/p1647947002270879?thread_ts=1647936835.099329&cid=C6W9E5GPJ
fun mottakerErIkkeDigitalOgHarUkjentAdresse(exception: RestClientResponseException) =
    exception.statusCode == HttpStatus.BAD_REQUEST &&
        exception.message?.contains("Mottaker har ukjent adresse") == true

// 409 Conflict betyr duplikatdistribusjon
// https://nav-it.slack.com/archives/C6W9E5GPJ/p1657610907144549?thread_ts=1657610829.116619&cid=C6W9E5GPJ
fun dokumentetErAlleredeDistribuert(exception: RestClientResponseException) = exception.statusCode == HttpStatus.CONFLICT
