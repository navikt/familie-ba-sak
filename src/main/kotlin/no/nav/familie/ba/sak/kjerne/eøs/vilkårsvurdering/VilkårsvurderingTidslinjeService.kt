package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import org.springframework.stereotype.Service
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.lagForskjøvetFamilieFellesTidslinjeForOppfylteVilkår as lagForskjøvetTidslinjeForOppfylteVilkår

@Service
class VilkårsvurderingTidslinjeService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
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

    fun hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId: BehandlingId): Tidslinje<Boolean> {
        val søker = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId.id).søker
        val søkerPersonresultater =
            vilkårsvurderingService
                .hentAktivForBehandlingThrows(behandlingId = behandlingId.id)
                .personResultater
                .single { it.aktør == søker.aktør }

        val erAnnenForelderOmfattetAvNorskLovgivingTidslinje =
            søkerPersonresultater.vilkårResultater
                .lagForskjøvetTidslinjeForOppfylteVilkår(Vilkår.BOSATT_I_RIKET)
                .mapVerdi { it?.utdypendeVilkårsvurderinger?.contains(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING) }
        return erAnnenForelderOmfattetAvNorskLovgivingTidslinje
    }
}
