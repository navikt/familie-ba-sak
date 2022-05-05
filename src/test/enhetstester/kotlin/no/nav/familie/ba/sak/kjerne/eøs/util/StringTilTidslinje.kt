package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
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

fun <T : Tidsenhet> String.tilRegelverkResultatTidslinje(start: Tidspunkt<T>) =
    this.tilCharTidslinje(start).map {
        when (it?.lowercaseChar()) {
            'e' -> RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN
            'n' -> RegelverkResultat.OPPFYLT_NASJONALE_REGLER
            '?' -> RegelverkResultat.OPPFYLT_BLANDET_REGELVERK
            '-' -> RegelverkResultat.IKKE_FULLT_VURDERT
            else -> null
        }
    }

fun <T : Tidsenhet> String.tilVilkårRegelverkResultatTidslinje(vilkår: Vilkår, start: Tidspunkt<T>) =
    this.tilCharTidslinje(start).map {
        when (it?.lowercaseChar()) {
            'e' -> VilkårRegelverkResultat(vilkår, RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN)
            'n' -> VilkårRegelverkResultat(vilkår, RegelverkResultat.OPPFYLT_NASJONALE_REGLER)
            '+' -> VilkårRegelverkResultat(vilkår, RegelverkResultat.OPPFYLT_REGELVERK_IKKE_SATT)
            '-' -> VilkårRegelverkResultat(vilkår, RegelverkResultat.IKKE_OPPFYLT)
            else -> null
        }
    }

fun <T : Tidsenhet> String.tilVilkårResultatTidslinje(tidspunkt: Tidspunkt<T>): Tidslinje<Resultat, T> =
    this.tilCharTidslinje(tidspunkt).map { c ->
        when (c) {
            '+' -> Resultat.OPPFYLT
            '-' -> Resultat.IKKE_OPPFYLT
            '?' -> Resultat.IKKE_VURDERT
            else -> null
        }
    }
