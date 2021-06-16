package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.filterAvslag
import no.nav.familie.ba.sak.kjerne.vedtak.grupperPåPeriode
import java.time.LocalDate

data class Avslagsperiode(
        override val periodeFom: LocalDate?,
        override val periodeTom: LocalDate?,
        override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.AVSLAG,
) : Vedtaksperiode

@Deprecated("Erstattes av mapTilAvslagsperioder")
fun mapTilAvslagsperioderDeprecated(vedtakBegrunnelser: List<VedtakBegrunnelse>): List<Avslagsperiode> =
        vedtakBegrunnelser
                .filterAvslag()
                .grupperPåPeriode()
                .map {
                    Avslagsperiode(periodeFom = it.key.fom,
                                   periodeTom = it.key.tom)
                }
