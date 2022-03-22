package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service

@Service
class TidslinjeService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository
) {

    fun hentTidslinjer(behandlingId: Long): Tidslinjer {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)!!

        return Tidslinjer(
            vilkårsvurdering,
        )
    }
}