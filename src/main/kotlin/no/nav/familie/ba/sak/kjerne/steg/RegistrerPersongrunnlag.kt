package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RegistrerPersongrunnlag(
    private val behandlingService: BehandlingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val personidentService: PersonidentService,
    private val vilkårService: VilkårService
) : BehandlingSteg<RegistrerPersongrunnlagDTO> {

    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: RegistrerPersongrunnlagDTO
    ): StegType {
        val forrigeBehandlingSomErVedtatt = behandlingService.hentForrigeBehandlingSomErVedtatt(
            behandling
        )
        val aktør = personidentService.hentOgLagreAktør(data.ident, true)
        val barnaAktør = personidentService.hentOgLagreAktørIder(data.barnasIdenter, true)

        if (behandling.type == BehandlingType.REVURDERING && forrigeBehandlingSomErVedtatt != null) {
            val forrigePersongrunnlagBarna =
                behandlingService
                    .finnBarnFraBehandlingMedTilkjentYtsele(behandlingId = forrigeBehandlingSomErVedtatt.id)
            val forrigeMålform =
                persongrunnlagService.hentSøkersMålform(behandlingId = forrigeBehandlingSomErVedtatt.id)

            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = aktør,
                barnFraInneværendeBehandling = barnaAktør,
                barnFraForrigeBehandling = forrigePersongrunnlagBarna,
                behandling = behandling,
                målform = forrigeMålform
            )
            // Lagre ned migreringsdato
            kopierOgLagreNedMigeringsdatoFraSisteVedtattBehandling(forrigeBehandlingSomErVedtatt, behandling)
        } else {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = aktør,
                barnFraInneværendeBehandling = barnaAktør,
                behandling = behandling,
                målform = Målform.NB
            )
        }
        when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.ENDRE_MIGRERINGSDATO -> {
                vilkårService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = behandlingService.hentForrigeBehandlingSomErVedtatt(behandling),
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
                    forrigeBehandlingSomErVedtatt = behandlingService.hentForrigeBehandlingSomErVedtatt(
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

    private fun kopierOgLagreNedMigeringsdatoFraSisteVedtattBehandling(
        forrigeBehandlingSomErVedtatt: Behandling,
        behandling: Behandling
    ) {
        if (forrigeBehandlingSomErVedtatt.erMigrering()) {
            val migreringsdato = behandlingService.hentMigreringsdatoIBehandling(forrigeBehandlingSomErVedtatt.id)
            if (migreringsdato != null) {
                behandlingService.lagreNedMigreringsdato(migreringsdato, behandling)
            }
        }
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
