package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.EksternBehandlingRelasjonService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RelatertBehandlingUtleder(
    private val eksternBehandlingRelasjonService: EksternBehandlingRelasjonService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    private val logger: Logger = LoggerFactory.getLogger(RelatertBehandlingUtleder::class.java)

    fun utledRelatertBehandling(behandling: Behandling): RelatertBehandling? {
        if (behandling.erRevurderingKlage()) {
            val eksternKlagebehandlingRelasjon =
                eksternBehandlingRelasjonService.finnEksternBehandlingRelasjon(
                    behandlingId = behandling.id,
                    fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )
            if (eksternKlagebehandlingRelasjon == null) {
                logger.warn("Forventer en ekstern klagebehandling relasjon for fagsak=${behandling.fagsak.id} og behandling=${behandling.id}")
                return null
            }
            return RelatertBehandling.fraEksternBehandlingRelasjon(eksternKlagebehandlingRelasjon)
        }

        if (behandling.erRevurderingEllerTekniskEndring()) {
            val forrigeVedtatteBarnetrygdbehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
            if (forrigeVedtatteBarnetrygdbehandling == null) {
                logger.warn("Forventer en vedtatt barnetrygdbehandling for fagsak=${behandling.fagsak.id} og behandling=${behandling.id}")
                return null
            }
            return RelatertBehandling.fraBarnetrygdbehandling(forrigeVedtatteBarnetrygdbehandling)
        }

        return null
    }
}
