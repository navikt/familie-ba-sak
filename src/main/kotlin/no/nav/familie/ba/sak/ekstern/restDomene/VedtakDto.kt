package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.LocalDateTime

data class VedtakDto(
    val aktiv: Boolean,
    val vedtaksdato: LocalDateTime?,
    val id: Long,
)

data class VedtakBegrunnelseTilknyttetVilkårDto(
    val id: String,
    val navn: String,
    val vilkår: Vilkår?,
)

fun Vedtak.tilVedtakDto() =
    VedtakDto(
        aktiv = this.aktiv,
        vedtaksdato = this.vedtaksdato,
        id = this.id,
    )
