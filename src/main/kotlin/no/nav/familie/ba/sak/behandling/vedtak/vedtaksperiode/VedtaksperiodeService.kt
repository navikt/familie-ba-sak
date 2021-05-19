package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.Behandlingutils
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vedtak.VedtakBegrunnelseRepository
import no.nav.familie.ba.sak.behandling.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.behandling.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import org.springframework.stereotype.Service

@Service
class VedtaksperiodeService(
        private val behandlingRepository: BehandlingRepository,
        private val persongrunnlagService: PersongrunnlagService,
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        private val vedtakBegrunnelseRepository: VedtakBegrunnelseRepository,
        private val vedtaksperiodeRepository: VedtaksperiodeRepository
) {

    fun lagre(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): VedtaksperiodeMedBegrunnelser {
        return vedtaksperiodeRepository.save(vedtaksperiodeMedBegrunnelser)
    }

    fun slettVedtaksperioderFor(behandling: Behandling) {
        vedtaksperiodeRepository.slettVedtaksperioderFor(behandling)
    }

    /**
     * POC på persisterte vedtaksperioder. Første iterasjon blir kun for en fortsatt innvilget periode.
     */
    fun oppdaterBehandlingMedVedtaksperioder(behandling: Behandling) {

        slettVedtaksperioderFor(behandling)
        if (behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET) {

            lagre(VedtaksperiodeMedBegrunnelser(
                    behandling = behandling,
                    type = Vedtaksperiodetype.FORTSATT_INNVILGET
            ))
        }
    }

    fun hentPersisterteVedtaksperioder(behandling: Behandling): List<VedtaksperiodeMedBegrunnelser> {
        return vedtaksperiodeRepository.finnVedtaksperioderFor(behandling = behandling)
    }

    fun hentVedtaksperioder(behandling: Behandling): List<Vedtaksperiode> {
        val iverksatteBehandlinger =
                behandlingRepository.finnIverksatteBehandlinger(fagsakId = behandling.fagsak.id)

        val forrigeIverksatteBehandling: Behandling? = Behandlingutils.hentForrigeIverksatteBehandling(
                iverksatteBehandlinger = iverksatteBehandlinger,
                behandlingFørFølgende = behandling
        )

        val forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? =
                if (forrigeIverksatteBehandling != null) persongrunnlagService.hentAktiv(behandlingId = forrigeIverksatteBehandling.id) else null
        val forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse> =
                if (forrigeIverksatteBehandling != null) andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                        behandlingId = forrigeIverksatteBehandling.id) else emptyList()

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                                       ?: return emptyList()
        val andelerTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        val utbetalingsperioder = mapTilUtbetalingsperioder(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        val opphørsperioder = mapTilOpphørsperioder(
                forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
                forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse
        )

        val avslagsperioder =
                mapTilAvslagsperioder(vedtakBegrunnelser = vedtakBegrunnelseRepository.finnForBehandling(behandlingId = behandling.id))

        return utbetalingsperioder + opphørsperioder + avslagsperioder
    }
}