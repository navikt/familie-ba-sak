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
        val aktiv: Boolean,
        val arbeidsfordelingPåBehandling: RestArbeidsfordelingPåBehandling,
        val årsak: BehandlingÅrsak,
        val skalBehandlesAutomatisk: Boolean,
        val behandlingId: Long,
        val type: BehandlingType,
        val status: BehandlingStatus,
        val steg: StegType,
        val stegTilstand: List<RestBehandlingStegTilstand>,
        val søknadsgrunnlag: SøknadDTO?,
        val kategori: BehandlingKategori,
        val personer: List<RestPerson>,
        val opprettetTidspunkt: LocalDateTime,
        val underkategori: BehandlingUnderkategori,
        val personResultater: List<RestPersonResultat>,
        val resultat: BehandlingResultat,
        val vedtakForBehandling: List<RestVedtak>,
        val totrinnskontroll: RestTotrinnskontroll?,
        @Deprecated("Sannsynligvis unødvendig når vedtaksperioder blir flyttet til vedtaket og utbetalingsperioder blir en egen liste")
        val vedtaksperioder: List<Vedtaksperiode>,
        val utbetalingsperioder: List<Utbetalingsperiode>,
        val personerMedAndelerTilkjentYtelse: List<RestPersonMedAndeler>,
        val tilbakekreving: RestTilbakekreving?,
        val endretAv: String,
)