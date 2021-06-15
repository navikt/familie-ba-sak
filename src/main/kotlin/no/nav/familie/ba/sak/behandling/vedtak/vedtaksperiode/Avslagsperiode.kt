package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.filterAvslag
import no.nav.familie.ba.sak.behandling.vedtak.grupperPåPeriode
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
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
