package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.periodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMåned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingTidslinjeService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val persongrunnlagService: PersongrunnlagService,
) {

    fun hentTidslinjerThrows(behandlingId: BehandlingId): VilkårsvurderingTidslinjer {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId.id)!!
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId.id)!!

        return VilkårsvurderingTidslinjer(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag,
        )
    }

    fun hentTidslinjer(behandlingId: BehandlingId): VilkårsvurderingTidslinjer? {
        return try {
            hentTidslinjerThrows(behandlingId)
        } catch (exception: NullPointerException) {
            return null
        }
    }

    fun hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId: BehandlingId): Tidslinje<Boolean, Måned> {
        val søker = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId.id).søker
        val søkerPersonresultater = vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = behandlingId.id)
            .personResultater.single { it.aktør == søker.aktør }

        val erAnnenForelderOmfattetAvNorskLovgivingTidslinje = søkerPersonresultater.vilkårResultater
            .filter { it.vilkårType === Vilkår.BOSATT_I_RIKET }
            .map {
                periodeAv(
                    it.periodeFom,
                    it.periodeTom,
                    it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                )
            }
            .tilTidslinje()

        return erAnnenForelderOmfattetAvNorskLovgivingTidslinje.tilMåned { it.contains(element = true) }
    }
}
