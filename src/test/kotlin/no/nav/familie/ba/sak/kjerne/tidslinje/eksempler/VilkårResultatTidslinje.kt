package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.PeriodeInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

data class VilkårRegelverkResultat(
    val regelverk: Regelverk?,
    val resultat: Resultat,
    val vilkår: Vilkår,
)

class VilkårResultatTidslinje(
    val aktør: Aktør,
    val vilkår: Vilkår,
    private val vilkårResultater: Collection<VilkårResultat>
) : KalkulerendeTidslinje<VilkårRegelverkResultat>(emptyList()) {
    val minsteDato = vilkårResultater.map { it.periodeFom ?: it.periodeTom }
        .filterNotNull()
        .minOfOrNull { it } ?: throw IllegalStateException("Finner ikke gyldig dato i vilkårsresultatene")

    val størsteDato = vilkårResultater.map { it.periodeTom ?: it.periodeFom }
        .filterNotNull()
        .maxOfOrNull { it } ?: throw IllegalStateException("Finner ikke gyldig dato i vilkårsresultatene")

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<VilkårRegelverkResultat> {
        return vilkårResultater.filter {
            it.periodeFom.tilTidspunktEllerUendeligLengeSiden(minsteDato) <= tidspunkt &&
                it.periodeTom.tilTidspunktEllerUendeligLengeTil(størsteDato) >= tidspunkt
        }.firstOrNull()
            ?.let { PeriodeInnhold(VilkårRegelverkResultat(it.vurderesEtter, it.resultat, vilkår), emptyList()) }
            ?: PeriodeInnhold(null, emptyList())
    }
}
