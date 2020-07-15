package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.restDomene.writeValueAsString
import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service

@Service
class RegistrereSøknad(
        private val søknadGrunnlagService: SøknadGrunnlagService,
        private val loggService: LoggService
) : BehandlingSteg<SøknadDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: SøknadDTO): StegType {
        val aktivSøknadGrunnlagFinnes = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id) != null

        val innsendtSøknad = data.writeValueAsString()

        loggService.opprettRegistrertSøknadLogg(behandling, aktivSøknadGrunnlagFinnes)
        søknadGrunnlagService.lagreOgDeaktiverGammel(søknadGrunnlag = SøknadGrunnlag(behandlingId = behandling.id,
                                                                                         søknad = innsendtSøknad))

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_SØKNAD
    }
}

