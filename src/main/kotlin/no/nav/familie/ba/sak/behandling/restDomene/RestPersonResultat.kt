package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.nare.Resultat
import java.time.LocalDate
import java.time.LocalDateTime

data class RestPersonResultat(
        val personIdent: String,
        val vilkårResultater: List<RestVilkårResultat>,
        val andreVurderinger: List<RestAnnenVurdering> = emptyList(),
)

data class RestVilkårResultat(
        val id: Long,
        val vilkårType: Vilkår,
        val resultat: Resultat,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        val begrunnelse: String,
        val endretAv: String,
        val endretTidspunkt: LocalDateTime,
        val behandlingId: Long,
        val erVurdert: Boolean = false,
        val erAutomatiskVurdert: Boolean = false,
        val erEksplisittAvslagPåSøknad: Boolean? = null,
) {

    fun erAvslagUtenPeriode() = this.erEksplisittAvslagPåSøknad == true && this.periodeFom == null && this.periodeTom == null
    fun harFremtidigTom() = this.periodeTom == null || this.periodeTom.isAfter(LocalDate.now().sisteDagIMåned())
}


fun PersonResultat.tilRestPersonResultat() =
        RestPersonResultat(personIdent = this.personIdent,
                           vilkårResultater = this.vilkårResultater.map { vilkårResultat ->
                               RestVilkårResultat(
                                       resultat = vilkårResultat.resultat,
                                       erAutomatiskVurdert = vilkårResultat.erAutomatiskVurdert,
                                       erEksplisittAvslagPåSøknad = vilkårResultat.erEksplisittAvslagPåSøknad,
                                       id = vilkårResultat.id,
                                       vilkårType = vilkårResultat.vilkårType,
                                       periodeFom = vilkårResultat.periodeFom,
                                       periodeTom = vilkårResultat.periodeTom,
                                       begrunnelse = vilkårResultat.begrunnelse,
                                       endretAv = vilkårResultat.endretAv,
                                       endretTidspunkt = vilkårResultat.endretTidspunkt,
                                       behandlingId = vilkårResultat.behandlingId,
                                       erVurdert = vilkårResultat.resultat != Resultat.IKKE_VURDERT || vilkårResultat.versjon > 0
                               )
                           },
                           andreVurderinger = this.andreVurderinger.map { annenVurdering ->
                               RestAnnenVurdering(resultat = annenVurdering.resultat,
                                                  type = annenVurdering.type,
                                                  begrunnelse = annenVurdering.begrunnelse)
                           })