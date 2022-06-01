package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RegistrerPersongrunnlag(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,
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

        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt,
            nyMigreringsdato = data.nyMigreringsdato
        )

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_PERSONGRUNNLAG
    }
}

data class RegistrerPersongrunnlagDTO(
    val ident: String,
    val barnasIdenter: List<String>,
    val nyMigreringsdato: LocalDate? = null
)
