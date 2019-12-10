package no.nav.familie.ba.sak.behandling.restDomene

data class RestBehandling(
        val aktiv: Boolean,
        val behandlingId: Long?,
        var barnasFødselsnummer: List<String?>?)