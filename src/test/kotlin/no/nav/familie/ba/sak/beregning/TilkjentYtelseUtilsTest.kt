package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.ClientMocks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilkjentYtelseUtilsTest {

    @Test
    fun `Skal beregne riktig etterbetaling for periode ett år tilbake i tid`() {
        val behandling = lagBehandling()
        val baseDato = LocalDate.now()
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, ClientMocks.søkerFnr[0], ClientMocks.barnFnr.toList())
        val vedtak = lagVedtak(behandling)
        val barn1 = personopplysningGrunnlag.barna.first()
        val barn2 = personopplysningGrunnlag.barna.last()

        val andelTilkjentYtelseBarn1 = lagAndelTilkjentYtelse(baseDato.withDayOfMonth(1).toString(),
                                                              baseDato.plusYears(1).sisteDagIMåned().toString(),
                                                              behandling = behandling,
                                                              person = barn1)
        val andelTilkjentYtelseBarn2 = lagAndelTilkjentYtelse(baseDato.minusYears(1).withDayOfMonth(1).toString(),
                                                              baseDato.plusYears(1).toString(),
                                                              behandling = behandling,
                                                              person = barn2)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling).copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelseBarn1, andelTilkjentYtelseBarn2))
        val beregningsoversikt = TilkjentYtelseUtils.hentBeregningOversikt(tilkjentYtelse, personopplysningGrunnlag)

        val etterbetalingsbeløp = TilkjentYtelseUtils.beregnEtterbetaling(beregningsoversikt, vedtak)

        assertEquals(12648, etterbetalingsbeløp)
    }

    @Test
    fun `Skal beregne riktig etterbetaling for flere perioder tilbake i tid`() {
        val behandling = lagBehandling()
        val baseDato = LocalDate.now()
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, ClientMocks.søkerFnr[0], ClientMocks.barnFnr.toList())
        val vedtak = lagVedtak(behandling)
        val barn1 = personopplysningGrunnlag.barna.first()
        val barn2 = personopplysningGrunnlag.barna.last()

        val andelTilkjentYtelseBarn1 = lagAndelTilkjentYtelse(baseDato.withDayOfMonth(1).minusMonths(6).toString(),
                                                              baseDato.plusYears(1).sisteDagIMåned().toString(),
                                                              behandling = behandling,
                                                              person = barn1)
        val andelTilkjentYtelseBarn2 = lagAndelTilkjentYtelse(baseDato.minusYears(1).withDayOfMonth(1).toString(),
                                                              baseDato.plusYears(1).toString(),
                                                              behandling = behandling,
                                                              person = barn2)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling).copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelseBarn1, andelTilkjentYtelseBarn2))
        val beregningsoversikt = TilkjentYtelseUtils.hentBeregningOversikt(tilkjentYtelse, personopplysningGrunnlag)

        val etterbetalingsbeløp = TilkjentYtelseUtils.beregnEtterbetaling(beregningsoversikt, vedtak)

        assertEquals(18972, etterbetalingsbeløp)
    }

    @Test
    fun `Skal hente riktig nåværende beløp for to barn og etterbetaling`() {
        val behandling = lagBehandling()
        val baseDato = LocalDate.now()
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, ClientMocks.søkerFnr[0], ClientMocks.barnFnr.toList())
        val vedtak = lagVedtak(behandling)
        val barn1 = personopplysningGrunnlag.barna.first()
        val barn2 = personopplysningGrunnlag.barna.last()

        val andelTilkjentYtelseBarn1 = lagAndelTilkjentYtelse(baseDato.minusYears(1).withDayOfMonth(1).toString(),
                                                              baseDato.plusYears(1).sisteDagIMåned().toString(),
                                                              behandling = behandling,
                                                              person = barn1)
        val andelTilkjentYtelseBarn2 = lagAndelTilkjentYtelse(baseDato.minusMonths(1).withDayOfMonth(1).toString(),
                                                              baseDato.plusYears(1).toString(),
                                                              behandling = behandling,
                                                              person = barn2)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling).copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelseBarn1, andelTilkjentYtelseBarn2))
        val beregningsoversikt = TilkjentYtelseUtils.hentBeregningOversikt(tilkjentYtelse, personopplysningGrunnlag)

        val nåværendeBeløp = TilkjentYtelseUtils.beregnNåværendeBeløp(beregningsoversikt, vedtak)

        assertEquals(2108, nåværendeBeløp)
    }

    @Test
    fun `Skal hente riktig nåværende beløp for ett barn uten etterbetaling`() {
        val behandling = lagBehandling()
        val baseDato = LocalDate.now()
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id,
                                                                       ClientMocks.søkerFnr[0],
                                                                       ClientMocks.barnFnr.toList().subList(0, 1))
        val vedtak = lagVedtak(behandling)
        val barn1 = personopplysningGrunnlag.barna.first()

        val andelTilkjentYtelseBarn1 = lagAndelTilkjentYtelse(baseDato.plusMonths(1).withDayOfMonth(1).toString(),
                                                              baseDato.plusYears(5).sisteDagIMåned().toString(),
                                                              behandling = behandling,
                                                              person = barn1)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling).copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelseBarn1))
        val beregningsoversikt = TilkjentYtelseUtils.hentBeregningOversikt(tilkjentYtelse, personopplysningGrunnlag)

        val nåværendeBeløp = TilkjentYtelseUtils.beregnNåværendeBeløp(beregningsoversikt, vedtak)

        assertEquals(1054, nåværendeBeløp)
    }
}