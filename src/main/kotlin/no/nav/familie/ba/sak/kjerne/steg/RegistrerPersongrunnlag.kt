package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RegistrerPersongrunnlag(
    private val behandlingService: BehandlingService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vilkårService: VilkårService,
    private val personopplysningGrunnlagForNyBehandlingService: PersonopplysningGrunnlagForNyBehandlingService
) : BehandlingSteg<RegistrerPersongrunnlagDTO> {

    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: RegistrerPersongrunnlagDTO
    ): StegType {
        val forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(
            behandling
        )

        personopplysningGrunnlagForNyBehandlingService.opprettPersonopplysningGrunnlag(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt,
            søkerIdent = data.ident,
            barnasIdenter = data.barnasIdenter
        )

        when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.ENDRE_MIGRERINGSDATO -> {
                vilkårService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(
                        behandling
                    ),
                    nyMigreringsdato = data.nyMigreringsdato!!
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(data.nyMigreringsdato, behandling)
            }
            BehandlingÅrsak.HELMANUELL_MIGRERING -> {
                vilkårService.genererVilkårsvurderingForHelmanuellMigrering(
                    behandling = behandling,
                    nyMigreringsdato = data.nyMigreringsdato!!
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(data.nyMigreringsdato, behandling)
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

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_PERSONGRUNNLAG
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}

data class RegistrerPersongrunnlagDTO(
    val ident: String,
    val barnasIdenter: List<String>,
    val nyMigreringsdato: LocalDate? = null
)
