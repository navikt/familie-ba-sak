package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.EvalueringÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.IKKE_OPPFYLT
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.IKKE_VURDERT
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.OPPFYLT
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed

sealed class Delvilkår {
    open val begrunnelse: String = ""
    open val begrunnelseForManuellKontroll: BegrunnelseForManuellKontrollAvVilkår? = null
    open val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList()
    open val evalueringÅrsaker: Set<EvalueringÅrsak> = emptySet()

    fun tilResultat(): Resultat =
        when (this) {
            is OppfyltDelvilkår -> OPPFYLT
            is IkkeOppfyltDelvilkår -> IKKE_OPPFYLT
            is IkkeVurdertVilkår -> IKKE_VURDERT
        }
}

data class IkkeVurdertVilkår(
    override val evalueringÅrsaker: Set<EvalueringÅrsak> = emptySet(),
) : Delvilkår()

data class OppfyltDelvilkår(
    override val begrunnelse: String,
    override val begrunnelseForManuellKontroll: BegrunnelseForManuellKontrollAvVilkår? = null,
    override val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
) : Delvilkår()

data class IkkeOppfyltDelvilkår(
    override val evalueringÅrsaker: Set<EvalueringÅrsak> = emptySet(),
    override val begrunnelse: String = "",
) : Delvilkår()

fun Tidslinje<Delvilkår>.vurderFinnmarkOgSvalbardtillegg(
    adresserForPerson: Adresser,
): Tidslinje<Delvilkår> {
    val erBosattIFinnmarkEllerNordTromsTidslinje = adresserForPerson.lagErBosattIFinnmarkEllerNordTromsTidslinje()
    val erOppholdsadressePåSvalbardTidslinje = adresserForPerson.lagErOppholdsadresserPåSvalbardTidslinje()
    return this.kombinerMed(erBosattIFinnmarkEllerNordTromsTidslinje, erOppholdsadressePåSvalbardTidslinje) { delvilkår, erBosattIFinnmarkEllerNordTroms, erOppholdsadressePåSvalbard ->
        when (delvilkår) {
            is OppfyltDelvilkår -> {
                val utdypendeVilkårsvurderinger =
                    when {
                        erOppholdsadressePåSvalbard == true -> listOf(BOSATT_PÅ_SVALBARD)
                        erBosattIFinnmarkEllerNordTroms == true -> listOf(BOSATT_I_FINNMARK_NORD_TROMS)
                        else -> emptyList()
                    }
                delvilkår.copy(utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger)
            }

            else -> {
                delvilkår
            }
        }
    }
}

fun List<Periode<Delvilkår>>.tilVilkårResultater(personResultat: PersonResultat): Set<VilkårResultat> =
    this
        .map { erBosattINorgePeriode ->
            VilkårResultat(
                personResultat = personResultat,
                erAutomatiskVurdert = true,
                resultat = erBosattINorgePeriode.verdi.tilResultat(),
                vilkårType = BOSATT_I_RIKET,
                periodeFom = erBosattINorgePeriode.fom,
                periodeTom = erBosattINorgePeriode.tom,
                begrunnelse = PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + erBosattINorgePeriode.verdi.begrunnelse,
                sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                begrunnelseForManuellKontroll = erBosattINorgePeriode.verdi.begrunnelseForManuellKontroll,
                utdypendeVilkårsvurderinger = erBosattINorgePeriode.verdi.utdypendeVilkårsvurderinger,
                erOpprinneligPreutfylt = true,
                evalueringÅrsaker = erBosattINorgePeriode.verdi.evalueringÅrsaker.map { it.hentNavn() },
            )
        }.toSet()
