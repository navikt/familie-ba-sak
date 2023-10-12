package no.nav.familie.ba.sak.internal.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilddMMyyyy
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilIEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
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
    kompetanse: Collection<Kompetanse>,
    kompetanseForrigeBehandling: Collection<Kompetanse>?,
) = """
<pre>
# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - ${RandomStringUtils.randomAlphanumeric(10)}

  Bakgrunn:""" +
    hentTekstForFagsak(behandling) +
    hentTekstForBehandlinger(behandling, forrigeBehandling) +
    hentTekstForPersongrunnlag(persongrunnlag, persongrunnlagForrigeBehandling) +
    """
      
  Scenario: Plassholdertekst for scenario - ${RandomStringUtils.randomAlphanumeric(10)}
    Og følgende dagens dato ${LocalDate.now().tilddMMyyyy()}""" +
    lagPersonresultaterTekst(forrigeBehandling) +
    lagPersonresultaterTekst(behandling) +
    hentTekstForVilkårresultater(
        personResultaterForrigeBehandling?.sorterPåFøselsdato(persongrunnlagForrigeBehandling!!),
        forrigeBehandling?.id,
    ) +
    hentTekstForVilkårresultater(personResultater.sorterPåFøselsdato(persongrunnlag), behandling.id) +
    hentTekstForTilkjentYtelse(andeler, andelerForrigeBehandling) +
    hentTekstForEndretUtbetaling(endredeUtbetalinger, endredeUtbetalingerForrigeBehandling) +
    hentTekstForKompetanse(kompetanse, kompetanseForrigeBehandling) + """
    
    Når vedtaksperiodene genereres for behandling ${behandling.id}""" +
    hentTekstForGyligeBegrunnelserForVedtaksperiodene(vedtaksperioder) +
    hentTekstValgteBegrunnelser(behandling.id, vedtaksperioder) +
    hentTekstBrevPerioder(behandling.id, vedtaksperioder) +
    hentBrevBegrunnelseTekster(behandling.id, vedtaksperioder) + """
</pre> 
"""

private fun lagPersonresultaterTekst(behandling: Behandling?) = behandling?.let {
    """
    Og lag personresultater for begrunnelse for behandling ${it.id}"""
} ?: ""

fun hentTekstForFagsak(behandling: Behandling) =
    """
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | ${behandling.fagsak.id} | ${behandling.fagsak.type} |"""

fun hentTekstForBehandlinger(behandling: Behandling, forrigeBehandling: Behandling?) =
    """

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |${
        forrigeBehandling?.let {
            """ 
      | ${it.id} | ${it.fagsak.id} |           | ${it.resultat} | ${it.opprettetÅrsak} | ${if (it.skalBehandlesAutomatisk) "Ja" else "Nei"} |"""
        } ?: ""
    }
      | ${behandling.id} | ${behandling.fagsak.id} | ${forrigeBehandling?.id ?: ""} |${behandling.resultat} | ${behandling.opprettetÅrsak} | ${if (behandling.skalBehandlesAutomatisk) "Ja" else "Nei"} |"""

fun hentTekstForPersongrunnlag(
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
) =
    """
    
    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |""" +
        hentPersongrunnlagRader(persongrunnlagForrigeBehandling) +
        hentPersongrunnlagRader(persongrunnlag)

private fun hentPersongrunnlagRader(persongrunnlag: PersonopplysningGrunnlag?): String =
    persongrunnlag?.personer?.joinToString("") {
        """
      | ${persongrunnlag.behandlingId} |${it.aktør.aktørId}|${it.type}|${it.fødselsdato.tilddMMyyyy()}|"""
    } ?: ""

fun hentTekstForVilkårresultater(
    personResultater: List<PersonResultat>?,
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

private fun tilVilkårResultatRader(personResultater: List<PersonResultat>?) =
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
      | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats | """ +
        hentAndelRader(andelerForrigeBehandling) +
        hentAndelRader(andeler)

private fun hentAndelRader(andeler: List<AndelTilkjentYtelse>?): String = andeler
    ?.sortedWith(compareBy({ it.aktør.aktivFødselsnummer() }, { it.stønadFom }, { it.stønadTom }))
    ?.joinToString("") {
        """
      | ${it.aktør.aktørId} |${it.behandlingId}|${
            it.stønadFom.førsteDagIInneværendeMåned().tilddMMyyyy()
        }|${
            it.stønadTom.sisteDagIInneværendeMåned().tilddMMyyyy()
        }|${it.kalkulertUtbetalingsbeløp}| ${it.type} | ${it.prosent} | ${it.sats} | """
    } ?: ""

fun hentTekstForEndretUtbetaling(
    endredeUtbetalinger: List<EndretUtbetalingAndel>,
    endredeUtbetalingerForrigeBehandling: List<EndretUtbetalingAndel>?,
): String {
    val rader = hentEndretUtbetalingRader(endredeUtbetalingerForrigeBehandling) +
        hentEndretUtbetalingRader(endredeUtbetalinger)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og med endrede utbetalinger for begrunnelse
      | AktørId  | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |""" +
            hentEndretUtbetalingRader(endredeUtbetalingerForrigeBehandling) +
            hentEndretUtbetalingRader(endredeUtbetalinger)
    }
}

