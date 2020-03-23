package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadDTO
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.writeValueAsString
import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service

@Service
class RegistrereSøknad(
        private val søknadGrunnlagService: SøknadGrunnlagService,
        private val loggService: LoggService
) : BehandlingSteg<SøknadDTO> {

    override fun utførSteg(behandling: Behandling, data: SøknadDTO): Behandling {
        val aktivSøknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)
        loggService.opprettRegistrertSøknadLogg(behandling, aktivSøknadGrunnlag != null)

        søknadGrunnlagService.lagreOgDeaktiverGammel(søknadGrunnlag = SøknadGrunnlag(behandlingId = behandling.id,
                                                                                     søknad = data.writeValueAsString()))

        return behandling
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_SØKNAD
    }
}

