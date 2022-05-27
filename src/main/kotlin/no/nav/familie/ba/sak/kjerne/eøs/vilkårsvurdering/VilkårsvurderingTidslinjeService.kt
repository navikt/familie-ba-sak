package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingTidslinjeService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) {

    fun hentTidslinjerThrows(behandlingId: Long): VilkårsvurderingTidslinjer {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)!!
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)!!

        return VilkårsvurderingTidslinjer(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag
        )
    }

    fun hentTidslinjer(behandlingId: Long): VilkårsvurderingTidslinjer? {
        return try {
            hentTidslinjerThrows(behandlingId)
        } catch (exception: NullPointerException) {
            return null
        }
    }
}
