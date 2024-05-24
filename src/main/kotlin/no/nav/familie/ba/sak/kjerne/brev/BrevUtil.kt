package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
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
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.UtbetalingstabellAutomatiskValutajustering
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.AndelUpbOgValutakurs
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingEøs
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøsOppsummering
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilUtfylteKompetanserEtterEndringstidpunktPerAktør
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.tilUtbetaltFraAnnetLand
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
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
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hjemlerTilhørendeFritekst
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

        BehandlingÅrsak.OMREGNING_6ÅR,
        BehandlingÅrsak.OMREGNING_18ÅR,
        BehandlingÅrsak.SMÅBARNSTILLEGG,
        BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG,
        -> Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG

        else -> throw Feil("Det er ikke laget funksjonalitet for automatisk behandling for $behandlingÅrsak")
    }
}

// Dokumenttittel legges på i familie-integrasjoner basert på dokumenttype
// Denne funksjonen bestemmer om dokumenttittelen skal overstyres eller ikke
fun hentOverstyrtDokumenttittel(behandling: Behandling): String? {
    return if (behandling.type == BehandlingType.REVURDERING) {
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
}

fun hjemlerTilHjemmeltekst(
    hjemler: List<String>,
    lovForHjemmel: String,
): String {
    return when (hjemler.size) {
        0 -> throw Feil("Kan ikke lage hjemmeltekst for $lovForHjemmel når ingen begrunnelser har hjemler fra $lovForHjemmel knyttet til seg.")
        1 -> "§ ${hjemler[0]}"
        else -> "§§ ${Utils.slåSammen(hjemler)}"
    }
}

fun hentHjemmeltekst(
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
    standardbegrunnelseTilSanityBegrunnelse: Map<Standardbegrunnelse, SanityBegrunnelse>,
    eøsStandardbegrunnelseTilSanityBegrunnelse: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean = false,
    målform: Målform,
    vedtakKorrigertHjemmelSkalMedIBrev: Boolean = false,
    refusjonEøsHjemmelSkalMedIBrev: Boolean = false,
    erFritekstIBrev: Boolean,
): String {
    val sanityStandardbegrunnelser =
        vedtaksperioder.flatMap { vedtaksperiode -> vedtaksperiode.begrunnelser.mapNotNull { begrunnelse -> standardbegrunnelseTilSanityBegrunnelse[begrunnelse.standardbegrunnelse] } }
    val sanityEøsBegrunnelser =
        vedtaksperioder.flatMap { vedtaksperiode -> vedtaksperiode.eøsBegrunnelser.mapNotNull { eøsBegrunnelse -> eøsStandardbegrunnelseTilSanityBegrunnelse[eøsBegrunnelse.begrunnelse] } }

    val ordinæreHjemler =
        hentOrdinæreHjemler(
            hjemler =
                (sanityStandardbegrunnelser.flatMap { it.hjemler } + sanityEøsBegrunnelser.flatMap { it.hjemler })
                    .toMutableSet(),
            opplysningspliktHjemlerSkalMedIBrev = opplysningspliktHjemlerSkalMedIBrev,
            finnesVedtaksperiodeMedFritekst = erFritekstIBrev,
        )

    val forvaltningsloverHjemler = hentForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev)

    val alleHjemlerForBegrunnelser =
        hentAlleTyperHjemler(
            hjemlerSeparasjonsavtaleStorbritannia =
                sanityEøsBegrunnelser.flatMap { it.hjemlerSeperasjonsavtalenStorbritannina }
                    .distinct(),
            ordinæreHjemler = ordinæreHjemler.distinct(),
            hjemlerFraFolketrygdloven =
                (sanityStandardbegrunnelser.flatMap { it.hjemlerFolketrygdloven } + sanityEøsBegrunnelser.flatMap { it.hjemlerFolketrygdloven })
                    .distinct(),
            hjemlerEØSForordningen883 = sanityEøsBegrunnelser.flatMap { it.hjemlerEØSForordningen883 }.distinct(),
            hjemlerEØSForordningen987 = hentHjemlerForEøsForordningen987(sanityEøsBegrunnelser, refusjonEøsHjemmelSkalMedIBrev),
            målform = målform,
            hjemlerFraForvaltningsloven = forvaltningsloverHjemler,
        )

    return slåSammenHjemlerAvUlikeTyper(alleHjemlerForBegrunnelser)
}

