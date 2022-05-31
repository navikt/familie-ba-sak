package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VilkårsvurderingForNyBehandlingService(
    private val vilkårService: VilkårService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService
) {

    fun opprettVilkårsvurderingUtenomHovedflyt(
        behandling: Behandling,
        nyMigreringsdato: LocalDate? = null
    ) {
        when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.ENDRE_MIGRERINGSDATO -> {
                vilkårService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(
                        behandling
                    ),
                    nyMigreringsdato = nyMigreringsdato!!
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(nyMigreringsdato, behandling)
            }
            BehandlingÅrsak.HELMANUELL_MIGRERING -> {
                vilkårService.genererVilkårsvurderingForHelmanuellMigrering(
                    behandling = behandling,
                    nyMigreringsdato = nyMigreringsdato!!
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(nyMigreringsdato, behandling)
            }
            !in listOf(BehandlingÅrsak.SØKNAD, BehandlingÅrsak.FØDSELSHENDELSE) -> {
                vilkårService.initierVilkårsvurderingForBehandling(
                    behandling = behandling,
                    bekreftEndringerViaFrontend = true,
                    forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(
                        behandling
                    )
                )
            }
            else -> logger.info(
                "Perioder i vilkårsvurdering generer ikke automatisk for " +
                    behandling.opprettetÅrsak.visningsnavn
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
