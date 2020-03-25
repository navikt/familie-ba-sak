package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.PeriodeResultat
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
                          val periodeResultater: List<RestPeriodeResultat>,
                          val vedtakForBehandling: List<RestVedtak?>,
                          val brevType: BrevType,
                          val begrunnelse: String)

data class RestPeriodeResultat(
        val personIdent: String,
        val periodeFom: LocalDate,
        val periodeTom: LocalDate?,
        val vilkårResultater: List<RestVilkårResultat>?
)

data class RestVilkårResultat(
        val vilkårType: Vilkår,
        val resultat: Resultat
)

fun PeriodeResultat.tilRestPeriodeResultat() = RestPeriodeResultat(personIdent = this.personIdent,
                                                                   periodeFom = this.periodeFom,
                                                                   periodeTom = this.periodeTom,
                                                                   vilkårResultater = this.vilkårResultater.map { resultat ->
                                                                       RestVilkårResultat(resultat = resultat.resultat,
                                                                                          vilkårType = resultat.vilkårType)
                                                                   })