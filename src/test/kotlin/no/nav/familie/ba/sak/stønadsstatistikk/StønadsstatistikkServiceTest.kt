package no.nav.familie.ba.sak.stønadsstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.ClientMocks.Companion.barnFnr
import no.nav.familie.ba.sak.config.ClientMocks.Companion.søkerFnr
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.økonomi.sats
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StønadsstatistikkServiceTest {

    private val behandlingService: BehandlingService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val beregningService: BeregningService = mockk()
    private val vedtakService: VedtakService = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()

    private val stønadsstatistikkService =
            StønadsstatistikkService(behandlingService, persongrunnlagService, beregningService, vedtakService, personopplysningerService)

    @BeforeAll
    fun init() {
        val behandling = lagBehandling()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList())
        val vedtak = lagVedtak(behandling)

        val barn1 = personopplysningGrunnlag.barna.first()
        val barn2 = personopplysningGrunnlag.barna.last()
        val andelTilkjentYtelseBarn1 = lagAndelTilkjentYtelse(barn1.fødselsdato.plusMonths(1).withDayOfMonth(1).toString(),
                barn1.fødselsdato.plusYears(3).sisteDagIMåned().toString(),
                YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = barn1)
        val andelTilkjentYtelseBarn2 = lagAndelTilkjentYtelse(barn2.fødselsdato.plusMonths(1).withDayOfMonth(1).toString(),
                barn2.fødselsdato.plusYears(18).sisteDagIMåned().toString(),
                YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = barn2)

        val andelTilkjentYtelseSøker = lagAndelTilkjentYtelseUtvidet(barn2.fødselsdato.plusMonths(1).withDayOfMonth(1).toString(),
                barn2.fødselsdato.plusYears(2).sisteDagIMåned().toString(),
                YtelseType.UTVIDET_BARNETRYGD,
                behandling = behandling,
                person = personopplysningGrunnlag.søker.first())

        every { behandlingService.hent(any()) } returns behandling
        every { beregningService.hentTilkjentYtelseForBehandling(any()) } returns
                tilkjentYtelse.copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelseBarn1, andelTilkjentYtelseBarn2, andelTilkjentYtelseSøker))
        every { persongrunnlagService.hentAktiv(any()) } returns personopplysningGrunnlag
        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(any()) } returns "DK"
    }

    @Test
    fun hentVedtak() {
        val vedtak = stønadsstatistikkService.hentVedtak(1L)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vedtak))

        assertEquals(3, vedtak.utbetalingsperioder[0].utbetalingsDetaljer.size)
        assertEquals(2 * sats(YtelseType.ORDINÆR_BARNETRYGD) + sats(YtelseType.UTVIDET_BARNETRYGD), vedtak.utbetalingsperioder[0].utbetaltPerMnd)
    }
}