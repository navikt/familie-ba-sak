package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingTidslinjeService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val persongrunnlagService: PersongrunnlagService,
) {

    fun hentTidslinjerThrows(behandlingId: BehandlingId): VilkårsvurderingTidslinjer {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = behandlingId.id)!!
        val søkerOgBarn = persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandlingId = behandlingId.id)

        return VilkårsvurderingTidslinjer(
            vilkårsvurdering = vilkårsvurdering,
            søkerOgBarn = søkerOgBarn,
        )
    }

    fun hentTidslinjer(behandlingId: BehandlingId): VilkårsvurderingTidslinjer? {
        return try {
            hentTidslinjerThrows(behandlingId)
        } catch (exception: NullPointerException) {
            return null
        }
    }
}
