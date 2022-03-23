package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service

@Service
class TidslinjeService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) {

    fun hentTidslinjer(behandlingId: Long): Tidslinjer {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)!!
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)!!

        return Tidslinjer(
            vilkårsvurdering = vilkårsvurdering,
            søkersFødselsdato = personopplysningGrunnlag.søker.fødselsdato,
            yngsteBarnSin18årsdag = personopplysningGrunnlag.yngsteBarnSinFødselsdato,
            barnOgFødselsdatoer = personopplysningGrunnlag.barna.associate { it.aktør to it.fødselsdato }
        )
    }
}
