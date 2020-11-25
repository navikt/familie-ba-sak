package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.behandling.restDomene.writeValueAsString
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service

@Service
class RegistrereSøknad(
        private val søknadGrunnlagService: SøknadGrunnlagService,
        private val persongrunnlagService: PersongrunnlagService,
        private val loggService: LoggService,
        private val vilkårService: VilkårService,
        private val behandlingService: BehandlingService
) : BehandlingSteg<RestRegistrerSøknad> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: RestRegistrerSøknad): StegType {
        val aktivSøknadGrunnlagFinnes = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id) != null
        val søknadDTO = data.søknad
        val innsendtSøknad = søknadDTO.writeValueAsString()

        loggService.opprettRegistrertSøknadLogg(behandling, aktivSøknadGrunnlagFinnes)
        søknadGrunnlagService.lagreOgDeaktiverGammel(søknadGrunnlag = SøknadGrunnlag(behandlingId = behandling.id,
                                                                                     søknad = innsendtSøknad))


        val forrigeBehandlingSomErIverksatt = behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)
        persongrunnlagService.registrerBarnFraSøknad(behandling = behandling,
                                                     forrigeBehandling = forrigeBehandlingSomErIverksatt,
                                                     søknadDTO = data.søknad)

        vilkårService.initierVilkårvurderingForBehandling(behandling = behandling,
                                                          bekreftEndringerViaFrontend = data.bekreftEndringerViaFrontend,
                                                          forrigeBehandling = forrigeBehandlingSomErIverksatt)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_SØKNAD
    }
}