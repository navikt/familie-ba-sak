package no.nav.familie.ba.sak.internal.vedtak

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilKortMånedLangtÅr
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
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDate

fun lagTestForVedtaksperioderOgBegrunnelser(
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
    kompetanse: Collection<Kompetanse>,
    kompetanseForrigeBehandling: Collection<Kompetanse>?,
    utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    utenlandskePeriodebeløpForrigeBehandling: Collection<UtenlandskPeriodebeløp>?,
    valutakurser: Collection<Valutakurs>,
    valutakurserForrigeBehandling: Collection<Valutakurs>?,
    personerFremstiltKravFor: List<Aktør>,
    testSpesifikkeTekster: String,
): String {
    val test =
        lagGenereltTestoppsettForVedtaksperioderOgBegrunnelser(
            behandling = behandling,
            forrigeBehandling = forrigeBehandling,
            persongrunnlag = persongrunnlag,
            persongrunnlagForrigeBehandling = persongrunnlagForrigeBehandling,
            personResultater = personResultater,
            personResultaterForrigeBehandling = personResultaterForrigeBehandling,
            andeler = andeler,
            andelerForrigeBehandling = andelerForrigeBehandling,
            endredeUtbetalinger = endredeUtbetalinger,
            endredeUtbetalingerForrigeBehandling = endredeUtbetalingerForrigeBehandling,
            kompetanse = kompetanse,
            kompetanseForrigeBehandling = kompetanseForrigeBehandling,
            utenlandskePeriodebeløp = utenlandskePeriodebeløp,
            utenlandskePeriodebeløpForrigeBehandling = utenlandskePeriodebeløpForrigeBehandling,
            valutakurser = valutakurser,
            valutakurserForrigeBehandling = valutakurserForrigeBehandling,
            personerFremstiltKravFor = personerFremstiltKravFor,
        ) + testSpesifikkeTekster
    return test.anonymiser(persongrunnlag, persongrunnlagForrigeBehandling, forrigeBehandling, behandling)
}

