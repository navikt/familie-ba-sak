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

fun hentUtdypendeVilkårSomIkkeSkalSplitteVedtaksperioder() = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS, UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)

fun Tidslinje<List<VilkårResultat>>.slåSammenFinnmarkstillegg(): Tidslinje<List<VilkårResultat>> {
    val perioder =
        this.tilPerioder().map { it.byttUtNullListeMedTomListe() }

    val sortertePerioder = perioder.sortedWith(compareBy({ it.fom }, { it.tom }))

    return sortertePerioder
        .fold(emptyList()) { acc: List<Periode<List<VilkårResultat>>>, dennePerioden ->
            val forrigePeriode = acc.lastOrNull()

            if (forrigePeriode != null && skalSlåSammenFinnmarkstillegg(dennePerioden.verdi, forrigePeriode.verdi)) {
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

private fun skalSlåSammenFinnmarkstillegg(
    vilkårResultaterDennePerioden: List<VilkårResultat>,
    vilkårResultaterForrigePeriode: List<VilkårResultat>,
): Boolean {
    val aktørTilErBosattPåFinnmarkForrigePeriode = hentAktørTilErBosattIFinnmarkIPeriode(vilkårResultaterForrigePeriode)
    val aktørTilErBosattPåFinnmarkDennePerioden = hentAktørTilErBosattIFinnmarkIPeriode(vilkårResultaterDennePerioden)

    val bosattIRiketVilkårForrigePeriode =
        vilkårResultatListeTilErBosattIRiketVilkårListe(vilkårResultaterForrigePeriode)
    val bosattIRiketVilkårDennePerioden = vilkårResultatListeTilErBosattIRiketVilkårListe(vilkårResultaterDennePerioden)

    val endringIAndreUtdypendeVilkår =
        endringIAnnetEnnFinnmark(bosattIRiketVilkårForrigePeriode, bosattIRiketVilkårDennePerioden)
    val erBarnOgSøkerBosattIFinnmark = aktørTilErBosattPåFinnmarkDennePerioden.all { it.value } == true
    val erAntallVilkårOgAktørerUlike =
        aktørTilErBosattPåFinnmarkDennePerioden.size != aktørTilErBosattPåFinnmarkForrigePeriode.size

    if (endringIAndreUtdypendeVilkår || erBarnOgSøkerBosattIFinnmark || erAntallVilkårOgAktørerUlike) {
        return false
    }

    return aktørTilErBosattPåFinnmarkForrigePeriode.any { (aktørForrigePeriode, erBosattIFinnmarkForrigePeriode) ->
        val erEndringIBosattIFinnmarkForAktør =
            aktørTilErBosattPåFinnmarkDennePerioden[aktørForrigePeriode] != erBosattIFinnmarkForrigePeriode

        erEndringIBosattIFinnmarkForAktør
    }
}

private fun vilkårResultatListeTilErBosattIRiketVilkårListe(vilkårResultater: List<VilkårResultat>?): List<VilkårResultat>? = vilkårResultater?.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BOSATT_I_RIKET }

private fun hentAktørTilErBosattIFinnmarkIPeriode(vilkårResultater: List<VilkårResultat>): Map<Aktør, Boolean> {
    val bosattIRiketVilkår =
        vilkårResultater.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BOSATT_I_RIKET }
    val aktørTilVilkårsrResultatListe =
        bosattIRiketVilkår.groupBy { it.personResultat?.aktør ?: throw Feil("VilkårResultat uten personResultat") }
    val aktørTilErBosattPåFinnmarkIPeriode =
        aktørTilVilkårsrResultatListe.mapValues { (_, value) ->
            value
                .flatMap { it.utdypendeVilkårsvurderinger }
                .any { it in hentUtdypendeVilkårSomIkkeSkalSplitteVedtaksperioder() }
        }
    return aktørTilErBosattPåFinnmarkIPeriode
}

private fun endringIAnnetEnnFinnmark(
    bosattIRiketVilkårForrigePeriode: List<VilkårResultat>?,
    bosattIRiketVilkårDennePerioden: List<VilkårResultat>?,
): Boolean {
    val vilkårResultatForrigePeriode =
        bosattIRiketVilkårForrigePeriode?.flatMap { vilkårResultat ->
            vilkårResultat.utdypendeVilkårsvurderinger
                .filterNot { it -> it in hentUtdypendeVilkårSomIkkeSkalSplitteVedtaksperioder() }
        }

    val vilkårResultatDennePerioden =
        bosattIRiketVilkårDennePerioden?.flatMap { vilkårResultat ->
            vilkårResultat.utdypendeVilkårsvurderinger
                .filterNot { it -> it in hentUtdypendeVilkårSomIkkeSkalSplitteVedtaksperioder() }
        }

    return vilkårResultatDennePerioden != vilkårResultatForrigePeriode
}
