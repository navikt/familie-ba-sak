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
        val behandlinger: List<RestUtvidetBehandling>)

fun Fagsak.tilRestFagsak(restUtvidetBehandlinger: List<RestUtvidetBehandling>) = RestFagsak(
        opprettetTidspunkt = this.opprettetTidspunkt,
        id = this.id,
        søkerFødselsnummer = this.hentAktivIdent().ident,
        status = this.status,
        underBehandling = restUtvidetBehandlinger.any {
            it.status == BehandlingStatus.UTREDES || (it.steg >= StegType.BESLUTTE_VEDTAK && it.steg != StegType.BEHANDLING_AVSLUTTET)
        },
        behandlinger = restUtvidetBehandlinger
)