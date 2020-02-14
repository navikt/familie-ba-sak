package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.domene.vilkår.SamletVilkårResultat
import no.nav.familie.ba.sak.behandling.domene.vilkår.UtfallType
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårType

data class RestBehandling(val aktiv: Boolean,
                          val behandlingId: Long?,
                          val type: BehandlingType,
                          val status: BehandlingStatus,
                          val kategori: BehandlingKategori,
                          val underkategori: BehandlingUnderkategori,
                          val samletVilkårResultat: List<RestVilkårResultat>?,
                          val barnasFødselsnummer: List<String?>?,
                          val vedtakForBehandling: List<RestVedtak?>)

fun SamletVilkårResultat.toRestSamletVilkårResultat() = this.samletVilkårResultat.map {
    RestVilkårResultat(vilkårType = it.vilkårType, utfallType = it.utfallType, personIdent = it.person.personIdent.ident)
}

data class RestVilkårResultat (
        val personIdent: String,
        val vilkårType: VilkårType,
        val utfallType: UtfallType
)