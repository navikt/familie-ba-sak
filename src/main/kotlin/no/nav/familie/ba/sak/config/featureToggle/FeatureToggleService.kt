package no.nav.familie.ba.sak.config.featureToggle

import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.hentArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.unleash.UnleashContextFields
import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class FeatureToggleService(
    private val unleashService: UnleashService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
) {
    fun isEnabled(
        toggle: FeatureToggle,
        behandlingId: Long,
    ): Boolean {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        return unleashService.isEnabled(
            toggle.navn,
            properties =
                mapOf(
                    UnleashContextFields.FAGSAK_ID to behandling.fagsak.id.toString(),
                    UnleashContextFields.BEHANDLING_ID to behandling.id.toString(),
                    UnleashContextFields.ENHET_ID to arbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetId,
                    UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
                    UnleashContextFields.EPOST to SikkerhetContext.hentSaksbehandlerEpost(),
                ),
        )
    }

    fun isEnabled(toggle: FeatureToggle): Boolean =
        unleashService.isEnabled(
            toggle.navn,
            properties =
                mapOf(
                    UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
                    UnleashContextFields.EPOST to SikkerhetContext.hentSaksbehandlerEpost(),
                ),
        )

    fun isEnabled(toggleId: String) =
        unleashService.isEnabled(
            toggleId,
            properties =
                mapOf(
                    UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
                    UnleashContextFields.EPOST to SikkerhetContext.hentSaksbehandlerEpost(),
                ),
        )

    fun isEnabled(
        toggle: FeatureToggle,
        defaultValue: Boolean,
    ): Boolean = unleashService.isEnabled(toggle.navn, defaultValue)
}
