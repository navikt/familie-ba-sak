package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.steg.StegType
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
                          val behandlingResultat: List<RestPersonVilkårResultat>, // TODO: Endret format og navn
                          val vedtakForBehandling: List<RestVedtak?>,
                          val brevType: BrevType, //TODO: Endret format og navn
                          val begrunnelse: String)

fun BehandlingResultat.toRestBehandlingResultat() = this.behandlingResultat.map {
    RestPersonVilkårResultat(personIdent = "12345678910",
                             vurderteVilkår = listOf(
                                     RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                                        resultat = Resultat.JA,
                                                        fom = LocalDate.now(),
                                                        tom = LocalDate.now(),
                                                        begrunnelse = "OK")))
    //TODO: Map behandlingsresultatet delt i perioder til behandlingsresultat delt i personer m/tilhørende vilkår+perioder
    // Gjøre dette i en egen periodeservice?
    //RestVilkårResultat(vilkårType = it.vilkårType, resultat = it.resultat, personIdent = it.person.personIdent.ident)
}

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