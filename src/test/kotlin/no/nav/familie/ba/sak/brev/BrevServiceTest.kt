package no.nav.familie.ba.sak.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.BrevPeriodeService
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.PeriodeType
import no.nav.familie.ba.sak.brev.domene.maler.VedtakEndring
import no.nav.familie.ba.sak.brev.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.client.Enhet
import no.nav.familie.ba.sak.client.Norg2RestClient
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.oppdrag.RestSimulerResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrevServiceTest {

    private val norg2RestClient: Norg2RestClient = mockk()
    private val beregningService: BeregningService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val økonomiService: ØkonomiService = mockk()
    private val totrinnskontrollService: TotrinnskontrollService = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk(relaxed = true)
    private val brevPeriodeService: BrevPeriodeService = mockk()
    private val brevService: BrevService = BrevService(
            totrinnskontrollService,
            persongrunnlagService,
            arbeidsfordelingService,
            økonomiService,
            brevPeriodeService,
    )

    @Test
    fun `test hentVedtaksbrevtype gir riktig vedtaksbrevtype`() {
        Assertions.assertEquals(
                brevService.hentVedtaksbrevtype(
                        false,
                        BehandlingType.FØRSTEGANGSBEHANDLING,
                        BehandlingResultat.INNVILGET),
                Vedtaksbrevtype.FØRSTEGANGSVEDTAK)

        Assertions.assertEquals(
                brevService.hentVedtaksbrevtype(
                        false,
                        BehandlingType.REVURDERING,
                        BehandlingResultat.INNVILGET),
                Vedtaksbrevtype.VEDTAK_ENDRING)

        Assertions.assertEquals(
                brevService.hentVedtaksbrevtype(
                        false,
                        BehandlingType.REVURDERING,
                        BehandlingResultat.OPPHØRT),
                Vedtaksbrevtype.OPPHØRT)

        Assertions.assertEquals(
                brevService.hentVedtaksbrevtype(
                        false,
                        BehandlingType.REVURDERING,
                        BehandlingResultat.INNVILGET_OG_OPPHØRT),
                Vedtaksbrevtype.OPPHØRT_ENDRING)
    }

    @Test
    fun `test mapTilNyttVedtaksbrev for 'Vedtak endring' med ett barn`() {
        every { norg2RestClient.hentEnhet(any()) } returns Enhet(1L, "enhet")

        val behandling = lagBehandling().copy(
                opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                fagsak = Fagsak(søkerIdenter = setOf(defaultFagsak.søkerIdenter.first()
                                                             .copy(personIdent = PersonIdent(
                                                                     ClientMocks.søkerFnr[0])))),
                type = BehandlingType.REVURDERING
        )

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id,
                                                                       ClientMocks.søkerFnr[0],
                                                                       ClientMocks.barnFnr.toList().subList(0, 1))
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val fødselsdato = personopplysningGrunnlag.barna.first().fødselsdato

        val andelTilkjentYtelse = lagAndelTilkjentYtelse(fødselsdato.nesteMåned().toString(),
                                                         fødselsdato.plusYears(18).forrigeMåned().toString(),
                                                         YtelseType.ORDINÆR_BARNETRYGD,
                                                         behandling = behandling,
                                                         person = personopplysningGrunnlag.barna.first())

        val vedtak = lagVedtak(behandling = behandling).also {
            it.vedtakBegrunnelser.add(VedtakBegrunnelse(vedtak = it,
                                                        fom = LocalDate.now(),
                                                        tom = LocalDate.now(),
                                                        brevBegrunnelse = "Begrunnelse",
                                                        begrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_NYFØDT_BARN))
        }
        vedtak.vedtaksdato = fødselsdato.plusDays(7).atStartOfDay()

        every { persongrunnlagService.hentSøker(any()) } returns personopplysningGrunnlag.søker
        every { persongrunnlagService.hentAktiv(any()) } returns personopplysningGrunnlag
        every { beregningService.hentTilkjentYtelseForBehandling(any()) } returns tilkjentYtelse.copy(
                andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelse))
        every { beregningService.hentSisteTilkjentYtelseFørBehandling(any()) } returns
                tilkjentYtelse.copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelse))
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(andelTilkjentYtelse)
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(behandling = behandling,
                                                                                                 aktiv = true,
                                                                                                 saksbehandler = "System",
                                                                                                 saksbehandlerId = "systemId",
                                                                                                 beslutter = "Beslutter",
                                                                                                 beslutterId = "beslutterId",
                                                                                                 godkjent = true)
        every { brevPeriodeService.hentVedtaksperioder(any()) } returns listOf(BrevPeriode(fom = LocalDate.now().toString(),
                                                                                           tom = LocalDate.now().toString(),
                                                                                           belop = "1000",
                                                                                           antallBarn = "1",
                                                                                           barnasFodselsdager = "01.01.2020",
                                                                                           begrunnelser = listOf(""),
                                                                                           type = PeriodeType.INNVILGELSE))

        every { økonomiService.hentEtterbetalingsbeløp(any()) } returns RestSimulerResultat(etterbetaling = 0)

        var brevfelter = brevService.hentVedtaksbrevData(vedtak, BehandlingResultat.INNVILGET)

        Assertions.assertTrue(brevfelter is VedtakEndring)
        brevfelter = brevfelter as VedtakEndring

        Assertions.assertEquals(ClientMocks.søkerFnr[0], brevfelter.data.flettefelter.fodselsnummer[0])
        Assertions.assertEquals(null, brevfelter.data.delmalData.etterbetaling?.etterbetalingsbelop)
    }

}