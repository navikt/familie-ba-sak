package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.integrasjoner.økonomi.KjedeId
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import java.time.YearMonth

data class VedtakMedTilkjentYtelse(
    val tilkjentYtelse: TilkjentYtelse,
    val vedtak: Vedtak,
    val saksbehandlerId: String,
    val sisteOffsetPerIdent: Map<KjedeId, Int> = emptyMap(),
    val sisteOffsetPåFagsak: Int? = null,
    val erSimulering: Boolean,
    val endretMigreringsdato: YearMonth? = null
)
