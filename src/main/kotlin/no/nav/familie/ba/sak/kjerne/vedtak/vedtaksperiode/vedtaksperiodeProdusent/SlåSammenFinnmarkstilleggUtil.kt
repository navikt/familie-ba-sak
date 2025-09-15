package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.byttUtNullListeMedTomListe
import no.nav.familie.tidslinje.utvidelser.tilPerioder

private val UTDYPENDE_VILKÅR_SOM_GIR_TILLEGG = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS, UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)

/**
 * Slår sammen perioder som ble splittet pga endring i Finnmarkstillegg eller Svalbardstillegg men som ikke fører til reell endring i
 * hvorvidt det ble utbetalt tillegg. F.eks. søker flyttet til Finnmark, men ikke barn.
 **/
fun Tidslinje<List<VilkårResultat>>.slåSammenSplitterPåUtdypendeVilkår(): Tidslinje<List<VilkårResultat>> {
    val perioder =
        this.tilPerioder().map { it.byttUtNullListeMedTomListe() }

    val sortertePerioder = perioder.sortedWith(compareBy({ it.fom }, { it.tom }))

    return sortertePerioder
        .fold(emptyList()) { acc: List<Periode<List<VilkårResultat>>>, dennePerioden ->
            val forrigePeriode = acc.lastOrNull()

            if (forrigePeriode != null && skalSlåsSammen(dennePerioden.verdi, forrigePeriode.verdi)) {
                slåSammenMedForrigePeriode(acc, forrigePeriode, dennePerioden)
            } else {
                acc + dennePerioden
            }
        }.tilTidslinje()
}

private fun slåSammenMedForrigePeriode(
    acc: List<Periode<List<VilkårResultat>>>,
    forrigePeriode: Periode<List<VilkårResultat>>,
    dennePerioden: Periode<List<VilkårResultat>>,
): List<Periode<List<VilkårResultat>>> = acc.dropLast(1) + forrigePeriode.copy(tom = dennePerioden.tom)

private fun skalSlåsSammen(
    vilkårResultaterDennePerioden: List<VilkårResultat>,
    vilkårResultaterForrigePeriode: List<VilkårResultat>,
): Boolean {
    val aktørTilHarUtdypendeVilkårForrigePeriode = hentAktørTilHarUtdypendeVilkårSomGirTilleggIPeriode(vilkårResultaterForrigePeriode)
    val aktørTilHarUtdypendeVilkårDennePerioden = hentAktørTilHarUtdypendeVilkårSomGirTilleggIPeriode(vilkårResultaterDennePerioden)

    val bosattIRiketVilkårForrigePeriode =
        vilkårResultatListeTilErBosattIRiketVilkårListe(vilkårResultaterForrigePeriode)
    val bosattIRiketVilkårDennePerioden = vilkårResultatListeTilErBosattIRiketVilkårListe(vilkårResultaterDennePerioden)

    val erAndreEndringerIVilkårResultat =
        endringIAndreTingIVilkårResultater(bosattIRiketVilkårForrigePeriode, bosattIRiketVilkårDennePerioden)

    val erTilleggInnvilgetForBarnOgSøker = aktørTilHarUtdypendeVilkårDennePerioden.all { it.value } == true

    val erAntallVilkårOgAktørerUlike =
        aktørTilHarUtdypendeVilkårDennePerioden.size != aktørTilHarUtdypendeVilkårForrigePeriode.size

    if (erAndreEndringerIVilkårResultat || erTilleggInnvilgetForBarnOgSøker || erAntallVilkårOgAktørerUlike) {
        return false
    }

    return aktørTilHarUtdypendeVilkårForrigePeriode.any { (aktørForrigePeriode, erUtdypendeVilkårInnvilgetForrigePeriode) ->
        val erEndringIUtdypendeVilkårSomGirTilleggForAktør =
            aktørTilHarUtdypendeVilkårDennePerioden[aktørForrigePeriode] != erUtdypendeVilkårInnvilgetForrigePeriode

        erEndringIUtdypendeVilkårSomGirTilleggForAktør
    }
}

private fun vilkårResultatListeTilErBosattIRiketVilkårListe(vilkårResultater: List<VilkårResultat>?): List<VilkårResultat>? = vilkårResultater?.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BOSATT_I_RIKET }

private fun hentAktørTilHarUtdypendeVilkårSomGirTilleggIPeriode(vilkårResultater: List<VilkårResultat>): Map<Aktør, Boolean> {
    val bosattIRiketVilkår =
        vilkårResultater.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BOSATT_I_RIKET }
    val aktørTilVilkårsResultatListe =
        bosattIRiketVilkår.groupBy { it.personResultat?.aktør ?: throw Feil("VilkårResultat uten personResultat") }
    val aktørTilHarUtdypendeVilkårSomGirTilleggIPeriode =
        aktørTilVilkårsResultatListe.mapValues { (_, value) ->
            value
                .flatMap { it.utdypendeVilkårsvurderinger }
                .any { it in UTDYPENDE_VILKÅR_SOM_GIR_TILLEGG }
        }
    return aktørTilHarUtdypendeVilkårSomGirTilleggIPeriode
}

private fun endringIAndreTingIVilkårResultater(
    bosattIRiketVilkårForrigePeriode: List<VilkårResultat>?,
    bosattIRiketVilkårDennePerioden: List<VilkårResultat>?,
): Boolean {
    val vilkårResultatForrigePeriode =
        bosattIRiketVilkårForrigePeriode?.map { vilkårResultat ->
            vilkårResultat.copy(
                utdypendeVilkårsvurderinger =
                    vilkårResultat.utdypendeVilkårsvurderinger
                        .filterNot { it -> it in UTDYPENDE_VILKÅR_SOM_GIR_TILLEGG },
                periodeFom = null,
                periodeTom = null,
            )
        }

    val vilkårResultatDennePerioden =
        bosattIRiketVilkårDennePerioden?.map { vilkårResultat ->
            vilkårResultat.copy(
                utdypendeVilkårsvurderinger =
                    vilkårResultat.utdypendeVilkårsvurderinger
                        .filterNot { it -> it in UTDYPENDE_VILKÅR_SOM_GIR_TILLEGG },
                periodeFom = null,
                periodeTom = null,
            )
        }

    return vilkårResultatDennePerioden != vilkårResultatForrigePeriode
}
