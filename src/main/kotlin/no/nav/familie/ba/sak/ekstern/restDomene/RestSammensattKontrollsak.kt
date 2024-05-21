package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
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

fun RestOpprettSammensattKontrollsak.tilSammensattKontrollsak(behandling: Behandling) = SammensattKontrollsak(behandling = behandling, fritekst = fritekst)
