package no.nav.familie.ba.sak.kjerne.eøs

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EøsService(
    val vilkårsvurderingRepository: VilkårsvurderingRepository,
    val persongrunnlagService: PersongrunnlagService
) {
    val eøsVilkår = setOf(Vilkår.BOSATT_I_RIKET, Vilkår.BOR_MED_SØKER, Vilkår.LOVLIG_OPPHOLD)

    fun utledEøsPerioder(behandlingId: Long): Map<Person, Periode> {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
            ?: throw Feil("Finner ikke vilkårsvurdering på behandling")

        val barna = persongrunnlagService.hentBarna(behandlingId)
        return barna.map { barn ->
            Pair(
                barn,
                vilkårsvurdering.personResultater
                    .filter { it.aktør == barn.aktør }
                    .flatMap { it.vilkårResultater }
                    .filter { it.resultat == Resultat.OPPFYLT }
                    .filter { Regelverk.EØS_FORORDNINGEN == it.vurderesEtter }
            )
        }.toMap()
            .filterValues { resultater -> resultater.map { it.vilkårType }.containsAll(eøsVilkår) }
            .mapValues { (_, resultater) ->
                resultater.map { Periode(it.periodeFom ?: LocalDate.MIN, it.periodeTom ?: LocalDate.MAX) }
            }.mapValues { (_, resultater) ->
                resultater.reduce { acc, periode ->
                    Periode(maxOf(acc.fom, periode.fom), minOf(acc.tom, periode.tom))
                }
            }
    }
}
