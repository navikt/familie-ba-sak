package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType

data class RestBehandling(val aktiv: Boolean,
                          val behandlingId: Long?,
                          val type: BehandlingType,
                          val status: BehandlingStatus,
                          val barnasFødselsnummer: List<String?>?,
                          val vedtakForBehandling: List<RestVedtak?>)
