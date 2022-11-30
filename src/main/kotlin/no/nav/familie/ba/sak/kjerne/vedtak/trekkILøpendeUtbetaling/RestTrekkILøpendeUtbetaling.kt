package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

data class RestTrekkILøpendeUtbetaling(
    val identifikator: TrekkILøpendeBehandlingRestIdentifikator,
    val periode: RestPeriode,
    val feilutbetaltBeløp: Int
)
