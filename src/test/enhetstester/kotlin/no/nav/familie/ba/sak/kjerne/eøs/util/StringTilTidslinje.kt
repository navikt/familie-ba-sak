package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.OPPFYLT
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.DELT_BOSTED
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

fun <T : Tidsenhet> String.tilUtdypendeVilkårRegelverkResultatTidslinje(vilkår: Vilkår, start: Tidspunkt<T>) =
    this.tilCharTidslinje(start).map {
        when (it?.lowercaseChar()) {
            '+' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, null)
            'n' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, Regelverk.NASJONALE_REGLER)
            'x' -> UtdypendeVilkårRegelverkResultat(vilkår, Resultat.IKKE_OPPFYLT, null)
            'e' -> UtdypendeVilkårRegelverkResultat(
                vilkår, OPPFYLT, EØS_FORORDNINGEN, BARN_BOR_I_NORGE, BARN_BOR_I_NORGE_MED_SØKER
            )
            'é' -> UtdypendeVilkårRegelverkResultat(
                vilkår, OPPFYLT, EØS_FORORDNINGEN, DELT_BOSTED, BARN_BOR_I_NORGE, BARN_BOR_I_NORGE_MED_SØKER
            )
            'd' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, null, DELT_BOSTED)
            else -> null
        }
    }
