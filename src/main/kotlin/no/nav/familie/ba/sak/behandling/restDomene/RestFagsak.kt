package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import java.time.LocalDateTime

data class RestFagsak(
        val opprettetTidspunkt: LocalDateTime,
        val id: Long,
        val søkerFødselsnummer: String,
        val status: FagsakStatus,
        val underBehandling: Boolean,
        val behandlinger: List<RestBehandling>)

fun Fagsak.toRestFagsak(restBehandlinger: List<RestBehandling>) = RestFagsak(
        opprettetTidspunkt = this.opprettetTidspunkt,
        id = this.id,
        søkerFødselsnummer = this.hentAktivIdent().ident,
        status = this.status,
        underBehandling = restBehandlinger.any { it.status == BehandlingStatus.UTREDES },
        behandlinger = restBehandlinger
)