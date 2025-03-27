package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.klage.KlageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RelatertBehandlingUtleder(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val klageService: KlageService,
    private val unleashService: UnleashNextMedContextService,
) {
    private val logger: Logger = LoggerFactory.getLogger(RelatertBehandlingUtleder::class.java)

    fun utledRelatertBehandling(behandling: Behandling): RelatertBehandling? {
        if (behandling.erRevurderingKlage() && unleashService.isEnabled(FeatureToggle.BEHANDLE_KLAGE, false)) {
            val forrigeVedtatteKlagebehandling = klageService.hentForrigeVedtatteKlagebehandling(behandling)
            if (forrigeVedtatteKlagebehandling == null) {
                throw Feil("Forventer en vedtatt klagebehandling for fagsak ${behandling.fagsak.id} og behandling ${behandling.id}")
            }
            return RelatertBehandling.fraKlagebehandling(forrigeVedtatteKlagebehandling)
        }

        if (behandling.erRevurderingEllerTekniskEndring()) {
            val forrigeVedtatteBarnetrygdbehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
            if (forrigeVedtatteBarnetrygdbehandling == null) {
                logger.warn("Forventer en vedtatt barnetrygdbehandling for fagsak ${behandling.fagsak.id} og behandling ${behandling.id}")
                return null
            }
            return RelatertBehandling.fraBarnetrygdbehandling(forrigeVedtatteBarnetrygdbehandling)
        }

        return null
    }
}
