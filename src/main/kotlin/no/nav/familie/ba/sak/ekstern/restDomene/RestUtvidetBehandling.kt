package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiode
import java.time.LocalDateTime

data class RestUtvidetBehandling(
        val behandlingId: Long,
        val aktiv: Boolean,
        val steg: StegType,
        val stegTilstand: List<RestBehandlingStegTilstand>,
        val status: BehandlingStatus,
        val resultat: BehandlingResultat,
        val skalBehandlesAutomatisk: Boolean,
        val type: BehandlingType,
        val kategori: BehandlingKategori,
        val underkategori: BehandlingUnderkategori,
        val årsak: BehandlingÅrsak,
        val opprettetTidspunkt: LocalDateTime,
        val endretAv: String,
        val arbeidsfordelingPåBehandling: RestArbeidsfordelingPåBehandling,
        val søknadsgrunnlag: SøknadDTO?,
        val personer: List<RestPerson>,
        val personResultater: List<RestPersonResultat>,
        val fødselshendelsefiltreringResultater: List<RestFødselshendelsefiltreringResultat>,
        val utbetalingsperioder: List<Utbetalingsperiode>,
        val personerMedAndelerTilkjentYtelse: List<RestPersonMedAndeler>,
        val endretUtbetalingAndeler: List<RestEndretUtbetalingAndel>,
        val tilbakekreving: RestTilbakekreving?,
        val vedtakForBehandling: List<RestVedtak>,
        val totrinnskontroll: RestTotrinnskontroll?,
        @Deprecated("Sannsynligvis unødvendig når vedtaksperioder blir flyttet til vedtaket og utbetalingsperioder blir en egen liste")
        val vedtaksperioder: List<Vedtaksperiode>,
)