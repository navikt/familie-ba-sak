package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtakBegrunnelseTilknyttetVilkår
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.filterAvslag
import no.nav.familie.ba.sak.kjerne.vedtak.grupperPåPeriode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.hentVilkårsbegrunnelse
import java.time.LocalDate

data class Avslagsperiode(
        override val periodeFom: LocalDate?,
        override val periodeTom: LocalDate?,
        override val relevanteVedtaksbegrunnelser: Set<RestVedtakBegrunnelseTilknyttetVilkår>? = null,
        override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.AVSLAG,
) : Vedtaksperiode {

    override fun leggTilRelevanteBegrunnelser(vilkårResultater: List<VilkårResultat>): Vedtaksperiode {
        val vedtakBegrunnelseTyperAvslag = listOf(VedtakBegrunnelseType.AVSLAG)

        val relevanteVedtaksbegrunnelserOpphør =
                vilkårResultater.filter { it.periodeFom == this.periodeFom && it.resultat == Resultat.IKKE_OPPFYLT }
                        .flatMap { it.vilkårType.hentVilkårsbegrunnelse(vedtakBegrunnelseTyperAvslag) }.toSet()

        return this.copy(relevanteVedtaksbegrunnelser = relevanteVedtaksbegrunnelserOpphør)
    }
}

@Deprecated("Erstattes av mapTilAvslagsperioder")
fun mapTilAvslagsperioderDeprecated(vedtakBegrunnelser: List<VedtakBegrunnelse>): List<Avslagsperiode> =
        vedtakBegrunnelser
                .filterAvslag()
                .grupperPåPeriode()
                .map {
                    Avslagsperiode(periodeFom = it.key.fom,
                                   periodeTom = it.key.tom)
                }
