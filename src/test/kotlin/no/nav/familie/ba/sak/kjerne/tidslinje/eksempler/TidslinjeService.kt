package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository

class TidslinjeService(
    val vilkårsvurderingRepository: VilkårsvurderingRepository,
    val persongrunnlagService: PersongrunnlagService,
    val kompetanseService: KompetanseService
) {
    fun hentTidslinjer(behandlingId: Long): Tidslinjer {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)!!
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId)
        val kompetanser = kompetanseService.hentKompetanser(behandlingId)

        return Tidslinjer(
            vilkårsvurdering,
            personopplysningGrunnlag,
            kompetanser
        )
    }
}
