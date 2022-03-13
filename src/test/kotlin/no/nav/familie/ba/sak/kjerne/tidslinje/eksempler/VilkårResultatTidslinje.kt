package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.TidslinjeUtenAvhengigheter
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.erEnDelAvTidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.erInnenforTidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

data class VilkårRegelverkResultat(
    val vilkårResultatId: Long,
    val regelverk: Regelverk?,
    val resultat: Resultat,
    val vilkår: Vilkår,
)

fun VilkårResultat.tilVilkårRegelverkResultat() =
    VilkårRegelverkResultat(id, vurderesEtter, resultat, vilkårType)

class VilkårResultatTidslinje(
    private val vilkårsresultater: List<VilkårResultat>,
    private val periodeRepository: PeriodeRepository
) : TidslinjeUtenAvhengigheter<VilkårRegelverkResultat>(
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
    private val vilkårResultater: Collection<VilkårResultat>,
    private val periodeRepository: PeriodeRepository
) : TidslinjeRepository<VilkårRegelverkResultat> {

    val behandlingId = vilkårResultater.first().behandlingId
    val aktørId = vilkårResultater.first().personResultat!!.aktør.aktørId
    val vilkår = vilkårResultater.first().vilkårType
    val tidslinjeId = "VilkårResult.$behandlingId.$aktørId.$vilkår"

    override fun lagre(perioder: Collection<Periode<VilkårRegelverkResultat>>): Collection<Periode<VilkårRegelverkResultat>> {
        val refTilInnhold = perioder.map { it.innhold!! }.refTilInnhold()

        return periodeRepository
            .lagrePerioder(tidslinjeId, perioder.map { it.tilDto(it.tilInnholdsreferanse()) })
            .map { dto -> dto.tilPeriode(refTilInnhold) }
    }

    override fun hent(): Collection<Periode<VilkårRegelverkResultat>> {
        val vilkårRegelverkResultater = vilkårResultater
            .map { it.tilVilkårRegelverkResultat() }

        return periodeRepository.hentPerioder(
            tidslinjeId = tidslinjeId,
            innholdReferanser = vilkårResultater.map { it.tilPeriode().tilInnholdsreferanse() }
        ).map { it.tilPeriode(vilkårRegelverkResultater.refTilInnhold()) }
            .filter { it.innhold != null }
    }
}

fun Collection<VilkårRegelverkResultat>.refTilInnhold(): (String) -> VilkårRegelverkResultat? =
    { ref -> this.find { it.tilInnholdsreferanse() == ref } }

fun VilkårRegelverkResultat.tilInnholdsreferanse(): String = "$vilkårResultatId.$regelverk.$resultat"
fun Periode<VilkårRegelverkResultat>.tilInnholdsreferanse(): String =
    "${innhold!!.vilkårResultatId}.${innhold!!.regelverk}.${innhold!!.resultat}.${fom.tilLocalDate()}.${tom.tilLocalDate()}"

fun VilkårResultat.tilPeriode(): Periode<VilkårRegelverkResultat> {
    val fom = periodeFom.tilTidspunktEllerUendeligLengeSiden { periodeTom!! }
    val tom = periodeTom.tilTidspunktEllerUendeligLengeTil { periodeFom!! }
    return Periode(fom, tom, tilVilkårRegelverkResultat())
}
