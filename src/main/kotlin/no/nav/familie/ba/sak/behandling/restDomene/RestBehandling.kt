package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.vilkår.SamletVilkårResultat
import no.nav.familie.ba.sak.behandling.domene.vilkår.UtfallType
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårType
import java.time.LocalDate
import java.time.LocalDateTime

data class RestBehandling(val aktiv: Boolean,
                          val behandlingId: Long?,
                          val type: BehandlingType,
                          val status: BehandlingStatus,
                          val kategori: BehandlingKategori,
                          val personer: List<RestPerson>,
                          val opprettetTidspunkt: LocalDateTime?,
                          val underkategori: BehandlingUnderkategori,
                          val samletVilkårResultat: List<RestVilkårResultat>,
                          val vedtakForBehandling: List<RestVedtak?>,
                          val resultat: BehandlingResultat,
                          val begrunnelse: String)

fun SamletVilkårResultat.toRestSamletVilkårResultat() = this.samletVilkårResultat.map {
    RestVilkårResultat(vilkårType = it.vilkårType, utfallType = it.utfallType, personIdent = it.person.personIdent.ident)
}

data class RestVilkårResultat (
        val personIdent: String,
        val vilkårType: VilkårType,
        val utfallType: UtfallType
)