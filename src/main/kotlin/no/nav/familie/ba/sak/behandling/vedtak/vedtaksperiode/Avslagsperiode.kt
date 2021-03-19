package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.grupperPåPeriode
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import java.time.LocalDate

data class Avslagsperiode(
        override val periodeFom: LocalDate?,
        override val periodeTom: LocalDate?,
        override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.AVSLAG,
) : Vedtaksperiode

fun mapTilAvslagsperioder(vedtakBegrunnelser: List<VedtakBegrunnelse>): List<Avslagsperiode> =
        vedtakBegrunnelser
                .filter { it.begrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG }
                .grupperPåPeriode()
                .map {
                    Avslagsperiode(periodeFom = it.key.fom,
                                   periodeTom = it.key.tom)
                }
