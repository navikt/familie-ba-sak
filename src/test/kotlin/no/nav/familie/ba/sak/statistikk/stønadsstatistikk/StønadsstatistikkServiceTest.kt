package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseUtvidet
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.ClientMocks.Companion.barnFnr
import no.nav.familie.ba.sak.config.ClientMocks.Companion.søkerFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.økonomi.sats
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockKExtension::class)
internal class StønadsstatistikkServiceTest(
    @MockK
    private val behandlingService: BehandlingService,

    @MockK
    private val persongrunnlagService: PersongrunnlagService,

    @MockK
    private val beregningService: BeregningService,

    @MockK
    private val vedtakService: VedtakService,

    @MockK
    private val personopplysningerService: PersonopplysningerService,

    @MockK
    private val vedtakRepository: VedtakRepository,
) {

    private val stønadsstatistikkService =
        StønadsstatistikkService(
            behandlingService,
            persongrunnlagService,
            beregningService,
            vedtakService,
            personopplysningerService,
            vedtakRepository
        )
    private val behandling = lagBehandling()
    private val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList())
    private val barn1 = personopplysningGrunnlag.barna.first()
    private val barn2 = personopplysningGrunnlag.barna.last()

    @BeforeAll
    fun init() {
        MockKAnnotations.init(this)

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val vedtak = lagVedtak(behandling)

        val andelTilkjentYtelseBarn1 = lagAndelTilkjentYtelse(
            barn1.fødselsdato.nesteMåned(),
            barn1.fødselsdato.plusYears(3).toYearMonth(),
            YtelseType.ORDINÆR_BARNETRYGD,
            behandling = behandling,
            person = barn1,
            periodeIdOffset = 1

        )
        val andelTilkjentYtelseBarn2 = lagAndelTilkjentYtelse(
            barn2.fødselsdato.nesteMåned(),
            barn2.fødselsdato.plusYears(18).forrigeMåned(),
            YtelseType.ORDINÆR_BARNETRYGD,
            behandling = behandling,
            person = barn2,
            periodeIdOffset = 2
        )

        val andelTilkjentYtelseSøker = lagAndelTilkjentYtelseUtvidet(
            barn2.fødselsdato.nesteMåned().toString(),
            barn2.fødselsdato.plusYears(2).toYearMonth().toString(),
            YtelseType.UTVIDET_BARNETRYGD,
            behandling = behandling,
            person = personopplysningGrunnlag.søker,
            periodeIdOffset = 3
        )

        every { behandlingService.hent(any()) } returns behandling
        every { beregningService.hentTilkjentYtelseForBehandling(any()) } returns
            tilkjentYtelse.copy(
                andelerTilkjentYtelse = mutableSetOf(
                    andelTilkjentYtelseBarn1,
                    andelTilkjentYtelseBarn2,
                    andelTilkjentYtelseSøker
                )
            )
        every { persongrunnlagService.hentAktiv(any()) } returns personopplysningGrunnlag
        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(any()) } returns "DK"
    }

    @Test
    fun hentVedtak() {
        val vedtak = stønadsstatistikkService.hentVedtak(1L)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vedtak))

        assertEquals(3, vedtak.utbetalingsperioder[0].utbetalingsDetaljer.size)
        assertEquals(
            2 * sats(YtelseType.ORDINÆR_BARNETRYGD) + sats(YtelseType.UTVIDET_BARNETRYGD),
            vedtak.utbetalingsperioder[0].utbetaltPerMnd
        )

        vedtak.utbetalingsperioder
            .flatMap { it.utbetalingsDetaljer.map { ud -> ud.person } }
            .filter { it.personIdent != søkerFnr[0] }
            .forEach {
                assertEquals(0, it.delingsprosentYtelse)
            }
    }

    @Test
    fun `hver utbetalingsDetalj innenfor en utbetalingsperiode skal ha unik delytelseId`() {
        val vedtak = stønadsstatistikkService.hentVedtak(1L)

        vedtak.utbetalingsperioder.forEach {
            assertEquals(it.utbetalingsDetaljer.size, it.utbetalingsDetaljer.distinctBy { it.delytelseId }.size)
        }
    }

    /**
     * Nye årsaker må legges til VedtakDVH når det legges til i Behandling
     *
     * Endringenen må være bakoverkompatibel. Hvis man f.eks. endrer navn på en årsak, så må man være sikker på at det ikke er sendt
     * et slik vedtak til stønaddstatistikk.
     *
     * Hvis det er sendt et slik vedtak, så legger man heller til den nye verdien i VedtakDVH og ikke slette gamle
     *
     */
    @Test
    fun `Skal gi feil hvis det kommer en ny BehandlingÅrsak som det ikke er tatt høyde for mot stønaddstatistkk - Man trenger å oppdatere schema og varsle stønaddstatistikk - Tips i javadoc`() {
        val behandlingsÅrsakIBASak = enumValues<BehandlingÅrsak>().map { it.name }
        val behandlingsÅrsakFraEksternKontrakt =
            enumValues<no.nav.familie.eksterne.kontrakter.BehandlingÅrsak>().map { it.name }

        assertThat(behandlingsÅrsakIBASak).hasSize(behandlingsÅrsakFraEksternKontrakt.size)
            .containsAll(behandlingsÅrsakFraEksternKontrakt)
    }

    /**
     * Nye behandlingstyper må legges til VedtakDVH når det legges til i Behandling
     *
     * Endringenen må være bakoverkompatibel. Hvis man f.eks. endrer navn på en type, så må man være sikker på at det ikke er sendt
     * et slik vedtak til stønaddstatistikk.
     *
     * Hvis det er sendt et slik vedtak, så legger man heller til den nye verdien i VedtakDVH og ikke slette gamle
     *
     */
    @Test
    fun `Skal gi feil hvis det kommer en ny BehandlingType som det ikke er tatt høyde for mot stønaddstatistkk - Man trenger å oppdatere schema og varsle stønaddstatistikk`() {
        val behandlingsTypeIBasak = enumValues<BehandlingType>().map { it.name }
        val behandlingsTypeFraStønadskontrakt =
            enumValues<no.nav.familie.eksterne.kontrakter.BehandlingType>().map { it.name }

        assertThat(behandlingsTypeIBasak).hasSize(behandlingsTypeFraStønadskontrakt.size)
            .containsAll(behandlingsTypeFraStønadskontrakt)
    }
}
