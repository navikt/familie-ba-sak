package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegistrerPersongrunnlag(
    private val behandlingService: BehandlingService,
    private val persongrunnlagService: PersongrunnlagService,
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

        if (behandling.type == BehandlingType.REVURDERING && forrigeBehandlingSomErVedtatt != null) {
            val forrigePersongrunnlagBarna =
                behandlingService.finnBarnFraBehandlingMedTilkjentYtsele(behandlingId = forrigeBehandlingSomErVedtatt.id)
            val forrigeMålform =
                persongrunnlagService.hentSøkersMålform(behandlingId = forrigeBehandlingSomErVedtatt.id)

            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                data.ident,
                data.barnasIdenter.union(
                    forrigePersongrunnlagBarna
                )
                    .toList(),
                behandling,
                forrigeMålform
            )
        } else {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                data.ident,
                data.barnasIdenter,
                behandling,
                Målform.NB
            )
        }

        if (!(
                behandling.opprettetÅrsak == BehandlingÅrsak.SØKNAD ||
                    behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE
                )
        ) {
            vilkårService.initierVilkårsvurderingForBehandling(
                behandling = behandling,
                bekreftEndringerViaFrontend = true,
                forrigeBehandlingSomErVedtatt = behandlingService.hentForrigeBehandlingSomErVedtatt(
                    behandling
                )
            )
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_PERSONGRUNNLAG
    }
}

data class RegistrerPersongrunnlagDTO(
    val ident: String,
    val barnasIdenter: List<String>
)
