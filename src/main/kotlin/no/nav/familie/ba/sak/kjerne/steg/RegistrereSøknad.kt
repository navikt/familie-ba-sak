package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.writeValueAsString
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import org.springframework.stereotype.Service

@Service
class RegistrereSøknad(
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val persongrunnlagService: PersongrunnlagService,
    private val loggService: LoggService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
) : BehandlingSteg<RestRegistrerSøknad> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: RestRegistrerSøknad
    ): StegType {
        val aktivSøknadGrunnlagFinnes = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id) != null
        val søknadDTO = data.søknad
        val innsendtSøknad = søknadDTO.writeValueAsString()

        behandlingService.oppdaterBehandlingUnderkategori(
            behandling = behandlingService.hent(behandlingId = behandling.id),
            nyBehandlingUnderkategori = søknadDTO.underkategori
        )

        loggService.opprettRegistrertSøknadLogg(behandling, aktivSøknadGrunnlagFinnes)
        søknadGrunnlagService.lagreOgDeaktiverGammel(
            søknadGrunnlag = SøknadGrunnlag(
                behandlingId = behandling.id,
                søknad = innsendtSøknad
            )
        )

        val forrigeBehandlingSomErIverksatt = behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)
        persongrunnlagService.registrerBarnFraSøknad(
            behandling = behandlingService.hent(behandlingId = behandling.id),
            forrigeBehandling = forrigeBehandlingSomErIverksatt,
            søknadDTO = søknadDTO
        )

        tilbakestillBehandlingService.initierOgSettBehandlingTilVilårsvurdering(
            behandling = behandlingService.hent(behandlingId = behandling.id),
            bekreftEndringerViaFrontend = data.bekreftEndringerViaFrontend
        )

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)

        vedtakService.oppdater(vedtak)

        return hentNesteStegForNormalFlyt(behandling = behandlingService.hent(behandlingId = behandling.id))
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_SØKNAD
    }
}
