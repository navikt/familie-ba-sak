package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak.SammensattKontrollsak

data class RestSammensattKontrollsak(
    val id: Long,
    val behandlingId: Long,
    val fritekst: String,
)

data class RestOpprettSammensattKontrollsak(
    val behandlingId: Long,
    val fritekst: String,
)

fun RestOpprettSammensattKontrollsak.tilSammensattKontrollsak() = SammensattKontrollsak(behandlingId = behandlingId, fritekst = fritekst)
