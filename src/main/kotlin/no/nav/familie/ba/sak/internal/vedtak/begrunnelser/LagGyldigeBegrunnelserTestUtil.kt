package no.nav.familie.ba.sak.internal.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.tilddMMyyyy
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.UtfyltEndretUtbetalingAndelDeltBosted
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilIEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtfyltUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.tilIUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.UtfyltValutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.tilIValutakurs
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
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
    utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    utenlandskePeriodebeløpForrigeBehandling: Collection<UtenlandskPeriodebeløp>?,
    valutakurser: Collection<Valutakurs>,
    valutakurserForrigeBehandling: Collection<Valutakurs>?,
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
    Og følgende dagens dato ${LocalDate.now().tilddMMyyyy()}""" +
            lagPersonresultaterTekst(forrigeBehandling) +
            lagPersonresultaterTekst(behandling) +
            hentTekstForVilkårresultater(
                personResultaterForrigeBehandling?.sorterPåFødselsdato(persongrunnlagForrigeBehandling!!),
                forrigeBehandling?.id,
            ) +
            hentTekstForVilkårresultater(personResultater.sorterPåFødselsdato(persongrunnlag), behandling.id) +
            hentTekstForKompetanse(kompetanse, kompetanseForrigeBehandling) +
            hentTekstForUtenlandskPeriodebeløp(utenlandskePeriodebeløp, utenlandskePeriodebeløpForrigeBehandling) +
            hentTekstForValutakurser(valutakurser, valutakurserForrigeBehandling) +
            hentTekstForEndretUtbetaling(endredeUtbetalinger, endredeUtbetalingerForrigeBehandling) +
            hentTekstForTilkjentYtelse(andeler, persongrunnlag, andelerForrigeBehandling, persongrunnlagForrigeBehandling) + """
    
    Når vedtaksperiodene genereres for behandling ${behandling.id}""" +
            hentTekstForGyligeBegrunnelserForVedtaksperiodene(vedtaksperioder) +
            hentTekstValgteBegrunnelser(behandling.id, vedtaksperioder) +
            hentTekstBrevPerioder(behandling.id, vedtaksperioder) +
            hentBrevBegrunnelseTekster(behandling.id, vedtaksperioder) +
            hentEØSBrevBegrunnelseTekster(behandling.id, vedtaksperioder) + """
"""
    return test.anonymiser(persongrunnlag, persongrunnlagForrigeBehandling, forrigeBehandling, behandling)
}

fun String.anonymiser(
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
    forrigeBehandling: Behandling?,
    behandling: Behandling,
): String {
    val personerSomTestes: Set<Person> =
        persongrunnlag.personer.toSet() + (persongrunnlagForrigeBehandling?.personer?.toSet() ?: emptySet())
    val aktørIder = personerSomTestes.sortedBy { it.fødselsdato }.map { it.aktør.aktørId }

    val behandlinger = listOfNotNull(forrigeBehandling?.id, behandling.id)

    val testMedAnonymeAktørIder =
        aktørIder.foldIndexed(this) { index, acc, aktørId ->
            acc.replace(aktørId, (index + 1).toString())
        }
    return behandlinger.foldIndexed(testMedAnonymeAktørIder) { index, acc, behandlingId ->
        acc.replace(behandlingId.toString(), (index + 1).toString())
    }
}

private fun lagPersonresultaterTekst(behandling: Behandling?) =
    behandling?.let {
        """
    Og lag personresultater for begrunnelse for behandling ${it.id}"""
    } ?: ""

fun hentTekstForFagsak(behandling: Behandling) =
    """
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype | Status |
      | 1 | ${behandling.fagsak.type} | ${behandling.fagsak.status} |"""

fun hentTekstForBehandlinger(
    behandling: Behandling,
    forrigeBehandling: Behandling?,
) =
    """

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |${
        forrigeBehandling?.let {
            """ 
      | ${it.id} | 1 |           | ${it.resultat} | ${it.opprettetÅrsak} | ${if (it.skalBehandlesAutomatisk) "Ja" else "Nei"} | ${it.kategori} | ${it.status} |"""
        } ?: ""
    }
      | ${behandling.id} | 1 | ${forrigeBehandling?.id ?: ""} |${behandling.resultat} | ${behandling.opprettetÅrsak} | ${if (behandling.skalBehandlesAutomatisk) "Ja" else "Nei"} | ${behandling.kategori} | ${behandling.status} |"""

fun hentTekstForPersongrunnlag(
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
) =
    """
    
    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |""" +
        hentPersongrunnlagRader(persongrunnlagForrigeBehandling) +
        hentPersongrunnlagRader(persongrunnlag)

private fun hentPersongrunnlagRader(persongrunnlag: PersonopplysningGrunnlag?): String =
    persongrunnlag?.personer?.sortedBy { it.fødselsdato }?.joinToString("") {
        """
      | ${persongrunnlag.behandlingId} |${it.aktør.aktørId}|${it.type}|${it.fødselsdato.tilddMMyyyy()}|${it.dødsfall?.dødsfallDato?.tilddMMyyyy() ?: ""}|"""
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
      | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter |""" +
        tilVilkårResultatRader(personResultater)
}

