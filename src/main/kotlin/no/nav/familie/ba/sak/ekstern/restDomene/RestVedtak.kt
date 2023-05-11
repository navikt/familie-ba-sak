package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.RestUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.LocalDateTime

data class RestVedtak(
    val aktiv: Boolean,
    val vedtaksdato: LocalDateTime?,
    val vedtaksperioderMedBegrunnelser: List<RestUtvidetVedtaksperiodeMedBegrunnelser>,
    val id: Long,
)

data class RestVedtakBegrunnelseTilknyttetVilkår(
    val id: String,
    val navn: String,
    val vilkår: Vilkår?,
)

fun Vedtak.tilRestVedtak(
    vedtaksperioderMedBegrunnelser: List<RestUtvidetVedtaksperiodeMedBegrunnelser>,
    skalMinimeres: Boolean,
) =
    RestVedtak(
        aktiv = this.aktiv,
        vedtaksdato = this.vedtaksdato,
        id = this.id,
        vedtaksperioderMedBegrunnelser = if (skalMinimeres) {
            vedtaksperioderMedBegrunnelser
                .filter { it.begrunnelser.isNotEmpty() }
                .map { it.copy(gyldigeBegrunnelser = emptyList()) }
        } else {
            vedtaksperioderMedBegrunnelser
        },
    )
