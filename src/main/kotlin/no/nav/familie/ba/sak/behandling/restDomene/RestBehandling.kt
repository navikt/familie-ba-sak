package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
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
                          val personResultater: List<RestPersonResultat>,
                          val vedtakForBehandling: List<RestVedtak?>)

data class RestPersonResultat(
        val personIdent: String,
        val vilkårResultater: List<RestVilkårResultat>?
)

data class RestVilkårResultat(
        val vilkårType: Vilkår,
        val resultat: Resultat,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        val begrunnelse: String
)

fun PersonResultat.tilRestPersonResultat() = RestPersonResultat(personIdent = this.personIdent,
                                                                vilkårResultater = this.vilkårResultater.map { resultat ->
                                                                       RestVilkårResultat(resultat = resultat.resultat,
                                                                                          vilkårType = resultat.vilkårType,
                                                                                          periodeFom = resultat.periodeFom,
                                                                                          periodeTom = resultat.periodeTom,
                                                                                          begrunnelse = resultat.begrunnelse
                                                                                          )
                                                                   })