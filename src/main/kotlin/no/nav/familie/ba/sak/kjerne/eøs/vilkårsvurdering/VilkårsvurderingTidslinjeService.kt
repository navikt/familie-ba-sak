package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.common.feilHvis
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.lagForskjøvetTidslinjeForOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import org.springframework.stereotype.Service

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
        return hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId = behandlingId, søkerAktør = søker.aktør)
    }

    fun hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(
        behandlingId: BehandlingId,
        søkerAktør: Aktør,
    ): Tidslinje<Boolean> {
        val personResultaterForSøker =
            vilkårsvurderingService
                .hentAktivForBehandlingThrows(behandlingId = behandlingId.id)
                .personResultater
                .filter { it.aktør == søkerAktør }
        feilHvis(personResultaterForSøker.size != 1) {
            "Forventet ett personresultat for søker på behandling=${behandlingId.id}, fant ${personResultaterForSøker.size}"
        }
        val søkerPersonresultater = personResultaterForSøker.single()

        val erAnnenForelderOmfattetAvNorskLovgivingTidslinje =
            søkerPersonresultater.vilkårResultater
                .lagForskjøvetTidslinjeForOppfylteVilkår(Vilkår.BOSATT_I_RIKET)
                .mapVerdi { it?.utdypendeVilkårsvurderinger?.contains(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING) }
        return erAnnenForelderOmfattetAvNorskLovgivingTidslinje
    }
}
