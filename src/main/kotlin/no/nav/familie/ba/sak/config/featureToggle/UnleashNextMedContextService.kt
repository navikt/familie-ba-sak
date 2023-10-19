package no.nav.familie.ba.sak.config.featureToggle

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.unleash.UnleashContextFields
import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class UnleashNextMedContextService(
    private val unleashService: UnleashService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
) {
    fun isEnabled(toggleId: String, behandlingId: Long): Boolean {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        return unleashService.isEnabled(
            toggleId,
            properties = mapOf(
                UnleashContextFields.FAGSAK_ID to behandling.fagsak.id.toString(),
                UnleashContextFields.ENHET_ID to arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetId,
                UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
                UnleashContextFields.EPOST to SikkerhetContext.hentSaksbehandlerEpost(),
            ),
        )
    }
}
