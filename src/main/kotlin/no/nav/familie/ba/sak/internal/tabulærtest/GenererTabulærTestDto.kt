package no.nav.familie.ba.sak.internal.tabulærtest

data class GenererTabulærTestDto(
    val behandlingId: Long,
    val inkluderForrigeBehandling: Boolean = true,
    val inkluderDagensDato: Boolean = true,
    val inkluderPersonerFremstiltKravFor: Boolean = true,
    val inkluderVilkårsvurdering: Boolean = true,
    val inkluderAndelerTilkjentYtelse: Boolean = true,
    val inkluderEndredeUtbetalinger: Boolean = true,
    val inkluderKompetanser: Boolean = false,
    val inkluderUtenlandskePeriodebeløp: Boolean = false,
    val inkluderValutakurser: Boolean = false,
)
