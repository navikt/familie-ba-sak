package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.restDomene.RestPersonVilkårResultat
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PeriodeService {

    /*
    fun manuellVurderingTilBehandlingResultat(behandlingRestultat: List<RestPersonVilkårResultat>): MutableSet<PeriodeResultat> {
        return mutableSetOf(PeriodeResultat(
                        periodeResultat = listeAvVilkårResultat,
                        periodeFom = LocalDate.now(),
                        periodeTom = LocalDate.now()))
    }
    */

    fun restPersonVilkårTilPerioder(restPersonVilkårResultat: RestPersonVilkårResultat): MutableSet<PeriodeResultat> {
        return mutableSetOf(PeriodeResultat(
                periodeResultat = mutableSetOf(),
                periodeFom = LocalDate.now(),
                periodeTom = LocalDate.now()))
    }

}

data class VilkårVurderingGrunnlag (
    val periode_fom: LocalDate? = null,
    val periode_tom: LocalDate? = null,
    val vilkår: Vilkår? = null
)

