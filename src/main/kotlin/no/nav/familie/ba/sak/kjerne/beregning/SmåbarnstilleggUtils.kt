package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.erUlike
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

fun vedtakOmOvergangsstønadPåvirkerFagsak(
    småbarnstilleggBarnetrygdGenerator: SmåbarnstilleggBarnetrygdGenerator,
    nyePerioderMedFullOvergangsstønad: List<InternPeriodeOvergangsstønad>,
    forrigeSøkersAndeler: List<AndelTilkjentYtelse>,
    barnasFødselsdatoer: List<LocalDate>
): Boolean {
    val (forrigeSøkersSmåbarnstilleggAndeler, forrigeSøkersAndreAndeler) = forrigeSøkersAndeler.partition { it.erSmåbarnstillegg() }

    val nyeSmåbarnstilleggAndeler = småbarnstilleggBarnetrygdGenerator.lagSmåbarnstilleggAndeler(
        perioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
        andelerSøker = forrigeSøkersAndreAndeler,
        barnasFødselsdatoer = barnasFødselsdatoer
    )

    return forrigeSøkersSmåbarnstilleggAndeler.erUlike(nyeSmåbarnstilleggAndeler)
}

fun finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
    utvidedeVedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
    vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
): VedtaksperiodeMedBegrunnelser {
    val utvidetVedtaksperiodeSomSkalOppdateres = utvidedeVedtaksperioderMedBegrunnelser.find {
        (
            it.fom?.toYearMonth() == YearMonth.now() || it.fom?.toYearMonth() == YearMonth.now()
                .nesteMåned()
            ) && it.type == Vedtaksperiodetype.UTBETALING
    }

    if (utvidetVedtaksperiodeSomSkalOppdateres == null) {
        LoggerFactory.getLogger("secureLogger")
            .info(
                "Finner ikke aktuell periode å begrunne ved autovedtak småbarnstillegg. " +
                    "Perioder: ${utvidedeVedtaksperioderMedBegrunnelser.map { "Periode(type=${it.type}, fom=${it.fom}, tom=${it.tom})" }}"
            )

        throw Feil("Finner ikke aktuell periode å begrunne ved autovedtak småbarnstillegg. Se securelogger for å periodene som ble generert.")
    }

    val søkersYtelsetyper =
        utvidetVedtaksperiodeSomSkalOppdateres.utbetalingsperiodeDetaljer.filter { it.person.type == PersonType.SØKER }
            .map { it.ytelseType }

    val vedtaksperiodeSomSkalOppdateres = vedtaksperioderMedBegrunnelser
        .single { it.id == utvidetVedtaksperiodeSomSkalOppdateres.id }

    vedtaksperiodeSomSkalOppdateres.settBegrunnelser(
        vedtaksperiodeSomSkalOppdateres.begrunnelser.toList() + listOf(
            Vedtaksbegrunnelse(
                vedtaksperiodeMedBegrunnelser = vedtaksperiodeSomSkalOppdateres,
                personIdenter = emptyList(),
                vedtakBegrunnelseSpesifikasjon = hentVedtakBegrunnelseSpesifikasjonForSmåbarnstillegg(
                    søkersYtelsetyper
                )
            )
        )
    )
    return vedtaksperiodeSomSkalOppdateres
}

fun hentVedtakBegrunnelseSpesifikasjonForSmåbarnstillegg(ytelseTyper: List<YtelseType>): VedtakBegrunnelseSpesifikasjon {
    return if (ytelseTyper.contains(YtelseType.SMÅBARNSTILLEGG)) VedtakBegrunnelseSpesifikasjon.INNVILGET_SMÅBARNSTILLEGG
    else if (ytelseTyper.contains(YtelseType.UTVIDET_BARNETRYGD)) VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD
    else error("Begrunnelse for småbarnstillegg er ikke støttet for ytelseTyper=$ytelseTyper")
}
