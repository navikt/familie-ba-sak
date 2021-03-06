package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.writeValueAsString
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service

@Service
class RegistrereSøknad(
        private val søknadGrunnlagService: SøknadGrunnlagService,
        private val persongrunnlagService: PersongrunnlagService,
        private val loggService: LoggService,
        private val vilkårService: VilkårService,
        private val behandlingService: BehandlingService,
        private val vedtakService: VedtakService,
        private val beregningService: BeregningService,
        private val vedtaksperiodeService: VedtaksperiodeService
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
                                                     søknadDTO = søknadDTO)

        vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling,
                                                           bekreftEndringerViaFrontend = data.bekreftEndringerViaFrontend,
                                                           forrigeBehandling = forrigeBehandlingSomErIverksatt)


        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)

        beregningService.slettTilkjentYtelseForBehandling(behandlingId = behandling.id)
        vedtaksperiodeService.slettVedtaksperioderFor(vedtak = vedtak)

        vedtak.settBegrunnelser(emptySet())
        if (data.søknad.barnaMedOpplysninger.any { !it.erFolkeregistrert }
            && vedtak.vedtakBegrunnelser.none { it.begrunnelse == VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN }) {
            vedtak.leggTilBegrunnelse(VedtakBegrunnelse(
                    fom = null,
                    tom = null,
                    vedtak = vedtak,
                    begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN
            ))
        }

        vedtakService.oppdater(vedtak)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_SØKNAD
    }
}