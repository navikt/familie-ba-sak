package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.PeriodeResultat
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate
import java.time.LocalDateTime

data class RestBehandling(val aktiv: Boolean,
                          val behandlingId: Long,
                          val type: BehandlingType,
                          val status: BehandlingStatus,
                          val steg: StegType,
                          val kategori: BehandlingKategori,
                          val personer: List<RestPerson>,
                          val opprettetTidspunkt: LocalDateTime,
                          val underkategori: BehandlingUnderkategori,
                          val behandlingResultat: List<RestPersonVilkårResultat>,
                          val vedtakForBehandling: List<RestVedtak?>,
                          val brevType: BrevType,
                          val begrunnelse: String)

data class RestPersonVilkårResultat(
        val personIdent: String,
        val vurderteVilkår: List<RestVilkårResultat>
)

data class RestVilkårResultat(
        val vilkårType: Vilkår,
        val resultat: Resultat,
        val fom: LocalDate,
        val tom: LocalDate,
        val begrunnelse: String
)

//TODO: Inntil perioder er implementert, lagre resultat fra frontend som et PeriodeResultat i én lenger periode
fun RestPersonVilkårResultat.tilPeriodeResultater(): MutableSet<PeriodeResultat> {
    //this.vurderteVilkår.map { vilkår -> VilkårResultat( 1,periodeResultat =  ,vilkårType = vilkår.vilkårType, resultat = vilkår.resultat ) }
    return mutableSetOf(PeriodeResultat(
            vilkårResultater = mutableSetOf(),
            periodeFom = LocalDate.now(),
            periodeTom = LocalDate.now()))
}


//TODO: Må sortere resultater fra this.periodeResultater på vilkår og se om noen av de er sammenhengende og kan plasseres som et RestVilkårResultat
fun BehandlingResultat.tilRestBehandlingResultat() = listOf(RestPersonVilkårResultat(personIdent = "12345678910",
                                                                                     vurderteVilkår = listOf(
                                                                                             RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                                                                                                resultat = Resultat.JA,
                                                                                                                fom = LocalDate.now(),
                                                                                                                tom = LocalDate.now(),
                                                                                                                begrunnelse = "OK"))))
