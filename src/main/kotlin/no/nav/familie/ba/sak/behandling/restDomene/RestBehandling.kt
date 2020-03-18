package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.vilkår.SamletVilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.nare.core.evaluations.Resultat
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
                          val samletVilkårResultat: List<RestVilkårResultat>,
                          val vedtakForBehandling: List<RestVedtak?>,
                          val resultat: BrevType,
                          val begrunnelse: String)

fun SamletVilkårResultat.toRestSamletVilkårResultat() = this.periodeResultat.map {
    RestVilkårResultat(vilkårType = it.vilkårType, resultat = it.resultat, personIdent = it.person.personIdent.ident)
}

data class RestVilkårResultat(
        val personIdent: String,
        val vilkårType: Vilkår,
        val resultat: Resultat
)