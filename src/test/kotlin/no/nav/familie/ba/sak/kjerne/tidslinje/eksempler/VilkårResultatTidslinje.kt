package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.TidslinjeUtenAvhengigheter
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
    override fun genererPerioder() = vilkårsresultater
        .map {
            val fom = it.periodeFom.tilTidspunktEllerUendeligLengeSiden { it.periodeTom!! }
            val tom = it.periodeTom.tilTidspunktEllerUendeligLengeTil { it.periodeFom!! }
            Periode(fom, tom, it.tilVilkårRegelverkResultat())
        }
}

class VilkårsresultatTidslinjeRepository(
    private val vilkårResultater: Collection<VilkårResultat>,
    private val periodeRepository: PeriodeRepository
) : TidslinjeRepository<VilkårRegelverkResultat> {

    override fun lagre(perioder: Collection<Periode<VilkårRegelverkResultat>>): Collection<Periode<VilkårRegelverkResultat>> {
        val refTilInnhold = perioder.map { it.innhold!! }.refTilInnhold()

        return periodeRepository
            .lagrePerioder(perioder.map { it.tilDto { it.id.tilInnholdsreferanse() } })
            .map { dto -> dto.tilPeriode(refTilInnhold) }
    }

    override fun hent(): Collection<Periode<VilkårRegelverkResultat>>? {
        val vilkårRegelverkResultater = vilkårResultater
            .map { it.tilVilkårRegelverkResultat() }

        return periodeRepository.hentPerioder(
            innholdReferanser = vilkårRegelverkResultater.map { it.vilkårResultatId.tilInnholdsreferanse() }
        ).map { it.tilPeriode(vilkårRegelverkResultater.refTilInnhold()) }
    }
}

fun Collection<VilkårRegelverkResultat>.refTilInnhold(): (String) -> VilkårRegelverkResultat =
    { ref -> this.find { it.vilkårResultatId.tilInnholdsreferanse() == ref } ?: throw Error() }

fun Long.tilInnholdsreferanse(): String = "Vilkårsresultat.$this"
