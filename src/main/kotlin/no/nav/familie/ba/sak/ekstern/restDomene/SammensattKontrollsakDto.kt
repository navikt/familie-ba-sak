package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak.SammensattKontrollsak

data class SammensattKontrollsakDto(
    val id: Long,
    val behandlingId: Long,
    val fritekst: String,
)

data class OpprettSammensattKontrollsakDto(
    val behandlingId: Long,
    val fritekst: String,
)

fun OpprettSammensattKontrollsakDto.tilSammensattKontrollsak() = SammensattKontrollsak(behandlingId = behandlingId, fritekst = fritekst)
