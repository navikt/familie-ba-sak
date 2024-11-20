package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.tilMånedÅrMedium
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET_OG_ENDRET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.filtrerBortIrrelevanteAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.UtbetalingstabellAutomatiskValutajustering
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.AndelUpbOgValutakurs
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingEøs
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøsOppsummering
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilUtfylteKompetanserEtterEndringstidpunktPerAktør
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utbetalingsland
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.tilUtbetaltFraAnnetLand
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.erIkkeTom
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.splitPerTidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import tilLandNavn
import java.time.LocalDate

fun hentAutomatiskVedtaksbrevtype(behandling: Behandling): Brevmal {
    val behandlingÅrsak = behandling.opprettetÅrsak
    val fagsakStatus = behandling.fagsak.status

    return when (behandlingÅrsak) {
        BehandlingÅrsak.FØDSELSHENDELSE -> {
            if (fagsakStatus == FagsakStatus.LØPENDE) {
                Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR
            } else {
                Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN
            }
        }

        BehandlingÅrsak.OMREGNING_18ÅR,
        BehandlingÅrsak.SMÅBARNSTILLEGG,
        BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG,
        -> Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG

        else -> throw Feil("Det er ikke laget funksjonalitet for automatisk behandling for $behandlingÅrsak")
    }
}

// Dokumenttittel legges på i familie-integrasjoner basert på dokumenttype
// Denne funksjonen bestemmer om dokumenttittelen skal overstyres eller ikke
fun hentOverstyrtDokumenttittel(behandling: Behandling): String? =
    if (behandling.type == BehandlingType.REVURDERING) {
        behandling.opprettetÅrsak.hentOverstyrtDokumenttittelForOmregningsbehandling() ?: when {
            listOf(
                INNVILGET,
                DELVIS_INNVILGET,
                INNVILGET_OG_ENDRET,
                INNVILGET_OG_OPPHØRT,
                DELVIS_INNVILGET_OG_OPPHØRT,
                ENDRET_OG_OPPHØRT,
            ).contains(behandling.resultat) -> "Vedtak om endret barnetrygd"

            behandling.resultat.erFortsattInnvilget() -> "Vedtak om fortsatt barnetrygd"
            else -> null
        }
    } else {
        null
    }

fun hjemlerTilHjemmeltekst(
    hjemler: List<String>,
    lovForHjemmel: String,
): String =
    when (hjemler.size) {
        0 -> throw Feil("Kan ikke lage hjemmeltekst for $lovForHjemmel når ingen begrunnelser har hjemler fra $lovForHjemmel knyttet til seg.")
        1 -> "§ ${hjemler[0]}"
        else -> "§§ ${Utils.slåSammen(hjemler)}"
    }

fun Collection<String>.slåSammen(): String =
    this.reduceIndexed { index, acc, s ->
        when (index) {
            0 -> s
            this.size - 1 -> "$acc og $s"
            else -> "$acc, $s"
        }
    }

fun hentVirkningstidspunktForDødsfallbrev(
    opphørsperioder: List<VedtaksperiodeMedBegrunnelser>,
    behandlingId: Long,
): String {
    val virkningstidspunkt =
        opphørsperioder
            .maxOfOrNull { it.fom ?: TIDENES_MORGEN }

    if (virkningstidspunkt == null) throw Feil("Fant ikke opphørdato ved generering av dødsfallbrev på behandling $behandlingId")
    if (virkningstidspunkt > LocalDate.now().plusMonths(1).sisteDagIMåned()) {
        throw Feil("Opphørsdato for dødsfallbrev på behandling $behandlingId er lenger frem i tid enn neste måned")
    }
    return virkningstidspunkt.tilMånedÅr()
}

fun skalHenteUtbetalingerEøs(
    endringstidspunkt: LocalDate,
    valutakurser: List<Valutakurs>,
): Boolean {
    // Hvis endringstidspunktet er satt til tidenes ende så returner vi bare false da det ikke finnes valutakurser etter det tidspunktet.
    if (endringstidspunkt == TIDENES_ENDE) return false

    val valutakurserEtterEndringtidspunktet =
        valutakurser
            .tilSeparateTidslinjerForBarna()
            .mapValues { (_, valutakursTidslinjeForBarn) -> valutakursTidslinjeForBarn.beskjærFraOgMed(endringstidspunkt.tilMånedTidspunkt()) }

    return valutakurserEtterEndringtidspunktet.any { it.value.erIkkeTom() }
}

fun hentLandOgStartdatoForUtbetalingstabell(
    endringstidspunkt: MånedTidspunkt,
    landkoder: Map<String, String>,
    kompetanser: Collection<Kompetanse>,
): UtbetalingstabellAutomatiskValutajustering {
    val utfylteSekundærlandsKompetanserEtterEndringstidspunkt =
        kompetanser
            .filter { it.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND }
            .tilUtfylteKompetanserEtterEndringstidpunktPerAktør(endringstidspunkt)

    if (utfylteSekundærlandsKompetanserEtterEndringstidspunkt.isEmpty()) {
        throw Feil("Finner ingen kompetanser etter endringstidspunkt")
    }

    val eøsLandMedUtbetalinger =
        utfylteSekundærlandsKompetanserEtterEndringstidspunkt.values
            .flatten()
            .map {
                it.utbetalingsland()
            }.toSet()
            .map { it.tilLandNavn(landkoder).navn }
    return UtbetalingstabellAutomatiskValutajustering(utbetalingerEosLand = eøsLandMedUtbetalinger.slåSammen(), utbetalingerEosMndAar = endringstidspunkt.tilYearMonth().tilMånedÅr())
}

