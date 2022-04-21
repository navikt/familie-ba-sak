package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.map
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

fun <T : Tidsenhet> String.tilRegelverkTidslinje(start: Tidspunkt<T>) =
    this.tilCharTidslinje(start).map {
        when (it?.lowercaseChar()) {
            'e' -> Regelverk.EØS_FORORDNINGEN
            'n' -> Regelverk.NASJONALE_REGLER
            else -> null
        }
    }

fun <T : Tidsenhet> String.tilVilkårRegelverkResultatTidslinje(vilkår: Vilkår, start: Tidspunkt<T>) =
    this.tilCharTidslinje(start).map {
        when (it?.lowercaseChar()) {
            'e' -> VilkårRegelverkResultat(vilkår, Regelverk.EØS_FORORDNINGEN, Resultat.OPPFYLT)
            'n' -> VilkårRegelverkResultat(vilkår, Regelverk.NASJONALE_REGLER, Resultat.OPPFYLT)
            '+' -> VilkårRegelverkResultat(vilkår, null, Resultat.OPPFYLT)
            '-' -> VilkårRegelverkResultat(vilkår, null, Resultat.IKKE_OPPFYLT)
            else -> null
        }
    }

fun <T : Tidsenhet> String.tilVilkårResultatTidslinje(tidspunkt: Tidspunkt<T>): Tidslinje<Resultat, T> =
    this.tilCharTidslinje(tidspunkt).map { c ->
        when (c) {
            '+' -> Resultat.OPPFYLT
            '-' -> Resultat.IKKE_OPPFYLT
            ' ' -> Resultat.IKKE_VURDERT
            else -> null
        }
    }
