package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import java.time.LocalDate

fun dødeBarnForrigePeriode(
    ytelserForrigePeriode: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    barnIBehandling: List<MinimertPerson>,
): List<String> {
    return barnIBehandling.filter { barn ->
        val ytelserForrigePeriodeForBarn =
            ytelserForrigePeriode.filter {
                it.aktør.aktivFødselsnummer() == barn.aktivPersonIdent
            }
        var barnDødeForrigePeriode = false
        if (barn.erDød() && ytelserForrigePeriodeForBarn.isNotEmpty()) {
            val fom =
                ytelserForrigePeriodeForBarn.minOf { it.stønadFom }
            val tom =
                ytelserForrigePeriodeForBarn.maxOf { it.stønadTom }
            val fomFørDødsfall = fom <= barn.dødsfallsdato!!.toYearMonth()
            val tomEtterDødsfall = tom >= barn.dødsfallsdato.toYearMonth()
            barnDødeForrigePeriode = fomFørDødsfall && tomEtterDødsfall
        }
        barnDødeForrigePeriode
    }.map { it.aktivPersonIdent }
}

fun List<LocalDate>.tilBrevTekst(): String = Utils.slåSammen(this.sorted().map { it.tilKortString() })

fun Standardbegrunnelse.tilVedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
): Vedtaksbegrunnelse {
    if (!vedtaksperiodeMedBegrunnelser
            .type
            .tillatteBegrunnelsestyper
            .contains(this.vedtakBegrunnelseType)
    ) {
        throw Feil(
            "Begrunnelsestype ${this.vedtakBegrunnelseType} passer ikke med " +
                "typen '${vedtaksperiodeMedBegrunnelser.type}' som er satt på perioden.",
        )
    }

    return Vedtaksbegrunnelse(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        standardbegrunnelse = this,
    )
}