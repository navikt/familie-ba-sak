package no.nav.familie.ba.sak.cucumber.mock

import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository

fun mockVilkårsvurderingTidslinjeService(
    vilkårsvurderingRepository: VilkårsvurderingRepository,
    vilkårsvurderingService: VilkårsvurderingService,
    persongrunnlagService: PersongrunnlagService,
): VilkårsvurderingTidslinjeService {
    val vilkårsvurderingTidslinjeService =
        VilkårsvurderingTidslinjeService(
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            vilkårsvurderingService = vilkårsvurderingService,
            persongrunnlagService = persongrunnlagService,
        )
    return vilkårsvurderingTidslinjeService
}
