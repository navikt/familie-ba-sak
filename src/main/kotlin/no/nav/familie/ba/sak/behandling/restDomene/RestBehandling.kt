package no.nav.familie.ba.sak.behandling.restDomene

data class RestBehandling(val aktiv: Boolean,
                          val behandlingId: Long?,
                          val barnasFødselsnummer: List<String?>?,
                          val vedtakForBehandling: List<RestVedtak?>)
