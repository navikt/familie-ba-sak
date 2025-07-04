package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import java.time.LocalDate
import java.time.LocalDateTime

open class RestBaseFagsak(
    open val opprettetTidspunkt: LocalDateTime,
    open val id: Long,
    open val fagsakeier: String,
    open val søkerFødselsnummer: String,
    open val status: FagsakStatus,
    open val underBehandling: Boolean,
    open val løpendeKategori: BehandlingKategori?,
    open val løpendeUnderkategori: BehandlingUnderkategori?,
    open val gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
    open val fagsakType: FagsakType = FagsakType.NORMAL,
    open val institusjon: RestInstitusjon? = null,
)

data class RestFagsak(
    override val opprettetTidspunkt: LocalDateTime,
    override val id: Long,
    override val fagsakeier: String,
    override val søkerFødselsnummer: String,
    override val status: FagsakStatus,
    override val underBehandling: Boolean,
    override val løpendeKategori: BehandlingKategori?,
    override val løpendeUnderkategori: BehandlingUnderkategori?,
    override val gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
    val behandlinger: List<RestUtvidetBehandling>,
    override val fagsakType: FagsakType = FagsakType.NORMAL,
) : RestBaseFagsak(
        opprettetTidspunkt = opprettetTidspunkt,
        id = id,
        fagsakeier = fagsakeier,
        søkerFødselsnummer = søkerFødselsnummer,
        status = status,
        underBehandling = underBehandling,
        løpendeKategori = løpendeKategori,
        løpendeUnderkategori = løpendeUnderkategori,
        gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder,
        fagsakType = fagsakType,
    )

fun RestBaseFagsak.tilRestFagsak(
    restUtvidetBehandlinger: List<RestUtvidetBehandling>,
) = RestFagsak(
    opprettetTidspunkt = this.opprettetTidspunkt,
    id = this.id,
    fagsakeier = this.fagsakeier,
    søkerFødselsnummer = this.søkerFødselsnummer,
    status = this.status,
    underBehandling = this.underBehandling,
    løpendeKategori = this.løpendeKategori,
    løpendeUnderkategori = this.løpendeUnderkategori,
    gjeldendeUtbetalingsperioder = this.gjeldendeUtbetalingsperioder,
    behandlinger = restUtvidetBehandlinger,
    fagsakType = this.fagsakType,
)

data class RestMinimalFagsak(
    override val opprettetTidspunkt: LocalDateTime,
    override val id: Long,
    override val fagsakeier: String,
    override val søkerFødselsnummer: String,
    override val status: FagsakStatus,
    override val løpendeKategori: BehandlingKategori?,
    override val løpendeUnderkategori: BehandlingUnderkategori?,
    override val underBehandling: Boolean,
    override val gjeldendeUtbetalingsperioder: List<Utbetalingsperiode>,
    val behandlinger: List<RestVisningBehandling>,
    val migreringsdato: LocalDate? = null,
    override val fagsakType: FagsakType,
    override val institusjon: RestInstitusjon?,
) : RestBaseFagsak(
        opprettetTidspunkt = opprettetTidspunkt,
        id = id,
        fagsakeier = fagsakeier,
        søkerFødselsnummer = søkerFødselsnummer,
        status = status,
        underBehandling = underBehandling,
        løpendeKategori = løpendeKategori,
        løpendeUnderkategori = løpendeUnderkategori,
        gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder,
        fagsakType = fagsakType,
        institusjon = institusjon,
    )

fun RestBaseFagsak.tilRestMinimalFagsak(
    restVisningBehandlinger: List<RestVisningBehandling>,
    migreringsdato: LocalDate?,
) = RestMinimalFagsak(
    opprettetTidspunkt = this.opprettetTidspunkt,
    id = this.id,
    fagsakeier = this.fagsakeier,
    søkerFødselsnummer = this.søkerFødselsnummer,
    status = this.status,
    underBehandling = this.underBehandling,
    løpendeKategori = this.løpendeKategori,
    løpendeUnderkategori = this.løpendeUnderkategori,
    gjeldendeUtbetalingsperioder = this.gjeldendeUtbetalingsperioder,
    behandlinger = restVisningBehandlinger,
    migreringsdato = migreringsdato,
    fagsakType = this.fagsakType,
    institusjon = this.institusjon,
)
