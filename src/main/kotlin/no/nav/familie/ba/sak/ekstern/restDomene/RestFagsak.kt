package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.tilbakekreving.RestTilbakekrevingsbehandling
import java.time.LocalDateTime

interface RestBaseFagsak {
    val opprettetTidspunkt: LocalDateTime
    val id: Long
    val søkerFødselsnummer: String
    val status: FagsakStatus
    val underBehandling: Boolean
}

data class RestFagsak(
    override val opprettetTidspunkt: LocalDateTime,
    override val id: Long,
    override val søkerFødselsnummer: String,
    override val status: FagsakStatus,
    override val underBehandling: Boolean,
    val behandlinger: List<RestUtvidetBehandling>,
    val gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
    val tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>
) : RestBaseFagsak

fun Fagsak.tilRestFagsak(
    restUtvidetBehandlinger: List<RestUtvidetBehandling>,
    gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
    tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>
) = RestFagsak(
    opprettetTidspunkt = this.opprettetTidspunkt,
    id = this.id,
    søkerFødselsnummer = this.hentAktivIdent().ident,
    status = this.status,
    underBehandling = restUtvidetBehandlinger.any {
        it.status == BehandlingStatus.UTREDES || (it.steg >= StegType.BESLUTTE_VEDTAK && it.steg != StegType.BEHANDLING_AVSLUTTET)
    },
    behandlinger = restUtvidetBehandlinger,
    gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder,
    tilbakekrevingsbehandlinger = tilbakekrevingsbehandlinger,
)

data class RestMinimalFagsak(
    override val opprettetTidspunkt: LocalDateTime,
    override val id: Long,
    override val søkerFødselsnummer: String,
    override val status: FagsakStatus,
    override val underBehandling: Boolean,
    val behandlinger: List<RestVisningBehandling>,
    val tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>,
    val gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
) : RestBaseFagsak

fun Fagsak.tilRestMinimalFagsak(
    underBehandling: Boolean,
    restVisningBehandlinger: List<RestVisningBehandling>,
    gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
    tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>
) = RestMinimalFagsak(
    opprettetTidspunkt = this.opprettetTidspunkt,
    id = this.id,
    søkerFødselsnummer = this.hentAktivIdent().ident,
    status = this.status,
    underBehandling = underBehandling,
    behandlinger = restVisningBehandlinger,
    tilbakekrevingsbehandlinger = tilbakekrevingsbehandlinger,
    gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder
)
