package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.TidslinjeUtenAvhengigheter
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.erEnDelAvTidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.erInnenforTidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

class VilkårResultatTidslinje(
    private val vilkårsresultater: List<VilkårResultat>,
    private val periodeRepository: PeriodeRepository
) : TidslinjeUtenAvhengigheter<VilkårResultat>(
    VilkårsresultatTidslinjeRepository(vilkårsresultater, periodeRepository)
) {
    override val fraOgMed: Tidspunkt = vilkårsresultater
        .map { it.periodeFom.tilTidspunktEllerUendeligLengeSiden { it.periodeTom!! } }.minOrNull()!!
    override val tilOgMed: Tidspunkt = vilkårsresultater
        .map { it.periodeTom.tilTidspunktEllerUendeligLengeTil { it.periodeFom!! } }.maxOrNull()!!

    override fun genererPerioder(tidsrom: Tidsrom) = vilkårsresultater
        .map { it.tilPeriode() }
        // Streng tolkning; perioden må være ekte innenfor tidsrommet. Kan gi hull i tidslinjen
        .filter { it.erInnenforTidsrom(tidsrom) }
        // Vid tolkning; periodeen må touch'e tidsrommet. Her vil vi kunne få overlappende perioder
        .filter { it.erEnDelAvTidsrom(tidsrom) }
}

class VilkårsresultatTidslinjeRepository(
    vilkårResultater: Collection<VilkårResultat>,
    periodeRepository: PeriodeRepository
) : AbstraktTidslinjeRepository<VilkårResultat>(vilkårResultater, periodeRepository) {

    val behandlingId = vilkårResultater.first().behandlingId
    val aktørId = vilkårResultater.first().personResultat!!.aktør.aktørId
    val vilkår = vilkårResultater.first().vilkårType
    override val tidslinjeId = "VilkårResult.$behandlingId.$aktørId.$vilkår"

    override fun innholdTilReferanse(innhold: VilkårResultat?) =
        "${innhold!!.id}.${innhold.vurderesEtter}.${innhold.resultat}.${innhold.periodeFom}.${innhold.resultat}"
}

fun VilkårResultat.tilPeriode(): Periode<VilkårResultat> {
    val fom = periodeFom.tilTidspunktEllerUendeligLengeSiden { periodeTom!! }
    val tom = periodeTom.tilTidspunktEllerUendeligLengeTil { periodeFom!! }
    return Periode(fom, tom, this)
}
