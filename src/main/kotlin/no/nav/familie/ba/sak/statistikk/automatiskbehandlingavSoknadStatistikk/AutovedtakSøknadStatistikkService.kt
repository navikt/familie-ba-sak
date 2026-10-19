package no.nav.familie.ba.sak.statistikk.automatiskbehandlingavSoknadStatistikk

import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.AutovedtakSøknadMåBehandlesManueltÅrsak
import org.springframework.stereotype.Service

@Service
class AutovedtakSøknadStatistikkService(
    private val repository: AutomatiskBehandlingAvSøknadStatistikkRepository,
) {
    fun lagreÅrsaker(
        behandlingId: Long,
        årsaker: List<AutovedtakSøknadMåBehandlesManueltÅrsak>,
    ) {
        repository.saveAll(
            årsaker.map { årsak ->
                AutomatiskBehandlingAvSøknadStatistikk(
                    behandlingId = behandlingId,
                    journalpostId = null,
                    årsakTilManuellBehandling = årsak.name,
                )
            },
        )
    }
}
