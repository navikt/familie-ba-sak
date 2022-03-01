package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils.hentSisteBehandlingSomErIverksatt
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.finnEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EndringstidspunktService(
    private val behandlingRepository: BehandlingRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val persongrunnlagService: PersongrunnlagService,
) {

    fun finnEndringstidpunkForBehandling(behandlingId: Long): LocalDate {
        val nyBehandling = behandlingRepository.finnBehandling(behandlingId)

        val iverksatteBehandlinger = behandlingRepository.finnIverksatteBehandlinger(fagsakId = nyBehandling.fagsak.id)
        val gammelBehandling = hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger)
            ?: return TIDENES_MORGEN

        val nyVilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = behandlingId)
        val gammelVilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = gammelBehandling.id)

        val nyeAndelerTilkjentYtelse = andelTilkjentYtelseRepository
            .finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)

        val gamleAndelerTilkjentYtelse = andelTilkjentYtelseRepository
            .finnAndelerTilkjentYtelseForBehandling(behandlingId = gammelBehandling.id)

        val nyttPersonopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId)
        val gammeltPersonopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = gammelBehandling.id)

        return finnEndringstidspunkt(
            nyVilkårsvurdering = nyVilkårsvurdering,
            gammelVilkårsvurdering = gammelVilkårsvurdering,
            nyeAndelerTilkjentYtelse = nyeAndelerTilkjentYtelse,
            gamleAndelerTilkjentYtelse = gamleAndelerTilkjentYtelse,
            nyttPersonopplysningGrunnlag = nyttPersonopplysningGrunnlag,
            gammeltPersonopplysningGrunnlag = gammeltPersonopplysningGrunnlag,
        )
    }
}
