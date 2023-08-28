package no.nav.familie.ba.sak.internal.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilddMMyyyy
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilIEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDate

fun lagGyldigeBegrunnelserTest(
    behandling: Behandling,
    forrigeBehandling: Behandling?,
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
    personResultater: Set<PersonResultat>,
    personResultaterForrigeBehandling: Set<PersonResultat>?,
    andeler: List<AndelTilkjentYtelse>,
    andelerForrigeBehandling: List<AndelTilkjentYtelse>?,
    endredeUtbetalinger: List<EndretUtbetalingAndel>,
    endredeUtbetalingerForrigeBehandling: List<EndretUtbetalingAndel>?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) = """
# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - ${RandomStringUtils.randomAlphanumeric(10)}

  Bakgrunn:""" +
    hentTekstForBehandlinger(behandling, forrigeBehandling) +
    hentTekstForPersongrunnlag(persongrunnlag, persongrunnlagForrigeBehandling) +
    """
      
  Scenario: Plassholdertekst for scenario - ${RandomStringUtils.randomAlphanumeric(10)}
    Og lag personresultater for begrunnelse for behandling ${behandling.id}""" +
    hentTekstForVilkårresultater(personResultater, behandling.id) +
    hentTekstForVilkårresultater(personResultaterForrigeBehandling, forrigeBehandling?.id) +
    hentTekstForTilkjentYtelse(andeler, andelerForrigeBehandling) +
    hentTekstForEndretUtbetaling(endredeUtbetalinger, endredeUtbetalingerForrigeBehandling) + """
    
    Når begrunnelsetekster genereres for behandling ${behandling.id}""" +
    hentTekstForVedtaksperioder(vedtaksperioder)

fun hentTekstForBehandlinger(behandling: Behandling, forrigeBehandling: Behandling?) =
    """
    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |${
        forrigeBehandling?.let {
            """ 
      | ${forrigeBehandling.id} | ${forrigeBehandling.fagsak.id} |           |"""
        } ?: ""
    }
      | ${behandling.id} | ${behandling.fagsak.id} | ${forrigeBehandling?.id ?: ""} |"""

fun hentTekstForPersongrunnlag(
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
) =
    """
    
    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |""" +
        hentPersongrunnlagRader(persongrunnlag) +
        hentPersongrunnlagRader(persongrunnlagForrigeBehandling)

private fun hentPersongrunnlagRader(persongrunnlag: PersonopplysningGrunnlag?): String =
    persongrunnlag?.personer?.joinToString("") {
        """
      | ${persongrunnlag.behandlingId} |${it.aktør.aktørId}|${it.type}|${it.fødselsdato.tilddMMyyyy()}|"""
    } ?: ""

fun hentTekstForVilkårresultater(
    personResultater: Set<PersonResultat>?,
    behandlingId: Long?,
): String {
    if (personResultater == null || behandlingId == null) {
        return ""
    }

    return """
        
    Og legg til nye vilkårresultater for begrunnelse for behandling $behandlingId
      | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag |""" +
        tilVilkårResultatRader(personResultater)
}

data class VilkårResultatRad(
    val aktørId: String,
    val utdypendeVilkårsvurderinger: Set<UtdypendeVilkårsvurdering>,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val resultat: Resultat,
    val erEksplisittAvslagPåSøknad: Boolean?,
)

private fun tilVilkårResultatRader(personResultater: Set<PersonResultat>?) =
    personResultater?.joinToString("\n") { personResultat ->
        personResultat.vilkårResultater
            .sortedBy { it.periodeFom }
            .groupBy {
                VilkårResultatRad(
                    personResultat.aktør.aktørId,
                    it.utdypendeVilkårsvurderinger.toSet(),
                    it.periodeFom,
                    it.periodeTom,
                    it.resultat,
                    it.erEksplisittAvslagPåSøknad,
                )
            }.toList().joinToString("") { (vilkårResultatRad, vilkårResultater) ->
                """
      | ${vilkårResultatRad.aktørId} |${vilkårResultater.map { it.vilkårType }.joinToString(",")}|${
                    vilkårResultatRad.utdypendeVilkårsvurderinger.joinToString(",")
                }|${vilkårResultatRad.fom?.tilddMMyyyy() ?: ""}|${vilkårResultatRad.tom?.tilddMMyyyy() ?: ""}| ${vilkårResultatRad.resultat} | ${if (vilkårResultatRad.erEksplisittAvslagPåSøknad == true) "Ja" else "Nei"} |"""
            }
    } ?: ""

fun hentTekstForTilkjentYtelse(
    andeler: List<AndelTilkjentYtelse>,
    andelerForrigeBehandling: List<AndelTilkjentYtelse>?,
) =
    """

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent |""" +
        hentAndelRader(andeler) +
        hentAndelRader(andelerForrigeBehandling)

private fun hentAndelRader(andeler: List<AndelTilkjentYtelse>?): String =
    andeler?.joinToString("") {
        """
      | ${it.aktør.aktørId} |${it.behandlingId}|${
            it.stønadFom.førsteDagIInneværendeMåned().tilddMMyyyy()
        }|${
            it.stønadTom.førsteDagIInneværendeMåned().tilddMMyyyy()
        }|${it.kalkulertUtbetalingsbeløp}| ${it.type} | ${it.prosent} |"""
    } ?: ""

fun hentTekstForEndretUtbetaling(
    endredeUtbetalinger: List<EndretUtbetalingAndel>,
    endredeUtbetalingerForrigeBehandling: List<EndretUtbetalingAndel>?,
) =
    """

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak             | Prosent |""" +
        hentEndretUtbetalingRader(endredeUtbetalinger) +
        hentEndretUtbetalingRader(endredeUtbetalingerForrigeBehandling)

private fun hentEndretUtbetalingRader(endredeUtbetalinger: List<EndretUtbetalingAndel>?): String =
    endredeUtbetalinger
        ?.map { it.tilIEndretUtbetalingAndel() }
        ?.filterIsInstance<IUtfyltEndretUtbetalingAndel>()
        ?.joinToString("") {
            """
      | ${it.person.aktør.aktørId} |${it.behandlingId}|${
                it.fom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                it.tom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${it.årsak} | ${it.prosent} |"""
        } ?: ""

fun hentTekstForVedtaksperioder(
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """
        
    Så forvent følgende standardBegrunnelser
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |""" +
        hentVedtaksperiodeRader(vedtaksperioder)

fun hentVedtaksperiodeRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") {
        """
      | ${it.fom?.tilddMMyyyy() ?: ""} |${it.tom?.tilddMMyyyy() ?: ""} |${it.type} | | | |"""
    }