data class VilkårResultatRad(
    val aktørId: String,
    val utdypendeVilkårsvurderinger: Set<UtdypendeVilkårsvurdering>,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val resultat: Resultat,
    val erEksplisittAvslagPåSøknad: Boolean?,
    val standardbegrunnelser: List<IVedtakBegrunnelse>,
    val vurderesEtter: Regelverk?,
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
                    it.standardbegrunnelser,
                    it.vurderesEtter,
                )
            }.toList().joinToString("") { (vilkårResultatRad, vilkårResultater) ->
                "\n | ${vilkårResultatRad.aktørId} " +
                    "| ${vilkårResultater.map { it.vilkårType }.joinToString(",")} " +
                    "| ${vilkårResultatRad.utdypendeVilkårsvurderinger.joinToString(",")} " +
                    "| ${vilkårResultatRad.fom?.tilddMMyyyy() ?: ""} " +
                    "| ${vilkårResultatRad.tom?.tilddMMyyyy() ?: ""} " +
                    "| ${vilkårResultatRad.resultat} " +
                    "| ${if (vilkårResultatRad.erEksplisittAvslagPåSøknad == true) "Ja" else "Nei"} " +
                    "| ${vilkårResultatRad.standardbegrunnelser.joinToString(",")}" +
                    "| ${vilkårResultatRad.vurderesEtter ?: ""} " +
                    "| "
            }
    } ?: ""

fun hentTekstForTilkjentYtelse(
    andeler: List<AndelTilkjentYtelse>,
    persongrunnlag: PersonopplysningGrunnlag,
    andelerForrigeBehandling: List<AndelTilkjentYtelse>?,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
) =
    """

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats | """ +
        hentAndelRader(andelerForrigeBehandling, persongrunnlagForrigeBehandling) +
        "\n" +
        hentAndelRader(andeler, persongrunnlag)

private fun hentAndelRader(
    andeler: List<AndelTilkjentYtelse>?,
    persongrunnlag: PersonopplysningGrunnlag?,
): String =
    andeler
        ?.sortedWith(
            compareBy(
                { persongrunnlag?.personer?.single { person -> person.aktør == it.aktør }?.fødselsdato },
                { it.stønadFom },
                { it.stønadTom },
            ),
        )
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
    val rader =
        hentEndretUtbetalingRader(endredeUtbetalingerForrigeBehandling) +
            hentEndretUtbetalingRader(endredeUtbetalinger)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og med endrede utbetalinger for begrunnelse
      | AktørId  | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |""" +
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
            }|${it.årsak} | ${it.prosent} | ${it.søknadstidspunkt.tilddMMyyyy()} | ${if (it is UtfyltEndretUtbetalingAndelDeltBosted) it.avtaletidspunktDeltBosted else ""} |"""
        } ?: ""

