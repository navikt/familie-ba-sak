package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.IKKE_OPPFYLT
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.OPPFYLT
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.util.UtdypendeVilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.mapVerdiNullable
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.tilMåned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.NASJONALE_REGLER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.DELT_BOSTED
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.LocalDate
import java.time.YearMonth

fun String.tilRegelverkResultatTidslinje(start: YearMonth) =
    this.tilCharTidslinje(start).mapVerdiNullable {
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

fun String.tilUtdypendeVilkårRegelverkResultatTidslinje(
    vilkår: Vilkår,
    start: LocalDate,
) = this.tilCharTidslinje(start).mapVerdiNullable {
    when (it?.lowercaseChar()) {
        '+' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, null)
        'n' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, NASJONALE_REGLER)
        'x' -> UtdypendeVilkårRegelverkResultat(vilkår, IKKE_OPPFYLT, null)
        'e' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, EØS_FORORDNINGEN)
        'é' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, EØS_FORORDNINGEN, DELT_BOSTED)
        'd' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, null, DELT_BOSTED)
        else -> null
    }
}

fun String.tilAnnenForelderOmfattetAvNorskLovgivningTidslinje(start: YearMonth) =
    this
        .tilCharTidslinje(start)
        .mapVerdiNullable {
            when (it?.lowercaseChar()) {
                '+' -> true
                '-' -> false
                else -> null
            }
        }.tilMåned { it.single() }
