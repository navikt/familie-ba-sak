package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

data class RestTrekkILøpendeUtbetaling(
    val id: Long,
    val behandlingId: Long,
    val periode: RestPeriode,
    val feilutbetaltBeløp: Int
)
