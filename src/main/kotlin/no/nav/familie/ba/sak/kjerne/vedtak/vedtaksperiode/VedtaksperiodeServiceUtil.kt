package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.hentPersonerForEtterEndretUtbetalingsperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakUtils
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
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

fun hentPersoneidenterGjeldendeForBegrunnelse(
    triggesAv: TriggesAv,
    persongrunnlag: PersonopplysningGrunnlag,
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    vilkårsvurdering: Vilkårsvurdering,
    vedtakBegrunnelseType: VedtakBegrunnelseType,
    identerMedUtbetaling: List<String>,
    endredeUtbetalingAndeler: List<EndretUtbetalingAndel>
): List<String> {
    val erFortsattInnvilgetBegrunnelse =
        vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.FORTSATT_INNVILGET ||
            vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET

    return when {
        triggesAv.vilkår.contains(Vilkår.UTVIDET_BARNETRYGD) -> identerMedUtbetaling

        triggesAv.barnMedSeksårsdag ->
            persongrunnlag.barnMedSeksårsdagPåFom(vedtaksperiodeMedBegrunnelser.fom)
                .map { person -> person.personIdent.ident }

        triggesAv.personerManglerOpplysninger ->
            if (vilkårsvurdering.harPersonerManglerOpplysninger())
                emptyList()
            else
                error("Legg til opplysningsplikt ikke oppfylt begrunnelse men det er ikke person med det resultat")

        erFortsattInnvilgetBegrunnelse -> identerMedUtbetaling

        triggesAv.etterEndretUtbetaling ->
            hentPersonerForEtterEndretUtbetalingsperiode(
                endretUtbetalingAndeler = endredeUtbetalingAndeler,
                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                endringsaarsaker = triggesAv.endringsaarsaker
            ).map { person -> person.personIdent.ident }

        else ->
            VedtakUtils.hentPersonerForAlleUtgjørendeVilkår(
                vilkårsvurdering = vilkårsvurdering,
                vedtaksperiode = Periode(
                    fom = vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
                    tom = vedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
                ),
                oppdatertBegrunnelseType = vedtakBegrunnelseType,
                aktuellePersonerForVedtaksperiode = hentAktuellePersonerForVedtaksperiode(
                    persongrunnlag,
                    vedtakBegrunnelseType,
                    identerMedUtbetaling
                ),
                triggesAv = triggesAv
            ).map { person -> person.personIdent.ident }
    }
}

private fun hentAktuellePersonerForVedtaksperiode(
    persongrunnlag: PersonopplysningGrunnlag,
    vedtakBegrunnelseType: VedtakBegrunnelseType,
    identerMedUtbetaling: List<String>
): List<Person> = persongrunnlag.personer.filter { person ->
    if (vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET) {
        identerMedUtbetaling.contains(person.personIdent.ident) || person.type == PersonType.SØKER
    } else true
}

fun validerSatsendring(fom: LocalDate?) {
    val satsendring = SatsService
        .finnSatsendring(fom ?: TIDENES_MORGEN)

    if (satsendring.isEmpty()) {
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

                acc + "'$tittel' forventer vurdering på '$vilkårsbeskrivelse'"
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
