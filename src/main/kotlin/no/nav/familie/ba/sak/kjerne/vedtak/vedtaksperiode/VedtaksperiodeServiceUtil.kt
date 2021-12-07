package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.hentPersonerForEtterEndretUtbetalingsperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakUtils
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelsePerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.barnMedSeksårsdagPåFom
import no.nav.familie.ba.sak.kjerne.vedtak.domene.harBarnMedSeksårsdagPåFom
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentSøker
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.BegrunnelseGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.somOverlapper
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.harPersonerSomManglerOpplysninger
import java.time.LocalDate

fun hentVedtaksperioderMedBegrunnelserForEndredeUtbetalingsperioder(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak
) = andelerTilkjentYtelse.filter { it.endretUtbetalingAndeler.isNotEmpty() }.groupBy { it.prosent }
    .flatMap { (_, andeler) ->
        andeler.lagVertikaleSegmenter()
            .map { (segmenter, andelerForSegment) ->
                VedtaksperiodeMedBegrunnelser(
                    fom = segmenter.fom,
                    tom = segmenter.tom,
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.ENDRET_UTBETALING
                ).also { vedtaksperiodeMedBegrunnelse ->
                    val endretUtbetalingAndeler = andelerForSegment.flatMap { it.endretUtbetalingAndeler }
                    vedtaksperiodeMedBegrunnelse.begrunnelser.addAll(
                        endretUtbetalingAndeler
                            .flatMap { it.vedtakBegrunnelseSpesifikasjoner }.toSet()
                            .map { vedtakBegrunnelseSpesifikasjon ->
                                Vedtaksbegrunnelse(
                                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelse,
                                    vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
                                    personIdenter = endretUtbetalingAndeler.filter {
                                        it.harVedtakBegrunnelseSpesifikasjon(vedtakBegrunnelseSpesifikasjon)
                                    }.mapNotNull { it.person?.personIdent?.ident }
                                )
                            }
                    )
                }
            }
    }

fun hentVedtaksperioderMedBegrunnelserForUtbetalingsperioder(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak
) = andelerTilkjentYtelse.filter { it.endretUtbetalingAndeler.isEmpty() }.lagVertikaleSegmenter()
    .map { (segmenter, _) ->
        VedtaksperiodeMedBegrunnelser(
            fom = segmenter.fom,
            tom = segmenter.tom,
            vedtak = vedtak,
            type = Vedtaksperiodetype.UTBETALING
        )
    }

fun hentPersonidenterGjeldendeForBegrunnelse(
    triggesAv: TriggesAv,
    periode: NullablePeriode,
    vedtaksperiodeType: Vedtaksperiodetype,
    vedtakBegrunnelseType: VedtakBegrunnelseType,
    begrunnelseGrunnlag: BegrunnelseGrunnlag,
    identerMedUtbetaling: List<String>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
): List<String> {

    val erFortsattInnvilgetBegrunnelse =
        vedtaksperiodeType == Vedtaksperiodetype.FORTSATT_INNVILGET ||
            vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET

    return when {
        triggesAv.vilkår.contains(Vilkår.UTVIDET_BARNETRYGD) || triggesAv.småbarnstillegg ->
            identerMedUtbetaling +
                begrunnelseGrunnlag.begrunnelsePersoner.hentSøker().personIdent +
                begrunnelseGrunnlag
                    .minimerteEndredeUtbetalingAndeler
                    .somOverlapper(periode.tilNullableMånedPeriode())
                    .map { it.personIdent }

        triggesAv.barnMedSeksårsdag ->
            begrunnelseGrunnlag.begrunnelsePersoner.barnMedSeksårsdagPåFom(periode.fom)
                .map { person -> person.personIdent }

        triggesAv.personerManglerOpplysninger ->
            if (begrunnelseGrunnlag.personResultater.harPersonerSomManglerOpplysninger())
                emptyList()
            else
                error("Legg til opplysningsplikt ikke oppfylt begrunnelse men det er ikke person med det resultat")

        erFortsattInnvilgetBegrunnelse -> identerMedUtbetaling

        triggesAv.etterEndretUtbetaling ->
            hentPersonerForEtterEndretUtbetalingsperiode(
                minimerteEndredeUtbetalingAndeler = begrunnelseGrunnlag.minimerteEndredeUtbetalingAndeler,
                fom = periode.fom,
                endringsaarsaker = triggesAv.endringsaarsaker
            )

        else ->
            VedtakUtils.hentPersonerForAlleUtgjørendeVilkår(
                personResultater = begrunnelseGrunnlag.personResultater,
                vedtaksperiode = Periode(
                    fom = periode.fom ?: TIDENES_MORGEN,
                    tom = periode.tom ?: TIDENES_ENDE
                ),
                oppdatertBegrunnelseType = vedtakBegrunnelseType,
                aktuellePersonerForVedtaksperiode = hentAktuellePersonerForVedtaksperiode(
                    begrunnelseGrunnlag.begrunnelsePersoner,
                    vedtakBegrunnelseType,
                    identerMedUtbetaling
                ),
                triggesAv = triggesAv,
                erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
            ).map { person -> person.personIdent }
    }.toSet().toList()
}

