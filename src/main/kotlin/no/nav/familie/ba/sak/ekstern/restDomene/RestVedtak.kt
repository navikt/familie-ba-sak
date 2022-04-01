package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.LocalDate
import java.time.LocalDateTime

data class RestVedtak(
    val aktiv: Boolean,
    val vedtaksdato: LocalDateTime?,
    val førsteEndringstidspunkt: LocalDate?,
    val vedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
    val id: Long
)

data class RestVedtakBegrunnelseTilknyttetVilkår(
    val id: Standardbegrunnelse,
    val navn: String,
    val vilkår: Vilkår?
)

fun Vedtak.tilRestVedtak(
    førsteEndringstidspunkt: LocalDate,
    vedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
    skalMinimeres: Boolean,
) =
    RestVedtak(
        aktiv = this.aktiv,
        vedtaksdato = this.vedtaksdato,
        id = this.id,
        førsteEndringstidspunkt = if (førsteEndringstidspunkt == TIDENES_ENDE ||
            førsteEndringstidspunkt == TIDENES_MORGEN
        ) null else førsteEndringstidspunkt,
        vedtaksperioderMedBegrunnelser = if (skalMinimeres) {
            vedtaksperioderMedBegrunnelser
                .filter { it.begrunnelser.isNotEmpty() }
                .map { it.copy(gyldigeBegrunnelser = emptyList()) }
        } else {
            vedtaksperioderMedBegrunnelser
        },
    )