fun hentTekstForKompetanse(
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

fun hentTekstForUtenlandskPeriodebeløp(
    utenlandskPeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    utenlandskPeriodebeløpForrigeBehandling: Collection<UtenlandskPeriodebeløp>?,
): String {
    val rader =
        hentUtenlandskPeriodebeløpRader(utenlandskPeriodebeløpForrigeBehandling) +
            hentUtenlandskPeriodebeløpRader(utenlandskPeriodebeløp)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og med utenlandsk periodebeløp for begrunnelse
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |""" +
            rader
    }
}

private fun hentUtenlandskPeriodebeløpRader(utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>?): String =
    utenlandskePeriodebeløp
        ?.map { it.tilIUtenlandskPeriodebeløp() }
        ?.filterIsInstance<UtfyltUtenlandskPeriodebeløp>()
        ?.joinToString("") { utenlandskPeriodebeløp ->
            """
      | ${
                utenlandskPeriodebeløp.barnAktører.joinToString(", ") { it.aktørId }
            } |${
                utenlandskPeriodebeløp.fom.tilKortString()
            }|${
                utenlandskPeriodebeløp.tom?.tilKortString() ?: ""
            }|${
                utenlandskPeriodebeløp.behandlingId
            }|${
                utenlandskPeriodebeløp.beløp
            }|${
                utenlandskPeriodebeløp.valutakode
            }|${
                utenlandskPeriodebeløp.intervall
            }|${
                utenlandskPeriodebeløp.utbetalingsland
            }|"""
        } ?: ""

fun hentTekstForValutakurser(
    valutakurser: Collection<Valutakurs>,
    valutakurserForrigeBehandling: Collection<Valutakurs>?,
): String {
    val rader =
        hentValutakursRader(valutakurserForrigeBehandling) +
            hentValutakursRader(valutakurser)

    return if (rader.isEmpty()) {
        ""
    } else {
        """

    Og med valutakurs for begrunnelse
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs |""" +
            rader
    }
}

private fun hentValutakursRader(valutakurser: Collection<Valutakurs>?): String =
    valutakurser
        ?.map { it.tilIValutakurs() }
        ?.filterIsInstance<UtfyltValutakurs>()
        ?.joinToString("") { valutakurs ->
            """
      | ${
                valutakurs.barnAktører.joinToString(", ") { it.aktørId }
            } |${
                valutakurs.fom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                valutakurs.tom?.sisteDagIInneværendeMåned()?.tilddMMyyyy() ?: ""
            }|${
                valutakurs.behandlingId
            }|${
                valutakurs.valutakursdato
            }|${
                valutakurs.valutakode
            }|${
                valutakurs.kurs
            }|"""
        } ?: ""

fun hentTekstForGyligeBegrunnelserForVedtaksperiodene(
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """
        
    Så forvent at følgende begrunnelser er gyldige
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Ugyldige begrunnelser |""" +
        hentVedtaksperiodeRaderForGyldigeBegrunnelser(vedtaksperioder)

fun hentVedtaksperiodeRaderForGyldigeBegrunnelser(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.type} | | ${vedtaksperiode.begrunnelser.joinToString { it.standardbegrunnelse.name }} | |""" +
            if (vedtaksperiode.eøsBegrunnelser.isNotEmpty()) {
                """
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.type} | EØS_FORORDNINGEN | ${vedtaksperiode.eøsBegrunnelser.joinToString { it.begrunnelse.name }} | |
                """
            } else {
                ""
            }
    }

private fun Set<PersonResultat>.sorterPåFødselsdato(persongrunnlag: PersonopplysningGrunnlag) =
    this.sortedBy { personresultat -> persongrunnlag.personer.single { personresultat.aktør == it.aktør }.fødselsdato }

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
        | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} | ${vedtaksperiode.begrunnelser.joinToString { it.standardbegrunnelse.name }} | ${vedtaksperiode.eøsBegrunnelser.joinToString { it.begrunnelse.name }} | |"""
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
        | | ${vedtaksperiode.fom?.tilMånedÅr() ?: ""} | ${vedtaksperiode.tom?.tilMånedÅr() ?: ""} | | | | |"""
    }

fun hentBrevBegrunnelseTekster(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
): String {
    return vedtaksperioder.filter { (it.begrunnelser).isNotEmpty() }.joinToString("") { vedtaksperiode ->
        """

    Så forvent følgende brevbegrunnelser for behandling $behandlingId i periode ${vedtaksperiode.fom?.tilddMMyyyy() ?: "-"} til ${vedtaksperiode.tom?.tilddMMyyyy() ?: "-"}
        | Begrunnelse | Type | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |""" +
            vedtaksperiode.begrunnelser.map { it.standardbegrunnelse }.joinToString("") {
                """
        | $it | STANDARD |               |                      |             |                                      |         |       |                  |                         |                               |"""
            }
    }
}

fun hentEØSBrevBegrunnelseTekster(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
): String {
    return vedtaksperioder.filter { (it.eøsBegrunnelser).isNotEmpty() }.joinToString("") { vedtaksperiode ->
        """

    Så forvent følgende brevbegrunnelser for behandling $behandlingId i periode ${vedtaksperiode.fom?.tilddMMyyyy() ?: "-"} til ${vedtaksperiode.tom?.tilddMMyyyy() ?: "-"}
        | Begrunnelse | Type | Barnas fødselsdatoer | Antall barn | Målform | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland | """ +
            vedtaksperiode.eøsBegrunnelser.map { it.begrunnelse }.joinToString("") {
                """
        | $it | EØS | | | | | | | | |"""
            }
    }
}