private fun lagGenereltTestoppsettForVedtaksperioderOgBegrunnelser(
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
    kompetanse: Collection<Kompetanse>,
    kompetanseForrigeBehandling: Collection<Kompetanse>?,
    utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    utenlandskePeriodebeløpForrigeBehandling: Collection<UtenlandskPeriodebeløp>?,
    valutakurser: Collection<Valutakurs>,
    valutakurserForrigeBehandling: Collection<Valutakurs>?,
    personerFremstiltKravFor: List<Aktør>,
): String {
    val test =
        """
# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - ${RandomStringUtils.secure().nextAlphanumeric(10)}

  Bakgrunn:""" +
            hentTekstForFagsak(behandling) +
            hentTekstForBehandlinger(behandling, forrigeBehandling) +
            hentTekstForPersongrunnlag(persongrunnlag, persongrunnlagForrigeBehandling) +
            """
      
  Scenario: Plassholdertekst for scenario - ${RandomStringUtils.secure().nextAlphanumeric(10)}
    Og dagens dato er ${LocalDate.now().tilddMMyyyy()}""" +
            hentTekstForPersonerFremstiltKravFor(behandling.id, personerFremstiltKravFor) +
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
    
    Når vedtaksperiodene genereres for behandling ${behandling.id}
"""
    return test
}

private fun String.anonymiser(
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

private fun hentTekstForFagsak(behandling: Behandling) =
    """
    Gitt følgende fagsaker
    | FagsakId | Fagsaktype | Status |
    | 1 | ${behandling.fagsak.type} | ${behandling.fagsak.status} |"""

private fun hentTekstForBehandlinger(
    behandling: Behandling,
    forrigeBehandling: Behandling?,
) = """

    Gitt følgende behandlinger
    | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |${
    forrigeBehandling?.let {
        """ 
      | ${it.id} | 1 |           | ${it.resultat} | ${it.opprettetÅrsak} | ${if (it.skalBehandlesAutomatisk) "Ja" else "Nei"} | ${it.kategori} | ${it.status} |"""
    } ?: ""
}
    | ${behandling.id} | 1 | ${forrigeBehandling?.id ?: ""} |${behandling.resultat} | ${behandling.opprettetÅrsak} | ${if (behandling.skalBehandlesAutomatisk) "Ja" else "Nei"} | ${behandling.kategori} | ${behandling.status} |"""

private fun hentTekstForPersongrunnlag(
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
) = """

    Og følgende persongrunnlag
    | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |""" +
    hentPersongrunnlagRader(persongrunnlagForrigeBehandling) +
    hentPersongrunnlagRader(persongrunnlag)

private fun hentPersongrunnlagRader(persongrunnlag: PersonopplysningGrunnlag?): String =
    persongrunnlag?.personer?.sortedBy { it.fødselsdato }?.joinToString("") {
        """
    | ${persongrunnlag.behandlingId} |${it.aktør.aktørId}|${it.type}|${it.fødselsdato.tilddMMyyyy()}|${it.dødsfall?.dødsfallDato?.tilddMMyyyy() ?: ""}|"""
    } ?: ""

private fun hentTekstForPersonerFremstiltKravFor(
    behandlingId: Long?,
    personerFremstiltKravFor: List<Aktør>,
) = """
    Og med personer fremstilt krav for
    | BehandlingId | AktørId |""" +
    personerFremstiltKravFor.joinToString(separator = "") {
        """
    | $behandlingId | ${it.aktørId} |"""
    }

private fun lagPersonresultaterTekst(behandling: Behandling?) =
    behandling?.let {
        """
    Og lag personresultater for behandling ${it.id}"""
    } ?: ""

private fun hentTekstForVilkårresultater(
    personResultater: List<PersonResultat>?,
    behandlingId: Long?,
): String {
    if (personResultater == null || behandlingId == null) {
        return ""
    }

    return """

    Og legg til nye vilkårresultater for behandling $behandlingId
    | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter |""" +
        tilVilkårResultatRader(personResultater)
}

private data class VilkårResultatRad(
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
            }.toList()
            .joinToString("") { (vilkårResultatRad, vilkårResultater) ->
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

private fun Set<PersonResultat>.sorterPåFødselsdato(persongrunnlag: PersonopplysningGrunnlag) = this.sortedBy { personresultat -> persongrunnlag.personer.single { personresultat.aktør == it.aktør }.fødselsdato }

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

private fun hentTekstForUtenlandskPeriodebeløp(
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

    Og med utenlandsk periodebeløp
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
                utenlandskPeriodebeløp.fom.tilKortMånedLangtÅr()
            }|${
                utenlandskPeriodebeløp.tom?.tilKortMånedLangtÅr() ?: ""
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

private fun hentTekstForValutakurser(
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

    Og med valutakurser
    | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs | Vurderingsform |""" +
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
            }|${
                valutakurs.vurderingsform
            }|"""
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
    | ${it.personer.joinToString(",") { person -> person.aktør.aktørId }} |${it.behandlingId}|${
                it.fom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                it.tom.sisteDagIInneværendeMåned().tilddMMyyyy()
            }|${it.årsak} | ${it.prosent} | ${it.søknadstidspunkt.tilddMMyyyy()} | ${if (it is UtfyltEndretUtbetalingAndelDeltBosted) it.avtaletidspunktDeltBosted else ""} |"""
        } ?: ""

private fun hentTekstForTilkjentYtelse(
    andeler: List<AndelTilkjentYtelse>,
    persongrunnlag: PersonopplysningGrunnlag,
    andelerForrigeBehandling: List<AndelTilkjentYtelse>?,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
) = """

    Og med andeler tilkjent ytelse
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
        )?.joinToString("") {
            """
    | ${it.aktør.aktørId} |${it.behandlingId}|${
                it.stønadFom.førsteDagIInneværendeMåned().tilddMMyyyy()
            }|${
                it.stønadTom.sisteDagIInneværendeMåned().tilddMMyyyy()
            }|${it.kalkulertUtbetalingsbeløp}| ${it.type} | ${it.prosent} | ${it.sats} | """
        } ?: ""