private fun hentHjemlerForEøsForordningen987(
    sanityEøsBegrunnelser: List<SanityEØSBegrunnelse>,
    refusjonEøsHjemmelSkalMedIBrev: Boolean,
): List<String> {
    val hjemler = mutableListOf<String>()

    hjemler.addAll(sanityEøsBegrunnelser.flatMap { it.hjemlerEØSForordningen987 })

    if (refusjonEøsHjemmelSkalMedIBrev) {
        hjemler.add("60")
    }

    return hjemler.distinct()
}

private fun slåSammenHjemlerAvUlikeTyper(hjemler: List<String>) =
    when (hjemler.size) {
        0 -> throw FunksjonellFeil("Ingen hjemler var knyttet til begrunnelsen(e) som er valgt. Du må velge minst én begrunnelse som er knyttet til en hjemmel.")
        1 -> hjemler.single()
        else -> hjemler.slåSammen()
    }

fun Collection<String>.slåSammen(): String {
    return this.reduceIndexed { index, acc, s ->
        when (index) {
            0 -> s
            this.size - 1 -> "$acc og $s"
            else -> "$acc, $s"
        }
    }
}

private fun hentAlleTyperHjemler(
    hjemlerSeparasjonsavtaleStorbritannia: List<String>,
    ordinæreHjemler: List<String>,
    hjemlerFraFolketrygdloven: List<String>,
    hjemlerEØSForordningen883: List<String>,
    hjemlerEØSForordningen987: List<String>,
    målform: Målform,
    hjemlerFraForvaltningsloven: List<String>,
): List<String> {
    val alleHjemlerForBegrunnelser = mutableListOf<String>()

    // Rekkefølgen her er viktig
    if (hjemlerSeparasjonsavtaleStorbritannia.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "Separasjonsavtalen mellom Storbritannia og Norge artikkel"
                    Målform.NN -> "Separasjonsavtalen mellom Storbritannia og Noreg artikkel"
                }
            } ${
                Utils.slåSammen(
                    hjemlerSeparasjonsavtaleStorbritannia,
                )
            }",
        )
    }
    if (ordinæreHjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "barnetrygdloven"
                    Målform.NN -> "barnetrygdlova"
                }
            } ${
                hjemlerTilHjemmeltekst(
                    hjemler = ordinæreHjemler,
                    lovForHjemmel = "barnetrygdloven",
                )
            }",
        )
    }
    if (hjemlerFraFolketrygdloven.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "folketrygdloven"
                    Målform.NN -> "folketrygdlova"
                }
            } ${
                hjemlerTilHjemmeltekst(
                    hjemler = hjemlerFraFolketrygdloven,
                    lovForHjemmel = "folketrygdloven",
                )
            }",
        )
    }
    if (hjemlerEØSForordningen883.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 883/2004 artikkel ${Utils.slåSammen(hjemlerEØSForordningen883)}")
    }
    if (hjemlerEØSForordningen987.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 987/2009 artikkel ${Utils.slåSammen(hjemlerEØSForordningen987)}")
    }
    if (hjemlerFraForvaltningsloven.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "forvaltningsloven"
                    Målform.NN -> "forvaltningslova"
                }
            } ${
                hjemlerTilHjemmeltekst(hjemler = hjemlerFraForvaltningsloven, lovForHjemmel = "forvaltningsloven")
            }",
        )
    }
    return alleHjemlerForBegrunnelser
}

private fun hentOrdinæreHjemler(
    hjemler: MutableSet<String>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean,
    finnesVedtaksperiodeMedFritekst: Boolean,
): List<String> {
    if (opplysningspliktHjemlerSkalMedIBrev) {
        val hjemlerNårOpplysningspliktIkkeOppfylt = listOf("17", "18")
        hjemler.addAll(hjemlerNårOpplysningspliktIkkeOppfylt)
    }

    if (finnesVedtaksperiodeMedFritekst) {
        hjemler.addAll(hjemlerTilhørendeFritekst.map { it.toString() }.toSet())
    }

    val sorterteHjemler = hjemler.map { it.toInt() }.sorted().map { it.toString() }
    return sorterteHjemler
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

fun hentForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev: Boolean): List<String> {
    return if (vedtakKorrigertHjemmelSkalMedIBrev) listOf("35") else emptyList()
}

