package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingTidslinjeService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val persongrunnlagService: PersongrunnlagService,
) {

    fun hentTidslinjerThrows(behandlingId: BehandlingId): VilkårsvurderingTidslinjer {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId.id)!!
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId.id)!!

        return VilkårsvurderingTidslinjer(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag,
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
