package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag

data class UtbetalingsoppdragDTO(val utbetalingsoppdrag: Utbetalingsoppdrag, val harAndelerTilOpprettelse: Boolean, val tilkjentYtelse: TilkjentYtelse?)
