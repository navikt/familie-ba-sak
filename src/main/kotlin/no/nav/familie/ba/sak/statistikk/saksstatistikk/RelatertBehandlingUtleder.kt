package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.klage.KlageService
import org.springframework.stereotype.Component

@Component
class RelatertBehandlingUtleder(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val klageService: KlageService,
    private val unleashService: UnleashNextMedContextService,
) {
    fun utledRelatertBehandling(behandling: Behandling): RelatertBehandling? {
        if (!unleashService.isEnabled(FeatureToggle.BEHANDLE_KLAGE, false)) {
            return null
        }
        return if (behandling.erRevurderingKlage()) {
            val sisteVedtatteKlagebehandling = klageService.hentSisteVedtatteKlagebehandling(behandling.fagsak.id)
            if (sisteVedtatteKlagebehandling == null) {
                throw Feil("Forventer en vedtatt klagebehandling for behandling ${behandling.id}")
            }
            RelatertBehandling.fraKlagebehandling(sisteVedtatteKlagebehandling)
        } else {
            behandlingHentOgPersisterService
                .hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
                ?.takeIf { harBarnetrygdbehandlingKorrektBehandlingType(it) }
                ?.let { RelatertBehandling.fraBarnetrygdbehandling(it) }
        }
    }

    private fun harBarnetrygdbehandlingKorrektBehandlingType(barnetrygdbehandling: Behandling) =
        when (barnetrygdbehandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING,
            BehandlingType.MIGRERING_FRA_INFOTRYGD,
            BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT,
            -> false

            BehandlingType.REVURDERING,
            BehandlingType.TEKNISK_ENDRING,
            -> true
        }
}
