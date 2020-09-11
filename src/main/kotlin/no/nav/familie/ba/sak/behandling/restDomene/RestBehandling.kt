package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
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
                          val samletResultat: BehandlingResultatType,
                          val vedtakForBehandling: List<RestVedtak>,
                          val gjeldendeForUtbetaling: Boolean,
                          val totrinnskontroll: RestTotrinnskontroll?,
                          val beregningOversikt: List<RestBeregningOversikt>,
                          val endretAv: String)

data class RestPersonResultat(
        val personIdent: String,
        val vilkårResultater: List<RestVilkårResultat>
)

data class RestNyttVilkår(
        val personIdent: String,
        val vilkårType: Vilkår
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
        val erVurdert: Boolean? = null
)

fun PersonResultat.tilRestPersonResultat() =
        RestPersonResultat(personIdent = this.personIdent,
                           vilkårResultater = this.vilkårResultater.map { resultat ->
                               RestVilkårResultat(
                                       resultat = resultat.resultat,
                                       id = resultat.id,
                                       vilkårType = resultat.vilkårType,
                                       periodeFom = resultat.periodeFom,
                                       periodeTom = resultat.periodeTom,
                                       begrunnelse = resultat.begrunnelse,
                                       endretAv = resultat.endretAv,
                                       endretTidspunkt = resultat.endretTidspunkt,
                                       behandlingId = resultat.behandlingId,
                                       erVurdert = resultat.resultat != Resultat.KANSKJE || resultat.versjon > 0
                               )
                           })