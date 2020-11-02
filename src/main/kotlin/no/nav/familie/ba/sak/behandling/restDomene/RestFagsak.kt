package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.steg.StegType
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
        underBehandling = restBehandlinger.any {
            it.status == BehandlingStatus.UTREDES || (it.gjeldendeSteg >= StegType.BESLUTTE_VEDTAK && it.gjeldendeSteg != StegType.BEHANDLING_AVSLUTTET)
        },
        behandlinger = restBehandlinger
)