private fun hentEndretUtbetalingRader(endredeUtbetalinger: List<EndretUtbetalingAndel>?): String =
    endredeUtbetalinger
        ?.map { it.tilIEndretUtbetalingAndel() }
        ?.filterIsInstance<IUtfyltEndretUtbetalingAndel>()
        ?.joinToString("") {
            """
      | ${it.person.aktør.aktørId} |${it.behandlingId}|${
                it.fom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                it.tom.sisteDagIInneværendeMåned().tilddMMyyyy()
            }|${it.årsak} | ${it.prosent} |"""
        } ?: ""

fun hentTekstForKompetanse(
    kompetanse: Collection<Kompetanse>,
    kompetanseForrigeBehandling: Collection<Kompetanse>?,
): String {
    val rader = hentKompetanseRader(kompetanseForrigeBehandling) +
        hentKompetanseRader(kompetanse)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato | Til dato | Resultat | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |""" +
            rader
    }
}

private fun hentKompetanseRader(kompetanser: Collection<Kompetanse>?): String =
    kompetanser
        ?.map { it.tilIKompetanse() }
        ?.filterIsInstance<UtfyltKompetanse>()
        ?.joinToString("") { kompetanse ->
            """
      | ${
                kompetanse.barnAktører.joinToString(", ") { it.aktørId }
            } |${
                kompetanse.fom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                kompetanse.tom?.sisteDagIInneværendeMåned()?.tilddMMyyyy() ?: ""
            }|${
                kompetanse.resultat
            }|${
                kompetanse.behandlingId
            }|${
                kompetanse.søkersAktivitet
            }|${
                kompetanse.annenForeldersAktivitet
            }|${
                kompetanse.søkersAktivitetsland
            }|${
                kompetanse.annenForeldersAktivitetsland ?: ""
            }|${
                kompetanse.barnetsBostedsland
            } |"""
        } ?: ""

fun hentTekstForGyligeBegrunnelserForVedtaksperiodene(
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """
        
    Så forvent at følgende begrunnelser er gyldige
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk | Gyldige begrunnelser | Ugyldige begrunnelser |""" +
        hentVedtaksperiodeRaderForGyldigeBegrunnelser(vedtaksperioder)

fun hentVedtaksperiodeRaderForGyldigeBegrunnelser(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.type} | | ${vedtaksperiode.begrunnelser.joinToString { it.standardbegrunnelse.name }} | |""" +
            if (vedtaksperiode.eøsBegrunnelser.isNotEmpty()) {
                """
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.type} | EØS_FORORDNINGEN | ${vedtaksperiode.eøsBegrunnelser.joinToString { it.begrunnelse.name }} | |
                """.trimIndent()
            } else {
                ""
            }
    }

private fun Set<PersonResultat>.sorterPåFøselsdato(persongrunnlag: PersonopplysningGrunnlag) =
    this.sortedByDescending { personresultat -> persongrunnlag.personer.single { personresultat.aktør == it.aktør }.fødselsdato }

fun hentTekstValgteBegrunnelser(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """
        
    Og når disse begrunnelsene er valgt for behandling $behandlingId
        | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |""" +
        hentValgteBegrunnelserRader(vedtaksperioder)

fun hentValgteBegrunnelserRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} | ${vedtaksperiode.begrunnelser.joinToString { it.standardbegrunnelse.name }} | ${vedtaksperiode.eøsBegrunnelser.joinToString { it.begrunnelse.name }} | ${vedtaksperiode.fritekster.joinToString()} |"""
    }

fun hentTekstBrevPerioder(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """

    Så forvent følgende brevperioder for behandling $behandlingId
        | Brevperiodetype  | Fra dato   | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |""" +
        hentBrevPeriodeRader(vedtaksperioder)

fun hentBrevPeriodeRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
        | | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} | | | | |"""
    }

fun hentBrevBegrunnelseTekster(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
): String {
    return vedtaksperioder.filter { it.begrunnelser.isNotEmpty() }.joinToString("") { vedtaksperiode ->
        """

    Så forvent følgende brevbegrunnelser for behandling $behandlingId i periode ${vedtaksperiode.fom?.tilddMMyyyy() ?: "-"} til ${vedtaksperiode.tom?.tilddMMyyyy() ?: "-"}
        | Begrunnelse                   | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |""" +
            vedtaksperiode.begrunnelser.map { it.standardbegrunnelse }.joinToString("") {
                """
        | $it |               |                      |             |                                      |         |       |                  |                         |"""
            }
    }
}
