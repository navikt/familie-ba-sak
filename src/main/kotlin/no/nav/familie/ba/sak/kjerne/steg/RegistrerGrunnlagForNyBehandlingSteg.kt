package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.EøsSkjemaerForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.PersonopplysningGrunnlagForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RegistrerGrunnlagForNyBehandlingSteg(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val personopplysningGrunnlagForNyBehandlingService: PersonopplysningGrunnlagForNyBehandlingService,
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,
    private val eøsSkjemaerForNyBehandlingService: EøsSkjemaerForNyBehandlingService
) : BehandlingSteg<RegistrerGrunnlagForNyBehandlingDTO> {

    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        registrerGrunnlagForNyBehandlingDTO: RegistrerGrunnlagForNyBehandlingDTO
    ): StegType {
        val forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService
            .hentForrigeBehandlingSomErVedtatt(behandling)

        personopplysningGrunnlagForNyBehandlingService.opprettPersonopplysningGrunnlag(
            behandling,
            forrigeBehandlingSomErVedtatt,
            registrerGrunnlagForNyBehandlingDTO.ident,
            registrerGrunnlagForNyBehandlingDTO.barnasIdenter
        )

        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
            behandling,
            forrigeBehandlingSomErVedtatt,
            registrerGrunnlagForNyBehandlingDTO.nyMigreringsdato
        )

        eøsSkjemaerForNyBehandlingService.kopierEøsSkjemaer(
            behandling,
            forrigeBehandlingSomErVedtatt
        )

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_GRUNNLAG_FOR_NY_BEHANDLING
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}

data class RegistrerGrunnlagForNyBehandlingDTO(
    val ident: String,
    val barnasIdenter: List<String>,
    val nyMigreringsdato: LocalDate? = null
)