fun skalHenteUtbetalingerEøs(
    endringstidspunkt: LocalDate,
    valutakurser: List<Valutakurs>,
): Boolean {
    // Hvis endringstidspunktet er satt til tidenes ende så ønsker vi ikke å hente eøs utbetalinger
    if (endringstidspunkt == TIDENES_ENDE) return false

    val valutakurserEtterEndringtidspunktet =
        valutakurser.tilSeparateTidslinjerForBarna()
            .mapValues { (_, valutakursTidslinjeForBarn) -> valutakursTidslinjeForBarn.beskjærFraOgMed(endringstidspunkt.tilMånedTidspunkt()) }

    return valutakurserEtterEndringtidspunktet.any { it.value.erIkkeTom() }
}

fun hentLandOgStartdatoForUtbetalingstabell(
    endringstidspunkt: MånedTidspunkt,
    landkoder: Map<String, String>,
    kompetanser: Collection<Kompetanse>,
): UtbetalingstabellAutomatiskValutajustering {
    val utfylteKompetanserEtterEndringstidspunkt =
        kompetanser.tilUtfylteKompetanserEtterEndringstidpunktPerAktør(endringstidspunkt)

    if (utfylteKompetanserEtterEndringstidspunkt.isEmpty()) {
        throw Feil("Finner ingen kompetanser etter endringstidspunkt")
    }

    val eøsLandMedUtbetalinger =
        utfylteKompetanserEtterEndringstidspunkt.values.flatten().map {
            if (it.erAnnenForelderOmfattetAvNorskLovgivning) {
                it.søkersAktivitetsland.tilLandNavn(landkoder).navn
            } else {
                it.annenForeldersAktivitetsland?.tilLandNavn(landkoder)?.navn ?: it.barnetsBostedsland.tilLandNavn(landkoder).navn
            }
        }.toSet()
    return UtbetalingstabellAutomatiskValutajustering(utbetalingerEosLand = eøsLandMedUtbetalinger.slåSammen(), utbetalingerEosMndAar = endringstidspunkt.tilYearMonth().tilMånedÅr())
}

fun hentUtbetalingerPerMndEøs(
    endringstidspunkt: LocalDate,
    andelerForVedtaksperioderPerAktørOgType: Map<Pair<Aktør, YtelseType>, AndelTilkjentYtelseTidslinje>,
    utenlandskePeriodebeløp: List<UtenlandskPeriodebeløp>,
    valutakurser: List<Valutakurs>,
): Map<String, UtbetalingMndEøs> {
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
        .kombiner().perioder()
        .flatMap { periode -> periode.splitPerTidsenhet(LocalDate.now().tilMånedTidspunkt()) }
        .associate { periode ->
            val utbetalingMndEøs = hentUtbetalingMndEøs(utbetalingerEøs = periode.innhold?.toList() ?: emptyList())
            val fraOgMedDato = periode.fraOgMed.tilYearMonth().tilMånedÅrMedium()

            fraOgMedDato to utbetalingMndEøs
        }
        .filter { it.value.utbetalinger.isNotEmpty() }
}

private fun hentUtbetalingEøs(
    aktørOgYtelseType: Pair<Aktør, YtelseType>,
    andelUpbOgValutakurs: AndelUpbOgValutakurs,
): UtbetalingEøs {
    return UtbetalingEøs(
        fnr = aktørOgYtelseType.first.aktivFødselsnummer(),
        ytelseType = aktørOgYtelseType.second,
        satsINorge = andelUpbOgValutakurs.andelTilkjentYtelse.sats,
        utbetaltFraAnnetLand = andelUpbOgValutakurs.utenlandskPeriodebeløp?.tilUtbetaltFraAnnetLand(andelUpbOgValutakurs.valutakurs),
        utbetaltFraNorge = andelUpbOgValutakurs.andelTilkjentYtelse.kalkulertUtbetalingsbeløp,
    )
}

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
