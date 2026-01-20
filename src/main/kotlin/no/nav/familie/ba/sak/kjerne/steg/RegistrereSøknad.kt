package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.ekstern.restDomene.RegistrerSøknadDto
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilDomene
import no.nav.familie.ba.sak.ekstern.restDomene.writeValueAsString
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import org.springframework.stereotype.Service

@Service
class RegistrereSøknad(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingstemaService: BehandlingstemaService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val persongrunnlagService: PersongrunnlagService,
    private val loggService: LoggService,
    private val vedtakService: VedtakService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
) : BehandlingSteg<RegistrerSøknadDto> {
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: RegistrerSøknadDto,
    ): StegType {
        val aktivSøknadGrunnlagFinnes = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id) != null
        val søknadDTO: SøknadDTO = data.søknad
        val innsendtSøknad = søknadDTO.writeValueAsString()

        if (behandling.underkategori != søknadDTO.underkategori.tilDomene()) {
            behandlingstemaService.oppdaterBehandlingstemaFraRegistrereSøknadSteg(
                behandling = behandling,
                nyUnderkategori = søknadDTO.underkategori.tilDomene(),
            )
        }

        loggService.opprettRegistrertSøknadLogg(behandling, aktivSøknadGrunnlagFinnes)
        søknadGrunnlagService.lagreOgDeaktiverGammel(
            søknadGrunnlag =
                SøknadGrunnlag(
                    behandlingId = behandling.id,
                    søknad = innsendtSøknad,
                ),
        )

        val forrigeBehandlingSomErVedtatt =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling = behandling)
        persongrunnlagService.registrerBarnFraSøknad(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt,
            søknadDTO = søknadDTO,
        )

        // TODO: Fjern EndretUtbetalingAndel for personer som ikke lenger er relevant for behandlingen
        tilbakestillBehandlingService.initierOgSettBehandlingTilVilkårsvurdering(
            behandling = behandling,
            bekreftEndringerViaFrontend = data.bekreftEndringerViaFrontend,
        )

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)

        vedtakService.oppdater(vedtak)

        return hentNesteStegForNormalFlyt(behandling = behandling)
    }

    override fun stegType(): StegType = StegType.REGISTRERE_SØKNAD
}
