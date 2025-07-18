package no.nav.familie.ba.sak.ekstern.restDomene

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelseDeserializer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.ResultatBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.Fødselsnummer
import java.time.LocalDate
import java.time.LocalDateTime

data class RestPersonResultat(
    val personIdent: String,
    val vilkårResultater: List<RestVilkårResultat>,
    val andreVurderinger: List<RestAnnenVurdering> = emptyList(),
) {
    // Bruker init til å validere personidenten
    init {
        Fødselsnummer(personIdent)
    }
}

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
    val erVurdert: Boolean = false,
    val erAutomatiskVurdert: Boolean = false,
    val erEksplisittAvslagPåSøknad: Boolean? = null,
    @JsonDeserialize(using = IVedtakBegrunnelseDeserializer::class)
    val avslagBegrunnelser: List<IVedtakBegrunnelse>? = emptyList(),
    val vurderesEtter: Regelverk? = null,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
    val resultatBegrunnelse: ResultatBegrunnelse? = null,
    val begrunnelseForManuellKontroll: String? = null,
) {
    fun erAvslagUtenPeriode() = this.erEksplisittAvslagPåSøknad == true && this.periodeFom == null && this.periodeTom == null

    fun harFremtidigTom() = this.periodeTom == null || this.periodeTom.isAfter(LocalDate.now().sisteDagIMåned())
}

fun PersonResultat.tilRestPersonResultat() =
    RestPersonResultat(
        personIdent = this.aktør.aktivFødselsnummer(),
        vilkårResultater =
            this.vilkårResultater.map { vilkårResultat ->
                RestVilkårResultat(
                    resultat = vilkårResultat.resultat,
                    resultatBegrunnelse = vilkårResultat.resultatBegrunnelse,
                    erAutomatiskVurdert = vilkårResultat.erAutomatiskVurdert,
                    erEksplisittAvslagPåSøknad = vilkårResultat.erEksplisittAvslagPåSøknad,
                    id = vilkårResultat.id,
                    vilkårType = vilkårResultat.vilkårType,
                    periodeFom = vilkårResultat.periodeFom,
                    periodeTom = vilkårResultat.periodeTom,
                    begrunnelse = vilkårResultat.begrunnelse,
                    endretAv = vilkårResultat.endretAv,
                    endretTidspunkt = vilkårResultat.endretTidspunkt,
                    behandlingId = vilkårResultat.sistEndretIBehandlingId,
                    erVurdert = vilkårResultat.resultat != Resultat.IKKE_VURDERT || vilkårResultat.versjon > 0,
                    avslagBegrunnelser = vilkårResultat.standardbegrunnelser,
                    vurderesEtter = vilkårResultat.vurderesEtter,
                    utdypendeVilkårsvurderinger = vilkårResultat.utdypendeVilkårsvurderinger,
                    begrunnelseForManuellKontroll = vilkårResultat.begrunnelseForManuellKontroll?.begrunnelse(vilkårResultat.vilkårType),
                )
            },
        andreVurderinger =
            this.andreVurderinger.map { annenVurdering ->
                annenVurdering.tilRestAnnenVurdering()
            },
    )

fun RestPersonResultat.fjernAutomatiskBegrunnelse() =
    copy(
        vilkårResultater =
            vilkårResultater.map { vilkårResultat ->
                if (vilkårResultat.begrunnelse.startsWith("Fylt ut automatisk fra registerdata i PDL")) {
                    vilkårResultat.copy(begrunnelse = "", erAutomatiskVurdert = false, begrunnelseForManuellKontroll = null)
                } else {
                    vilkårResultat
                }
            },
    )
