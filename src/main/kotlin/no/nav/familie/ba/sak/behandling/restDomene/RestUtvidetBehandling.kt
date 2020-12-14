package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.arbeidsfordeling.domene.RestArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.steg.StegType
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
                                 val utbetalingsperioder: List<Utbetalingsperiode>,
                                 val personerMedAndeler: List<RestPersonMedAndeler>,
                                 val endretAv: String,
                                 val opplysningsplikt: RestOpplysningsplikt?)