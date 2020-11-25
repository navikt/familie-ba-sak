package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.arbeidsfordeling.domene.RestArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.nare.Resultat
import java.time.LocalDate
import java.time.LocalDateTime

data class RestBehandling(val aktiv: Boolean,
                          val arbeidsfordelingPåBehandling: RestArbeidsfordelingPåBehandling,
                          val årsak: BehandlingÅrsak,
                          val skalBehandlesAutomatisk: Boolean,
                          val behandlingId: Long,
                          val type: BehandlingType,
                          val status: BehandlingStatus,
                          val steg: StegType,
                          val stegTilstand: List<RestBehandlingStegTilstand>,
                          val kategori: BehandlingKategori,
                          val personer: List<RestPerson>,
                          val opprettetTidspunkt: LocalDateTime,
                          val underkategori: BehandlingUnderkategori,
                          val personResultater: List<RestPersonResultat>,
                          val samletResultat: BehandlingResultatType,
                          val vedtakForBehandling: List<RestVedtak>,
                          val gjeldendeForUtbetaling: Boolean,
                          val totrinnskontroll: RestTotrinnskontroll?,
                          @Deprecated("Bruk utbetalingsperioder")
                          val beregningOversikt: List<Utbetalingsperiode>,
                          val utbetalingsperioder: List<Utbetalingsperiode>,
                          val personerMedAndelerTilkjentYtelse: List<RestPersonMedAndelerTilkjentYtelse>,
                          val endretAv: String,
                          val opplysningsplikt: RestOpplysningsplikt?)

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
                           vilkårResultater = this.vilkårResultater.map { vilkårResultat ->
                               RestVilkårResultat(
                                       resultat = vilkårResultat.resultat,
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
                           })