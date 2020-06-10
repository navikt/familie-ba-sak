package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import java.time.LocalDateTime

data class RestTotrinnskontroll(
        val saksbehandler: String,
        val beslutter: String? = null,
        val godkjent: Boolean = false,
        val opprettetTidspunkt: LocalDateTime
)


fun Totrinnskontroll.toRestTotrinnskontroll() = RestTotrinnskontroll(
    saksbehandler = this.saksbehandler,
    beslutter = this.beslutter,
    godkjent = this.godkjent,
    opprettetTidspunkt = this.opprettetTidspunkt
)