package no.nav.familie.ba.sak.internal.vedtak.vedtaksperioder

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilddMMyyyy
import no.nav.familie.ba.sak.internal.vedtak.begrunnelser.VilkårResultatRad
import no.nav.familie.ba.sak.internal.vedtak.begrunnelser.anonymiser
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilIEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDate

fun lagVedtaksperioderTest(
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
    personerFremstiltKravFor: List<Aktør>,
): String {
    val test =
        """
# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - ${RandomStringUtils.randomAlphanumeric(10)}

  Bakgrunn:""" +
            hentTekstForFagsak(behandling) +
            hentTekstForBehandlinger(behandling, forrigeBehandling) +
            hentTekstForPersongrunnlag(persongrunnlag, persongrunnlagForrigeBehandling) +
            """
      
  Scenario: Plassholdertekst for scenario - ${RandomStringUtils.randomAlphanumeric(10)}
    Og dagens dato er ${LocalDate.now().tilddMMyyyy()}""" +
            hentTekstForPersonerFremstiltKravFor(behandling.id, personerFremstiltKravFor) +
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
            hentTekstForVedtaksperioder(vedtaksperioder, behandling.id) + """
    """

    return test.anonymiser(persongrunnlag, persongrunnlagForrigeBehandling, forrigeBehandling, behandling)
}

private fun lagPersonresultaterTekst(behandling: Behandling?) =
    behandling?.let {
        """
    Og lag personresultater for behandling ${it.id}"""
    } ?: ""

private fun hentTekstForFagsak(behandling: Behandling) =
    """
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1 | ${behandling.fagsak.type} |"""

private fun hentTekstForBehandlinger(
    behandling: Behandling,
    forrigeBehandling: Behandling?,
) =
    """

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |${
        forrigeBehandling?.let {
            """ 
      | ${it.id} | 1 |           | ${it.resultat} | ${it.opprettetÅrsak} |"""
        } ?: ""
    }
      | ${behandling.id} | 1 | ${forrigeBehandling?.id ?: ""} |${behandling.resultat} | ${behandling.opprettetÅrsak} |"""

private fun hentTekstForPersongrunnlag(
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
) =
    """
    
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |""" +
        hentPersongrunnlagRader(persongrunnlagForrigeBehandling) +
        hentPersongrunnlagRader(persongrunnlag)

private fun hentPersongrunnlagRader(persongrunnlag: PersonopplysningGrunnlag?): String =
    persongrunnlag?.personer?.sortedBy { it.fødselsdato }?.joinToString("") {
        """
      | ${persongrunnlag.behandlingId} |${it.aktør.aktørId}|${it.type}|${it.fødselsdato.tilddMMyyyy()}|"""
    } ?: ""

private fun hentTekstForPersonerFremstiltKravFor(
    behandlingId: Long?,
    personerFremstiltKravFor: List<Aktør>,
) =
    """
        Og med personer fremstilt krav for
        | BehandlingId | AktørId |""" +
        personerFremstiltKravFor.joinToString {
            """
        | $behandlingId | ${it.aktørId} |"""
        }

private fun hentTekstForVilkårresultater(
    personResultater: List<PersonResultat>?,
    behandlingId: Long?,
): String {
    if (personResultater == null || behandlingId == null) {
        return ""
    }

    return """
        
    Og legg til nye vilkårresultater for behandling $behandlingId
      | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter |
      """ + tilVilkårResultatRader(personResultater)
}

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
                    it.standardbegrunnelser,
                    it.vurderesEtter,
                )
            }.toList()
            .joinToString("") { (vilkårResultatRad, vilkårResultater) ->
                "| ${vilkårResultatRad.aktørId} " +
                    "| ${vilkårResultater.map { it.vilkårType }.joinToString(",")} " +
                    "| ${vilkårResultatRad.utdypendeVilkårsvurderinger.joinToString(",")} " +
                    "| ${vilkårResultatRad.fom?.tilddMMyyyy() ?: ""} " +
                    "| ${vilkårResultatRad.tom?.tilddMMyyyy() ?: ""} " +
                    "| ${vilkårResultatRad.resultat} " +
                    "| ${if (vilkårResultatRad.erEksplisittAvslagPåSøknad == true) "Ja" else "Nei"}" +
                    "| ${vilkårResultatRad.standardbegrunnelser.joinToString(",")}" +
                    "| ${vilkårResultatRad.vurderesEtter ?: ""}" +
                    "| \n"
            }
    } ?: ""

private fun hentTekstForTilkjentYtelse(
    andeler: List<AndelTilkjentYtelse>,
    andelerForrigeBehandling: List<AndelTilkjentYtelse>?,
) =
    """

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats | """ +
        hentAndelRader(andelerForrigeBehandling) +
        hentAndelRader(andeler)

private fun hentAndelRader(andeler: List<AndelTilkjentYtelse>?): String =
    andeler
        ?.sortedWith(compareBy({ it.aktør.aktivFødselsnummer() }, { it.stønadFom }, { it.stønadTom }))
        ?.joinToString("") {
            """
      | ${it.aktør.aktørId} |${it.behandlingId}|${
                it.stønadFom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                it.stønadTom.sisteDagIInneværendeMåned().tilddMMyyyy()
            }|${it.kalkulertUtbetalingsbeløp}| ${it.type} | ${it.prosent} | ${it.sats} | """
        } ?: ""

private fun hentTekstForEndretUtbetaling(
    endredeUtbetalinger: List<EndretUtbetalingAndel>,
    endredeUtbetalingerForrigeBehandling: List<EndretUtbetalingAndel>?,
): String {
    val rader =
        hentEndretUtbetalingRader(endredeUtbetalingerForrigeBehandling) +
            hentEndretUtbetalingRader(endredeUtbetalinger)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og med endrede utbetalinger
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

private fun hentTekstForKompetanse(
    kompetanse: Collection<Kompetanse>,
    kompetanseForrigeBehandling: Collection<Kompetanse>?,
): String {
    val rader =
        hentKompetanseRader(kompetanseForrigeBehandling) +
            hentKompetanseRader(kompetanse)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og med kompetanser
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

private fun hentTekstForVedtaksperioder(
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
    behandlingId: Long?,
) =
    """
        
    Så forvent følgende vedtaksperioder for behandling $behandlingId
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |""" +
        hentVedtaksperiodeRader(vedtaksperioder)

private fun hentVedtaksperiodeRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") {
        """
      | ${it.fom?.tilddMMyyyy() ?: ""} |${it.tom?.tilddMMyyyy() ?: ""} |${it.type} |               |"""
    }

private fun Set<PersonResultat>.sorterPåFøselsdato(persongrunnlag: PersonopplysningGrunnlag) =
    this.sortedBy { personresultat -> persongrunnlag.personer.single { personresultat.aktør == it.aktør }.fødselsdato }
