package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.tilEøsRegelverkTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

fun <T : Tidsenhet> String.tilRegelverkResultatTidslinje(start: Tidspunkt<T>) =
    this.tilCharTidslinje(start).map {
        when (it?.lowercaseChar()) {
            'e' -> RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN
            'n' -> RegelverkResultat.OPPFYLT_NASJONALE_REGLER
            '!' -> RegelverkResultat.OPPFYLT_BLANDET_REGELVERK
            '+' -> RegelverkResultat.OPPFYLT_REGELVERK_IKKE_SATT
            '?' -> RegelverkResultat.IKKE_FULLT_VURDERT
            'x' -> RegelverkResultat.IKKE_OPPFYLT
            else -> null
        }
    }

fun <T : Tidsenhet> String.tilVilkårRegelverkResultatTidslinje(vilkår: Vilkår, start: Tidspunkt<T>) =
    this.tilRegelverkResultatTidslinje(start)
        .map { it?.let { VilkårRegelverkResultat(vilkår, it) } }

fun String.tilEøsRegelverkTidslinje(start: MånedTidspunkt) =
    this.tilRegelverkResultatTidslinje(start)
        .tilEøsRegelverkTidslinje()
