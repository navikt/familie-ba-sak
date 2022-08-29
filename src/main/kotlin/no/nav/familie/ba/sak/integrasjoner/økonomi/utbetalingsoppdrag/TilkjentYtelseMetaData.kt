package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import java.time.YearMonth

data class TilkjentYtelseMetaData(
    val tilkjentYtelse: TilkjentYtelse,
    val vedtak: Vedtak,
    val saksbehandlerId: String,
    val erSimulering: Boolean,
    val endretMigreringsdato: YearMonth? = null,
    val kompetanser: List<Kompetanse>
)
