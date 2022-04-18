package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service

@Service
class TidslinjeService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) {

    fun hentTidslinjerThrows(behandlingId: Long): Tidslinjer {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)!!
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)!!

        return Tidslinjer(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag
        )
    }

    fun hentTidslinjer(behandlingId: Long): Tidslinjer? {
        return try {
            hentTidslinjerThrows(behandlingId)
        } catch (exception: NullPointerException) {
            return null
        }
    }
}