fun hentUtbetalingerPerMndEøs(
    endringstidspunkt: LocalDate,
    andelTilkjentYtelserForBehandling: List<AndelTilkjentYtelse>,
    utenlandskePeriodebeløp: List<UtenlandskPeriodebeløp>,
    valutakurser: List<Valutakurs>,
    endretutbetalingAndeler: List<EndretUtbetalingAndel>,
): Map<String, UtbetalingMndEøs> {
    // Ønsker kun andeler som ikke er satt til 0 pga endret utbetaling andel med årsakene ALLEREDE_UTBETALT, ENDRE_MOTTAKER eller ETTERBETALING_3ÅR
    val filtrerteAndelTilkjentYtelser = andelTilkjentYtelserForBehandling.filtrerBortIrrelevanteAndeler(endretutbetalingAndeler)
    val andelerForVedtaksperioderPerAktørOgType = filtrerteAndelTilkjentYtelser.tilTidslinjerPerAktørOgType()

    // Ønsker kun andeler etter endringstidspunkt så beskjærer fra og med endringstidspunktet
    val andelerForVedtaksperioderPerAktørOgTypeAvgrensetTilVedtaksperioder =
        andelerForVedtaksperioderPerAktørOgType.mapValues { (_, andelForVedtaksperiode) -> andelForVedtaksperiode.beskjærFraOgMed(endringstidspunkt.tilMånedTidspunkt()) }

    val utenlandskePeriodebeløpTidslinjerForBarna = utenlandskePeriodebeløp.tilSeparateTidslinjerForBarna().mapKeys { entry -> Pair(entry.key, YtelseType.ORDINÆR_BARNETRYGD) }
    val valutakursTidslinjerForBarna = valutakurser.tilSeparateTidslinjerForBarna().mapKeys { entry -> Pair(entry.key, YtelseType.ORDINÆR_BARNETRYGD) }

    return andelerForVedtaksperioderPerAktørOgTypeAvgrensetTilVedtaksperioder
        // Kombinerer tidslinjene for andeler, utenlandskPeriodebeløp og valutakurser per aktørOgYtelse
        .outerJoin(utenlandskePeriodebeløpTidslinjerForBarna, valutakursTidslinjerForBarna) { andelForVedtaksperiode, utenlandsPeriodebeløp, valutakurs -> andelForVedtaksperiode?.let { AndelUpbOgValutakurs(andelTilkjentYtelse = andelForVedtaksperiode, utenlandskPeriodebeløp = utenlandsPeriodebeløp, valutakurs = valutakurs) } }
        .map { (aktørOgYtelseType, andelUpbOgValutakursTidslinje) ->
            andelUpbOgValutakursTidslinje.mapIkkeNull { andelerUpbOgValutakurs ->
                hentUtbetalingEøs(aktørOgYtelseType = aktørOgYtelseType, andelUpbOgValutakurs = andelerUpbOgValutakurs)
            }
        }
        // Kombinerer verdiene til alle tidslinjene slik at vi får en liste av UtbetalingEøs per periode, samt sørger for at vi får en periode per mnd.
        // Grupperer deretter på periodenes fom
        .kombiner()
        .perioder()
        .flatMap { periode -> periode.splitPerTidsenhet(LocalDate.now().tilMånedTidspunkt()) }
        .associate { periode ->
            val utbetalingMndEøs = hentUtbetalingMndEøs(utbetalingerEøs = periode.innhold?.toList() ?: emptyList())
            val fraOgMedDato = periode.fraOgMed.tilYearMonth().tilMånedÅrMedium()

            fraOgMedDato to utbetalingMndEøs
        }.filter { it.value.utbetalinger.isNotEmpty() }
}

private fun hentUtbetalingEøs(
    aktørOgYtelseType: Pair<Aktør, YtelseType>,
    andelUpbOgValutakurs: AndelUpbOgValutakurs,
): UtbetalingEøs =
    UtbetalingEøs(
        fnr = aktørOgYtelseType.first.aktivFødselsnummer(),
        ytelseType = aktørOgYtelseType.second,
        satsINorge = andelUpbOgValutakurs.andelTilkjentYtelse.sats,
        utbetaltFraAnnetLand = andelUpbOgValutakurs.utenlandskPeriodebeløp?.tilUtbetaltFraAnnetLand(andelUpbOgValutakurs.valutakurs),
        utbetaltFraNorge = andelUpbOgValutakurs.andelTilkjentYtelse.kalkulertUtbetalingsbeløp,
    )

private fun hentUtbetalingMndEøs(utbetalingerEøs: List<UtbetalingEøs>): UtbetalingMndEøs {
    val summertUtbetaltFraAnnetLand = utbetalingerEøs.sumOf { utbetalingEøs -> utbetalingEøs.utbetaltFraAnnetLand?.beløpINok ?: 0 }
    return UtbetalingMndEøs(
        utbetalinger = utbetalingerEøs,
        oppsummering =
            UtbetalingMndEøsOppsummering(
                summertSatsINorge = utbetalingerEøs.sumOf { utbetalingEøs -> utbetalingEøs.satsINorge },
                summertUtbetaltFraAnnetLand = summertUtbetaltFraAnnetLand.takeIf { it != 0 },
                summertUtbetaltFraNorge = utbetalingerEøs.sumOf { it.utbetaltFraNorge },
            ),
    )
}
