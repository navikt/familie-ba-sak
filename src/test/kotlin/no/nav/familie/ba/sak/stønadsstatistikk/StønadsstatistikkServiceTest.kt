package no.nav.familie.ba.sak.stønadsstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.ClientMocks.Companion.barnFnr
import no.nav.familie.ba.sak.config.ClientMocks.Companion.søkerFnr
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.økonomi.sats
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StønadsstatistikkServiceTest {

    private val behandlingRepository: BehandlingRepository = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val beregningService: BeregningService = mockk()
    private val loggService: LoggService = mockk()
    private val vedtakService: VedtakService  = mockk()

    private val stønadsstatistikkService =
            StønadsstatistikkService(behandlingRepository, persongrunnlagService, beregningService, loggService, vedtakService)

    @BeforeAll
    fun init() {
        val behandling = lagBehandling()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList())
        val vedtak = lagVedtak(behandling)

        val barn1 = personopplysningGrunnlag.barna.first()
        val barn2 = personopplysningGrunnlag.barna.last()
        val andelTilkjentYtelseBarn1 = lagAndelTilkjentYtelse(barn1.fødselsdato.plusMonths(1).withDayOfMonth(1).toString(),
                                                              barn1.fødselsdato.plusYears(18).sisteDagIMåned().toString(),
                                                              behandling = behandling,
                                                              person = barn1)
        val andelTilkjentYtelseBarn2 = lagAndelTilkjentYtelse(barn2.fødselsdato.plusMonths(1).withDayOfMonth(1).toString(),
                                                              barn2.fødselsdato.plusYears(18).sisteDagIMåned().toString(),
                                                              behandling = behandling,
                                                              person = barn2)

        every { behandlingRepository.getOne(any()) } returns behandling
        every { beregningService.hentTilkjentYtelseForBehandling(any()) } returns
                tilkjentYtelse.copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelseBarn1, andelTilkjentYtelseBarn2))
        every { persongrunnlagService.hentAktiv(any()) } returns personopplysningGrunnlag
        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
    }

    @Test
    fun hentVedtak() {
        val vedtak = stønadsstatistikkService.hentVedtak(1L)
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vedtak))

        assertEquals(2, vedtak.utbetalingsperioder[0].utbetalingsDetaljer.size)
        assertEquals(2 * sats(YtelseType.ORDINÆR_BARNETRYGD), vedtak.utbetalingsperioder[0].utbetaltPerMnd)
    }
}