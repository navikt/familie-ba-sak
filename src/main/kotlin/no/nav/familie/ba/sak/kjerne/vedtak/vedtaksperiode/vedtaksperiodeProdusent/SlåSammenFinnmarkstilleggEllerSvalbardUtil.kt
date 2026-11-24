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
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

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
    val bosattIRiketVilkårForrigePeriode =
        vilkårResultaterForrigePeriode.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BOSATT_I_RIKET }
    val bosattIRiketVilkårDennePerioden = vilkårResultaterDennePerioden.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BOSATT_I_RIKET }

    val aktørTilHarUtdypendeVilkårSomGirTilleggForrigePeriode = hentAktørTilHarUtdypendeVilkårSomGirTilleggIPeriode(bosattIRiketVilkårForrigePeriode)
    val aktørTilHarUtdypendeVilkårSomGirTilleggDennePerioden = hentAktørTilHarUtdypendeVilkårSomGirTilleggIPeriode(bosattIRiketVilkårDennePerioden)

    val erAndreEndringerIVilkårResultat =
        vilkårResultaterDennePerioden.erUlikUtenomFinnmarksOgSvalbardTillegg(vilkårResultaterForrigePeriode)

    val erTilleggInnvilgetForBarnOgSøker = aktørTilHarUtdypendeVilkårSomGirTilleggDennePerioden.all { it.value }

    if (erAndreEndringerIVilkårResultat || erTilleggInnvilgetForBarnOgSøker) {
        return false
    }

    return aktørTilHarUtdypendeVilkårSomGirTilleggForrigePeriode.any { (aktør, erUtdypendeVilkårInnvilgetForrigePeriode) ->
        val erEndringIUtdypendeVilkårSomGirTilleggForAktør =
            aktørTilHarUtdypendeVilkårSomGirTilleggDennePerioden[aktør] != erUtdypendeVilkårInnvilgetForrigePeriode

        erEndringIUtdypendeVilkårSomGirTilleggForAktør
    }
}

private fun hentAktørTilHarUtdypendeVilkårSomGirTilleggIPeriode(vilkårResultater: List<VilkårResultat>): Map<Aktør, Boolean> {
    val aktørTilVilkårsResultatListe =
        vilkårResultater.groupBy { it.personResultat?.aktør ?: throw Feil("VilkårResultat ${it.id} har ikke personResultat") }
    val aktørTilHarUtdypendeVilkårSomGirTilleggIPeriode =
        aktørTilVilkårsResultatListe.mapValues { (_, vilkårResultater) ->
            vilkårResultater
                .flatMap { it.utdypendeVilkårsvurderinger }
                .any { it in UTDYPENDE_VILKÅR_SOM_GIR_TILLEGG }
        }
    return aktørTilHarUtdypendeVilkårSomGirTilleggIPeriode
}

internal fun List<VilkårResultat>.erUlikUtenomFinnmarksOgSvalbardTillegg(
    vilkårResultaterForrigePeriode: List<VilkårResultat>,
): Boolean =
    this.map { VilkårResultatForVedtaksperiode(it) }.utenTilleggsmarkeringOgDatoer() !=
        vilkårResultaterForrigePeriode.map { VilkårResultatForVedtaksperiode(it) }.utenTilleggsmarkeringOgDatoer()

private fun List<VilkårResultatForVedtaksperiode>.utenTilleggsmarkeringOgDatoer(): Set<VilkårResultatForVedtaksperiode> =
    this
        .map { vilkårResultat ->
            vilkårResultat.copy(
                utdypendeVilkårsvurderinger =
                    vilkårResultat.utdypendeVilkårsvurderinger
                        .filterNot { it in UTDYPENDE_VILKÅR_SOM_GIR_TILLEGG },
                fom = null,
                tom = null,
            )
        }.toSet()

fun Tidslinje<VedtaksperiodeGrunnlagForPerson>.slåSammenPerioderUtenUtbetalingMedBareEndringITilleggsmarkering(): Tidslinje<VedtaksperiodeGrunnlagForPerson> =
    this
        .tilPerioderIkkeNull()
        .fold(emptyList()) { acc: List<Periode<VedtaksperiodeGrunnlagForPerson>>, dennePerioden ->
            val forrigePeriode = acc.lastOrNull()

            if (forrigePeriode != null &&
                forrigePeriode.tom?.plusDays(1) == dennePerioden.fom &&
                !dennePerioden.verdi.erInnvilget() &&
                dennePerioden.verdi.harBareEndringITilleggsmarkering(forrigePeriode.verdi)
            ) {
                acc.dropLast(1) + forrigePeriode.copy(tom = dennePerioden.tom)
            } else {
                acc + dennePerioden
            }
        }.tilTidslinje()

private fun VedtaksperiodeGrunnlagForPerson.harBareEndringITilleggsmarkering(grunnlagForrigePeriode: VedtaksperiodeGrunnlagForPerson): Boolean {
    val erLikUtenomVilkårResultater =
        this.kopier(vilkårResultaterForVedtaksperiode = emptyList()) == grunnlagForrigePeriode.kopier(vilkårResultaterForVedtaksperiode = emptyList())
    val erVilkårResultaterLikeUtenomTilleggsmarkering =
        this.vilkårResultaterForVedtaksperiode.utenTilleggsmarkeringOgDatoer() == grunnlagForrigePeriode.vilkårResultaterForVedtaksperiode.utenTilleggsmarkeringOgDatoer()
    val erEndringITilleggsmarkering =
        this.vilkårResultaterForVedtaksperiode.tilleggsmarkeringer() != grunnlagForrigePeriode.vilkårResultaterForVedtaksperiode.tilleggsmarkeringer()

    return erLikUtenomVilkårResultater && erVilkårResultaterLikeUtenomTilleggsmarkering && erEndringITilleggsmarkering
}

private fun List<VilkårResultatForVedtaksperiode>.tilleggsmarkeringer(): Set<Pair<AktørId, UtdypendeVilkårsvurdering>> =
    this
        .flatMap { vilkårResultat ->
            vilkårResultat.utdypendeVilkårsvurderinger
                .filter { it in UTDYPENDE_VILKÅR_SOM_GIR_TILLEGG }
                .map { vilkårResultat.aktørId to it }
        }.toSet()
