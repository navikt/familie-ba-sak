package no.nav.familie.ba.sak.kjerne.eøs

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullableMånedPeriode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service

@Service
class EøsService(
    val vilkårsvurderingRepository: VilkårsvurderingRepository,
    val persongrunnlagService: PersongrunnlagService
) {

    fun utledEøsPerioder(behandlingId: Long): Map<Person, List<NullableMånedPeriode>> {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
            ?: throw Feil("Finner ikke vilkårsvurdering på behandling")

        val barna = persongrunnlagService.hentBarna(behandlingId)

        return barna.associateBy(
            keySelector = { it },
            valueTransform = { barn ->
                vilkårsvurdering.personResultater
                    .filter { it.aktør == barn.aktør }
                    .filter { !it.harEksplisittAvslag() }
                    .map { EøsUtil.utledEøsPerioder(it.vilkårResultater) }
                    .flatten()
            }
        )
    }
}
