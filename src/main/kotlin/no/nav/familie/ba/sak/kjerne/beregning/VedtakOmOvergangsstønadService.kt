package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.springframework.stereotype.Service

@Service
class VedtakOmOvergangsstønadService(
    val fagsakService: FagsakService,
    val behandlingService: BehandlingService,
    val vedtakService: VedtakService,
    val stegService: StegService,
    val vedtaksperiodeService: VedtaksperiodeService,
    val beregningService: BeregningService,
    val småbarnstilleggService: SmåbarnstilleggService
) {

    fun håndterVedtakOmOvergangsstønad(personIdent: String) {
        val fagsak = fagsakService.hent(personIdent = PersonIdent(personIdent)) ?: return
        val harÅpenBehandling = behandlingService.hentAktivOgÅpenForFagsak(fagsakId = fagsak.id) != null

        if (harÅpenBehandling) {
            // TODO lag oppgave
            return
        }

        val påvirkerFagsak = småbarnstilleggService.vedtakOmOvergangsstønadPåvirkerFagsak(fagsak)

        if (påvirkerFagsak) {
            // TODO behandle endring
            val nyBehandling = stegService.håndterNyBehandling(
                NyBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER, // TODO småbarnstillegg
                    søkersIdent = fagsak.hentAktivIdent().ident,
                    skalBehandlesAutomatisk = true
                )
            )

            val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(nyBehandling)
            val behandlingEtterBehandlingsresultat =
                stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)

            leggTilBegrunnelserPåVedtak(fagsak, behandlingEtterBehandlingsresultat)

            vedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandlingEtterBehandlingsresultat)
        }
    }

    private fun leggTilBegrunnelserPåVedtak(
        fagsak: Fagsak,
        behandlingEtterBehandlingsresultat: Behandling
    ) {
        val sistIverksatteBehandling = behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsak.id)
        val forrigeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) emptyList()
            else beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                behandlingId = sistIverksatteBehandling.id
            ).filter { it.erSmåbarnstillegg() }

        val nyeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) emptyList()
            else beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                behandlingId = behandlingEtterBehandlingsresultat.id
            ).filter { it.erSmåbarnstillegg() }

        val (innvilgelsesperioder, reduksjonsperioder) = hentEndredePerioderISmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeSmåbarnstilleggAndeler,
            nyeSmåbarnstilleggAndeler = nyeSmåbarnstilleggAndeler
        )
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterBehandlingsresultat.id)

        /**vedtaksperiodeService.lagre(innvilgelsesperioder.map {
         VedtaksperiodeMedBegrunnelser(
         vedtak = vedtak,
         fom = it.fom.førsteDagIInneværendeMåned(),
         tom = it.tom.sisteDagIInneværendeMåned(),
         type = VedtakBegrunnelseType.INNVILGET,
         begrunnelser
         )
         })*/
    }
}
