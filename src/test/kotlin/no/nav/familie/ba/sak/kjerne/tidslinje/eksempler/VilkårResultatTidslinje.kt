package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.TidslinjeUtenAvhengigheter
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
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
    val aktør: Aktør,
    val vilkår: Vilkår,
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

    val behandlingId = vilkårResultater.first().behandlingId
    val aktør = vilkårResultater.first().personResultat?.aktør!!

    override fun lagre(perioder: Collection<Periode<VilkårRegelverkResultat>>): Collection<Periode<VilkårRegelverkResultat>> {
        return periodeRepository.lagrePerioder(
            "Vilkårsresultat",
            "$behandlingId.${aktør.aktørId}",
            perioder.map { it.tilDto { it -> it.id } }
        ).map { dto -> dto.tilPeriode(perioder.find { dto.innholdReferanse == it.id }?.innhold!!) }
    }

    override fun hent(): Collection<Periode<VilkårRegelverkResultat>>? {
        val vilkårRegelverkResultater = vilkårResultater
            .map { it.tilVilkårRegelverkResultat() }

        val refTilInhhold: (Long) -> VilkårRegelverkResultat = { ref ->
            vilkårRegelverkResultater.find { it.vilkårResultatId == ref } ?: throw Error()
        }

        return periodeRepository.hentPerioder(
            tidslinjeType = "Vilkårsresultat",
            tidslinjeId = "$behandlingId.${aktør.aktørId}",
            innholdReferanser = vilkårRegelverkResultater.map { it.vilkårResultatId }
        ).map { it.tilPeriode(refTilInhhold(it.id)) }
    }
}
