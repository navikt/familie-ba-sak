package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.tilbakekreving.RestTilbakekrevingsbehandling
import java.time.LocalDateTime

open class RestBaseFagsak(
    open val opprettetTidspunkt: LocalDateTime,
    open val id: Long,
    open val søkerFødselsnummer: String,
    open val status: FagsakStatus,
    open val underBehandling: Boolean,
    open val gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
)

fun Fagsak.tilRestBaseFagsak(
    underBehandling: Boolean,
    gjeldendeUtbetalingsperioder: List<Utbetalingsperiode> = emptyList()
): RestBaseFagsak = RestBaseFagsak(
    opprettetTidspunkt = this.opprettetTidspunkt,
    id = this.id,
    søkerFødselsnummer = this.hentAktivIdent().ident,
    status = this.status,
    underBehandling = underBehandling,
    gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder
)

data class RestFagsak(
    override val opprettetTidspunkt: LocalDateTime,
    override val id: Long,
    override val søkerFødselsnummer: String,
    override val status: FagsakStatus,
    override val underBehandling: Boolean,
    override val gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
    val behandlinger: List<RestUtvidetBehandling>,
    val tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>
) : RestBaseFagsak(
    opprettetTidspunkt = opprettetTidspunkt,
    id = id,
    søkerFødselsnummer = søkerFødselsnummer,
    status = status,
    underBehandling = underBehandling,
    gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder
)

fun RestBaseFagsak.tilRestFagsak(
    restUtvidetBehandlinger: List<RestUtvidetBehandling>,
    tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>
) = RestFagsak(
    opprettetTidspunkt = this.opprettetTidspunkt,
    id = this.id,
    søkerFødselsnummer = this.søkerFødselsnummer,
    status = this.status,
    underBehandling = this.underBehandling,
    gjeldendeUtbetalingsperioder = this.gjeldendeUtbetalingsperioder,
    behandlinger = restUtvidetBehandlinger,
    tilbakekrevingsbehandlinger = tilbakekrevingsbehandlinger,
)

data class RestMinimalFagsak(
    override val opprettetTidspunkt: LocalDateTime,
    override val id: Long,
    override val søkerFødselsnummer: String,
    override val status: FagsakStatus,
    override val underBehandling: Boolean,
    override val gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
    val behandlinger: List<RestVisningBehandling>,
    val tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>,
) : RestBaseFagsak(
    opprettetTidspunkt = opprettetTidspunkt,
    id = id,
    søkerFødselsnummer = søkerFødselsnummer,
    status = status,
    underBehandling = underBehandling,
    gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder
)

fun RestBaseFagsak.tilRestMinimalFagsak(
    restVisningBehandlinger: List<RestVisningBehandling>,
    tilbakekrevingsbehandlinger: List<RestTilbakekrevingsbehandling>
) = RestMinimalFagsak(
    opprettetTidspunkt = this.opprettetTidspunkt,
    id = this.id,
    søkerFødselsnummer = this.søkerFødselsnummer,
    status = this.status,
    underBehandling = this.underBehandling,
    gjeldendeUtbetalingsperioder = this.gjeldendeUtbetalingsperioder,
    behandlinger = restVisningBehandlinger,
    tilbakekrevingsbehandlinger = tilbakekrevingsbehandlinger,
)
