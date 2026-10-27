package no.nav.familie.ba.sak.internal.tabulærtest

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilKortMånedLangtÅr
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
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtfyltUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.tilIUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.UtfyltValutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.tilIValutakurs
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDate

internal fun hentTekstForBakgrunn() =
    """
# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - ${RandomStringUtils.secure().nextAlphanumeric(10)}

  Bakgrunn:"""

internal fun hentTekstForScenario() =
    """

  Scenario: Plassholdertekst for scenario - ${RandomStringUtils.secure().nextAlphanumeric(10)}"""

internal fun hentTekstForDagensDato(dato: LocalDate) = "\n    Og dagens dato er ${dato.tilddMMyyyy()}"

internal fun hentTekstForFagsak(behandling: Behandling) =
    """
    Gitt følgende fagsaker
    | FagsakId | Fagsaktype | Status |
    | 1 | ${behandling.fagsak.type} | ${behandling.fagsak.status} |"""

internal fun hentTekstForBehandlinger(
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

internal fun hentTekstForPersongrunnlag(
    persongrunnlag: PersonopplysningGrunnlag,
    persongrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
) = """

    Og følgende persongrunnlag
    | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |""" +
    hentPersongrunnlagRader(persongrunnlagForrigeBehandling) +
    hentPersongrunnlagRader(persongrunnlag)

private fun hentPersongrunnlagRader(
    persongrunnlag: PersonopplysningGrunnlag?,
): String =
    persongrunnlag?.personer?.sortedBy { it.fødselsdato }?.joinToString("") {
        """
    | ${persongrunnlag.behandlingId} |${it.aktør.aktørId}|${it.type}|${it.fødselsdato.tilddMMyyyy()}|${it.dødsfall?.dødsfallDato?.tilddMMyyyy() ?: ""}|"""
    } ?: ""

internal fun hentTekstForPersonerFremstiltKravFor(
    behandlingId: Long,
    personerFremstiltKravFor: List<Aktør>,
) = """
    Og med personer fremstilt krav for
    | BehandlingId | AktørId |""" +
    personerFremstiltKravFor.joinToString(separator = "") {
        """
    | $behandlingId | ${it.aktørId} |"""
    }

internal fun lagPersonresultaterTekst(
    behandling: Behandling?,
) = behandling?.let {
    """
    Og lag personresultater for behandling ${it.id}"""
} ?: ""

internal fun hentTekstForVilkårresultater(
    personResultater: Set<PersonResultat>,
    behandlingId: Long,
    persongrunnlag: PersonopplysningGrunnlag,
): String =
    """

    Og legg til nye vilkårresultater for behandling $behandlingId
    | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter |""" +
        tilVilkårResultatRader(personResultater.sorterPåFødselsdato(persongrunnlag))

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

private fun tilVilkårResultatRader(
    personResultater: List<PersonResultat>?,
) = personResultater?.joinToString("\n") { personResultat ->
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

internal fun hentTekstForKompetanse(
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

private fun hentKompetanseRader(
    kompetanser: Collection<Kompetanse>?,
): String =
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

internal fun hentTekstForUtenlandskPeriodebeløp(
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

private fun hentUtenlandskPeriodebeløpRader(
    utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>?,
): String =
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

internal fun hentTekstForValutakurser(
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

private fun hentValutakursRader(
    valutakurser: Collection<Valutakurs>?,
): String =
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

internal fun hentTekstForEndretUtbetaling(
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
    | AktørId  | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |""" + rader
    }
}

private fun hentEndretUtbetalingRader(
    endredeUtbetalinger: List<EndretUtbetalingAndel>?,
): String =
    endredeUtbetalinger
        .orEmpty()
        .mapNotNull { it.tilEndretUtbetalingRad() }
        .joinToString("")

private fun EndretUtbetalingAndel.tilEndretUtbetalingRad(): String? {
    val utfylt = tilIEndretUtbetalingAndel() as? IUtfyltEndretUtbetalingAndel ?: return null

    val aktørIder = utfylt.aktører.joinToString(",") { aktør -> aktør.aktørId }
    val fom = utfylt.fom.førsteDagIInneværendeMåned().tilddMMyyyy()
    val tom = utfylt.tom.sisteDagIInneværendeMåned().tilddMMyyyy()
    val søknadstidspunkt = utfylt.søknadstidspunkt.tilddMMyyyy()
    val avtaletidspunkt = avtaletidspunktDeltBosted?.tilddMMyyyy() ?: ""

    return """
    | $aktørIder |${utfylt.behandlingId}|$fom|$tom|${utfylt.årsak} | ${utfylt.prosent} | $søknadstidspunkt | $avtaletidspunkt |"""
}

internal fun hentTekstForTilkjentYtelse(
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
