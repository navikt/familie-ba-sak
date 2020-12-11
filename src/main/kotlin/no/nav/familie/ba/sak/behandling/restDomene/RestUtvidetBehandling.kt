package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.arbeidsfordeling.domene.RestArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.nare.Resultat
import java.time.LocalDate
import java.time.LocalDateTime

data class RestUtvidetBehandling(val aktiv: Boolean,
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
                                 val resultat: BehandlingResultat,
                                 val vedtakForBehandling: List<RestVedtak>,
                                 val totrinnskontroll: RestTotrinnskontroll?,
                                 val utbetalingsperioder: List<Utbetalingsperiode>,
                                 val personerMedAndeler: List<RestPersonMedAndeler>,
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

fun Behandling.tilRestUtvidetBehandling(restPersoner: List<RestPerson>,
                                        restArbeidsfordelingPåBehandling: RestArbeidsfordelingPåBehandling,
                                        restVedtak: List<RestVedtak>,
                                        restPersonResultater: List<RestPersonResultat>,
                                        restTotrinnskontroll: RestTotrinnskontroll?,
                                        restPersonerMedAndeler: List<RestPersonMedAndeler>,
                                        restOpplysningsplikt: RestOpplysningsplikt?,
                                        utbetalingsperioder: List<Utbetalingsperiode>) =
        RestUtvidetBehandling(behandlingId = this.id,
                              opprettetTidspunkt = this.opprettetTidspunkt,
                              aktiv = this.aktiv,
                              status = this.status,
                              steg = this.steg,
                              stegTilstand = this.behandlingStegTilstand.map { it.toRestBehandlingStegTilstand() },
                              type = this.type,
                              kategori = this.kategori,
                              underkategori = this.underkategori,
                              endretAv = this.endretAv,
                              årsak = this.opprettetÅrsak,
                              personer = restPersoner,
                              arbeidsfordelingPåBehandling = restArbeidsfordelingPåBehandling,
                              skalBehandlesAutomatisk = this.skalBehandlesAutomatisk,
                              vedtakForBehandling = restVedtak,
                              personResultater = restPersonResultater,
                              resultat = this.resultat,
                              totrinnskontroll = restTotrinnskontroll,
                              utbetalingsperioder = utbetalingsperioder,
                              opplysningsplikt = restOpplysningsplikt,
                              personerMedAndeler = restPersonerMedAndeler
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