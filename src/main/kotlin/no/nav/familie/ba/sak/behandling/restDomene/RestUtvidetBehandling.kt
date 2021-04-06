package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiode
import java.time.LocalDateTime

data class RestUtvidetBehandling(val aktiv: Boolean,
                                 val arbeidsfordelingPåBehandling: RestArbeidsfordelingPåBehandling,
                                 val årsak: BehandlingÅrsak,
                                 val skalBehandlesAutomatisk: Boolean,
                                 val behandlingId: Long,
                                 val type: BehandlingType,
                                 val status: BehandlingStatus,
                                 val steg: StegType,
                                 val stegTilstand: List<RestBehandlingStegTilstand>,
                                 val kategori: BehandlingKategori,
                                 val personer: List<RestPerson>,
                                 val opprettetTidspunkt: LocalDateTime,
                                 val underkategori: BehandlingUnderkategori,
                                 val personResultater: List<RestPersonResultat>,
                                 val resultat: BehandlingResultat,
                                 val vedtakForBehandling: List<RestVedtak>,
                                 val totrinnskontroll: RestTotrinnskontroll?,
                                 val vedtaksperioder: List<Vedtaksperiode>,
                                 val personerMedAndelerTilkjentYtelse: List<RestPersonMedAndeler>,
                                 val endretAv: String)