private fun hentAktuellePersonerForVedtaksperiode(
    begrunnelsePersoner: List<BegrunnelsePerson>,
    vedtakBegrunnelseType: VedtakBegrunnelseType,
    identerMedUtbetaling: List<String>
): List<BegrunnelsePerson> = begrunnelsePersoner.filter { person ->
    if (vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET) {
        identerMedUtbetaling.contains(person.personIdent) || person.type == PersonType.SØKER
    } else true
}

fun validerSatsendring(fom: LocalDate?, begrunnelsePerson: List<BegrunnelsePerson>) {
    val satsendring = SatsService
        .finnSatsendring(fom ?: TIDENES_MORGEN)

    val harBarnMedSeksårsdagPåFom = begrunnelsePerson.harBarnMedSeksårsdagPåFom(fom)

    if (satsendring.isEmpty() && !harBarnMedSeksårsdagPåFom) {
        throw FunksjonellFeil(
            melding = "Begrunnelsen stemmer ikke med satsendring.",
            frontendFeilmelding = "Begrunnelsen stemmer ikke med satsendring. Vennligst velg en annen begrunnelse."
        )
    }
}

fun kastFeilmeldingForBegrunnelserMedFeil(
    begrunnelserMedFeil: List<VedtakBegrunnelseSpesifikasjon>,
    sanityBegrunnelser: List<SanityBegrunnelse>
) {
    throw FunksjonellFeil(
        melding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake til " +
            "vilkårsvurderingen eller velg en annen begrunnelse.",
        frontendFeilmelding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake " +
            "til vilkårsvurderingen eller velg en annen begrunnelse.\n" +
            begrunnelserMedFeil.fold("") { acc, vedtakBegrunnelseSpesifikasjon ->
                val sanityBegrunnelse =
                    vedtakBegrunnelseSpesifikasjon
                        .tilSanityBegrunnelse(sanityBegrunnelser)
                        ?: error(
                            "Finner ikke begrunnelse med apiNavn ${vedtakBegrunnelseSpesifikasjon.sanityApiNavn} " +
                                "i Sanity"
                        )

                val vilkårsbeskrivelse =
                    sanityBegrunnelse.tilTriggesAv().vilkår.first().beskrivelse
                val tittel =
                    sanityBegrunnelse.navnISystem

                "$acc'$tittel' forventer vurdering på '$vilkårsbeskrivelse'"
            }
    )
}

fun validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser) {
    if ((
            vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.OPPHØR ||
                vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.AVSLAG
            ) &&
        vedtaksperiodeMedBegrunnelser.harFriteksterUtenStandardbegrunnelser()
    ) {
        val fritekstUtenStandardbegrunnelserFeilmelding =
            "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. " +
                "Legg først til en ny begrunnelse eller fjern friteksten(e)."
        throw FunksjonellFeil(
            melding = fritekstUtenStandardbegrunnelserFeilmelding,
            frontendFeilmelding = fritekstUtenStandardbegrunnelserFeilmelding
        )
    }

    if (vedtaksperiodeMedBegrunnelser.vedtak.behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET &&
        vedtaksperiodeMedBegrunnelser.harFriteksterOgStandardbegrunnelser()
    ) {
        throw FunksjonellFeil(
            "Det ble sendt med både fritekst og begrunnelse. " +
                "Vedtaket skal enten ha fritekst eller bregrunnelse, men ikke begge deler."
        )
    }
}

fun erFørsteVedtaksperiodePåFagsak(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    periodeFom: LocalDate?
): Boolean = !andelerTilkjentYtelse.any {
    it.stønadFom.isBefore(
        periodeFom?.toYearMonth() ?: TIDENES_MORGEN.toYearMonth()
    )
}
