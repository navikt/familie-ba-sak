package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.Fagsak
import java.time.LocalDateTime

data class RestFagsak(
        val opprettetTidspunkt: LocalDateTime?,
        val id: Long?,
        val søkerFødselsnummer: String?,
        val behandlinger: List<RestBehandling>)

fun Fagsak.toRestFagsak(restBehandlinger: List<RestBehandling>) = RestFagsak(
        opprettetTidspunkt = this.opprettetTidspunkt,
        id = this.id,
        søkerFødselsnummer = this.personIdent.ident,
        behandlinger = restBehandlinger
)