package no.nav.familie.ba.sak.dokument

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.UtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.client.Enhet
import no.nav.familie.ba.sak.client.Norg2RestClient
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.ClientMocks.Companion.barnFnr
import no.nav.familie.ba.sak.config.ClientMocks.Companion.søkerFnr
import no.nav.familie.ba.sak.dokument.domene.maler.Innvilget
import no.nav.familie.ba.sak.dokument.domene.maler.InnvilgetAutovedtak
import no.nav.familie.ba.sak.dokument.domene.maler.Opphørt
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.RestSimulerResultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class MalerServiceTest {

    private val norg2RestClient: Norg2RestClient = mockk()
    private val beregningService: BeregningService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk(relaxed = true)
    private val økonomiService: ØkonomiService = mockk()

    private val totrinnskontrollService: TotrinnskontrollService = mockk()

    private val malerService: MalerService = MalerService(totrinnskontrollService,
                                                          beregningService,
                                                          persongrunnlagService,
                                                          arbeidsfordelingService,
                                                          økonomiService)

    @Test
    fun `test mapTilInnvilgetBrevfelter for innvilget autovedtak med ett barn`() {
        every { norg2RestClient.hentEnhet(any()) } returns Enhet(1L, "enhet")

        val behandling = lagBehandling().copy(
                opprettetÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
                skalBehandlesAutomatisk = true,
                fagsak = Fagsak(søkerIdenter = setOf(defaultFagsak.søkerIdenter.first()
                                                             .copy(personIdent = PersonIdent(
                                                                     søkerFnr[0]))))
        )

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList().subList(0, 1))
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val fødselsdato = personopplysningGrunnlag.barna.first().fødselsdato
        val vedtak = lagVedtak(behandling)
        vedtak.vedtaksdato = fødselsdato.plusDays(7).atStartOfDay()
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(fødselsdato.nesteMåned().toString(),
                                                         fødselsdato.plusYears(18).forrigeMåned().toString(),
                                                         YtelseType.ORDINÆR_BARNETRYGD,
                                                         behandling = behandling,
                                                         person = personopplysningGrunnlag.barna.first())

        every { persongrunnlagService.hentSøker(any()) } returns personopplysningGrunnlag.søker
        every { persongrunnlagService.hentAktiv(any()) } returns personopplysningGrunnlag
        every { beregningService.hentTilkjentYtelseForBehandling(any()) } returns tilkjentYtelse.copy(
                andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelse))
        every { beregningService.hentSisteTilkjentYtelseFørBehandling(any()) } returns
                tilkjentYtelse.copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelse))
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(andelTilkjentYtelse)

        every { økonomiService.hentEtterbetalingsbeløp(any()) } returns RestSimulerResultat(etterbetaling = 0)

        val brevfelter = malerService.mapTilVedtakBrevfelter(vedtak, BehandlingResultat.INNVILGET)

        val autovedtakBrevfelter = objectMapper.readValue(brevfelter.fletteFelter, InnvilgetAutovedtak::class.java)

        assertEquals(søkerFnr[0], autovedtakBrevfelter.fodselsnummer)
        assertEquals(Utils.formaterBeløp(andelTilkjentYtelse.beløp), autovedtakBrevfelter.belop)
        assertEquals(fødselsdato.plusMonths(1).tilMånedÅr(), autovedtakBrevfelter.virkningstidspunkt)
        assertEquals(personopplysningGrunnlag.barna.first().fødselsdato.tilKortString(), autovedtakBrevfelter.fodselsdato)
        assertEquals(null, autovedtakBrevfelter.etterbetalingsbelop)
    }

    @Test
    fun `test mapTilVedtakBrevfelter for innvilget autovedtak med flere barn og etterbetaling`() {
        every { norg2RestClient.hentEnhet(any()) } returns Enhet(1L, "enhet")

        val behandling = lagBehandling().copy(
                opprettetÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
                skalBehandlesAutomatisk = true,
                fagsak = Fagsak(søkerIdenter = setOf(defaultFagsak.søkerIdenter.first()
                                                             .copy(personIdent = PersonIdent(
                                                                     søkerFnr[0]))))
        )

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList())
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val barn1 = personopplysningGrunnlag.barna.first()
        val barn2 = personopplysningGrunnlag.barna.last()
        val vedtak = lagVedtak(behandling)
        vedtak.vedtaksdato = LocalDateTime.of(barn2.fødselsdato.year,
                                              barn2.fødselsdato.plusMonths(6).month,
                                              barn2.fødselsdato.dayOfMonth,
                                              4,
                                              35)
        val andelTilkjentYtelseBarn1 = lagAndelTilkjentYtelse(barn1.fødselsdato.nesteMåned().toString(),
                                                              barn1.fødselsdato.plusYears(18).forrigeMåned().toString(),
                                                              ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                                              behandling = behandling,
                                                              person = barn1)
        val andelTilkjentYtelseBarn2 = lagAndelTilkjentYtelse(barn2.fødselsdato.nesteMåned().toString(),
                                                              barn2.fødselsdato.plusYears(18).forrigeMåned().toString(),
                                                              ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                                              behandling = behandling,
                                                              person = barn2)

        every { persongrunnlagService.hentSøker(any()) } returns personopplysningGrunnlag.søker
        every { persongrunnlagService.hentAktiv(any()) } returns personopplysningGrunnlag
        every { beregningService.hentTilkjentYtelseForBehandling(any()) } returns
                tilkjentYtelse.copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelseBarn1, andelTilkjentYtelseBarn2))
        every { beregningService.hentSisteTilkjentYtelseFørBehandling(any()) } returns
                tilkjentYtelse.copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelseBarn1, andelTilkjentYtelseBarn2))
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(andelTilkjentYtelseBarn1, andelTilkjentYtelseBarn2)

        every { økonomiService.hentEtterbetalingsbeløp(any()) } returns RestSimulerResultat(etterbetaling = 1054)

        val brevfelter = malerService.mapTilVedtakBrevfelter(vedtak, BehandlingResultat.INNVILGET)

        val autovedtakBrevfelter = objectMapper.readValue(brevfelter.fletteFelter, InnvilgetAutovedtak::class.java)

        assertEquals(søkerFnr[0], autovedtakBrevfelter.fodselsnummer)
        assertEquals(Utils.formaterBeløp(andelTilkjentYtelseBarn1.beløp + andelTilkjentYtelseBarn2.beløp),
                     autovedtakBrevfelter.belop)
        assertEquals(andelTilkjentYtelseBarn2.stønadFom.tilMånedÅr(), autovedtakBrevfelter.virkningstidspunkt)
        assertEquals("${barn2.fødselsdato.tilKortString()} og ${barn1.fødselsdato.tilKortString()}",
                     autovedtakBrevfelter.fodselsdato)
        assertEquals(Utils.formaterBeløp(1054), autovedtakBrevfelter.etterbetalingsbelop)
        assertEquals(2, autovedtakBrevfelter.antallBarn)
    }

    @Test
    fun `test mapTilOpphørtBrevfelter for opphørt vedtak`() {
        every { norg2RestClient.hentEnhet(any()) } returns Enhet(1L, "enhet")

        val behandling = lagBehandling().copy(
                opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                resultat = BehandlingResultat.OPPHØRT,
                skalBehandlesAutomatisk = false,
                fagsak = Fagsak(søkerIdenter = setOf(defaultFagsak.søkerIdenter.first()
                                                             .copy(personIdent = PersonIdent(
                                                                     søkerFnr[0]))))
        )

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList().subList(0, 1))
        val fødselsdato = personopplysningGrunnlag.barna.first().fødselsdato
        val vedtak = lagVedtak(behandling).also {
            it.utbetalingBegrunnelser.add(UtbetalingBegrunnelse(vedtak = it,
                                                                fom = LocalDate.now(),
                                                                tom = LocalDate.now(),
                                                                brevBegrunnelse = "Begrunnelse"))
        }

        val stønadFom = YearMonth.now().minusMonths(1)
        val stønadTom = YearMonth.now()

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling).also {
            it.stønadFom = stønadFom
            it.stønadTom = stønadTom
        }

        val andelTilkjentYtelse = lagAndelTilkjentYtelse(stønadFom.toString(),
                                                         stønadTom.toString(),
                                                         YtelseType.ORDINÆR_BARNETRYGD,
                                                         tilkjentYtelse = tilkjentYtelse,
                                                         behandling = behandling,
                                                         person = personopplysningGrunnlag.barna.first())

        vedtak.vedtaksdato = fødselsdato.plusDays(7).atStartOfDay()
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(any()) } returns listOf(andelTilkjentYtelse)

        every { persongrunnlagService.hentSøker(any()) } returns personopplysningGrunnlag.søker
        every { persongrunnlagService.hentAktiv(any()) } returns personopplysningGrunnlag
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(behandling = behandling,
                                                                                                 aktiv = true,
                                                                                                 saksbehandler = "System",
                                                                                                 beslutter = "Beslutter",
                                                                                                 godkjent = true)

        val brevfelter = malerService.mapTilVedtakBrevfelter(vedtak, BehandlingResultat.OPPHØRT)

        val opphørt = objectMapper.readValue(brevfelter.fletteFelter, Opphørt::class.java)

        assertEquals("Begrunnelse", opphørt.opphor.begrunnelser.get(0))
        assertEquals(stønadTom.atEndOfMonth().plusDays(1).tilDagMånedÅr().toString(), opphørt.opphor.dato.toString())
        assertEquals("System", opphørt.saksbehandler)
        assertEquals("Beslutter", opphørt.beslutter)
        assertEquals("NB", opphørt.maalform.name)
    }

    @Test
    fun `test mapTilVedtakBrevfelter for behandlingsresultat endret og opphørt med fire perioder to Opphørt og to innvilget`() {
        every { norg2RestClient.hentEnhet(any()) } returns Enhet(1L, "enhet")

        val behandling = lagBehandling().copy(
                opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                resultat = BehandlingResultat.ENDRING_OG_OPPHØRT,
                skalBehandlesAutomatisk = false,
                fagsak = Fagsak(søkerIdenter = setOf(defaultFagsak.søkerIdenter.first()
                                                             .copy(personIdent = PersonIdent(
                                                                     søkerFnr[0]))))
        )
        val innvilgetPeriode1Fom = YearMonth.now().minusMonths(6)
        val innvilgetPeriode1Tom = YearMonth.now().minusMonths(4)
        val innvilgetPeriode2Fom = YearMonth.now().minusMonths(2)
        val innvilgetPeriode2Tom = YearMonth.now()

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList().subList(0, 1))
        val fødselsdato = personopplysningGrunnlag.barna.first().fødselsdato
        val vedtak = lagVedtak(behandling).also {
            it.utbetalingBegrunnelser.addAll(listOf(UtbetalingBegrunnelse(vedtak = it,
                                                                fom = innvilgetPeriode1Fom.førsteDagIInneværendeMåned(),
                                                                tom = innvilgetPeriode1Tom.sisteDagIInneværendeMåned(),
                                                                begrunnelseType = VedtakBegrunnelseType.INNVILGELSE,
                                                                vedtakBegrunnelse = VedtakBegrunnelse.INNVILGET_BOR_HOS_SØKER,
                                                                brevBegrunnelse = "Innvilget begrunnelse en"),
                                                    UtbetalingBegrunnelse(vedtak = it,
                                                                fom = innvilgetPeriode1Fom.førsteDagIInneværendeMåned(),
                                                                tom = innvilgetPeriode1Tom.sisteDagIInneværendeMåned(),
                                                                begrunnelseType = VedtakBegrunnelseType.OPPHØR,
                                                                vedtakBegrunnelse = VedtakBegrunnelse.OPPHØR_BARN_FLYTTET_FRA_SØKER,
                                                                brevBegrunnelse = "Opphør begrunnelse en"),
                                                    UtbetalingBegrunnelse(vedtak = it,
                                                                fom = innvilgetPeriode2Fom.førsteDagIInneværendeMåned(),
                                                                tom = innvilgetPeriode2Tom.sisteDagIInneværendeMåned(),
                                                                begrunnelseType = VedtakBegrunnelseType.INNVILGELSE,
                                                                vedtakBegrunnelse = VedtakBegrunnelse.INNVILGET_BOR_HOS_SØKER,
                                                                brevBegrunnelse = "Innvilget begrunnelse to"),
                                                    UtbetalingBegrunnelse(vedtak = it,
                                                                fom = innvilgetPeriode2Fom.førsteDagIInneværendeMåned(),
                                                                tom = innvilgetPeriode2Tom.sisteDagIInneværendeMåned(),
                                                                begrunnelseType = VedtakBegrunnelseType.OPPHØR,
                                                                vedtakBegrunnelse = VedtakBegrunnelse.OPPHØR_BARN_FLYTTET_FRA_SØKER,
                                                                brevBegrunnelse = "Opphør begrunnelse to"))
            )
        }

        val tilkjentYtelse1 = lagInitiellTilkjentYtelse(behandling).also {
            it.stønadFom = innvilgetPeriode1Fom
            it.stønadTom = innvilgetPeriode1Tom
        }

        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling).also {
            it.stønadFom = innvilgetPeriode1Fom
            it.stønadTom = innvilgetPeriode1Tom
        }

        val andelTilkjentYtelse1 = lagAndelTilkjentYtelse(innvilgetPeriode1Fom.toString(),
                                                         innvilgetPeriode1Tom.toString(),
                                                         YtelseType.ORDINÆR_BARNETRYGD,
                                                         tilkjentYtelse = tilkjentYtelse1,
                                                         behandling = behandling,
                                                         person = personopplysningGrunnlag.barna.first())

        val andelTilkjentYtelse2 = lagAndelTilkjentYtelse(innvilgetPeriode2Fom.toString(),
                                                         innvilgetPeriode2Tom.toString(),
                                                         YtelseType.ORDINÆR_BARNETRYGD,
                                                         tilkjentYtelse = tilkjentYtelse2,
                                                         behandling = behandling,
                                                         person = personopplysningGrunnlag.barna.first())

        vedtak.vedtaksdato = fødselsdato.plusDays(7).atStartOfDay()
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(any()) } returns listOf(andelTilkjentYtelse1, andelTilkjentYtelse2)

        every { persongrunnlagService.hentSøker(any()) } returns personopplysningGrunnlag.søker
        every { persongrunnlagService.hentAktiv(any()) } returns personopplysningGrunnlag
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(behandling = behandling,
                                                                                                 aktiv = true,
                                                                                                 saksbehandler = "System",
                                                                                                 beslutter = "Beslutter",
                                                                                                 godkjent = true)
        every { økonomiService.hentEtterbetalingsbeløp(any()) } returns RestSimulerResultat(etterbetaling = 0)

        val brevfelter = malerService.mapTilVedtakBrevfelter(vedtak, BehandlingResultat.ENDRING_OG_OPPHØRT)

        val innvilget = objectMapper.readValue(brevfelter.fletteFelter, Innvilget::class.java)

        assertEquals(4, innvilget.duFaar.size)

        assertEquals("INNVILGET", innvilget.duFaar[0].begrunnelseType)
        assertEquals(innvilgetPeriode1Fom.førsteDagIInneværendeMåned().tilDagMånedÅr(), innvilget.duFaar[0].fom)
        assertEquals(innvilgetPeriode1Tom.sisteDagIInneværendeMåned().tilDagMånedÅr(), innvilget.duFaar[0].tom)
        assertEquals("Innvilget begrunnelse en", innvilget.duFaar[0].begrunnelser[0])

        assertEquals("OPPHØR", innvilget.duFaar[1].begrunnelseType)
        assertEquals(innvilgetPeriode1Tom.plusMonths(1).førsteDagIInneværendeMåned().tilDagMånedÅr(), innvilget.duFaar[1].fom)
        assertEquals(innvilgetPeriode2Fom.minusMonths(1).sisteDagIInneværendeMåned().tilDagMånedÅr(), innvilget.duFaar[1].tom)
        assertEquals("Opphør begrunnelse en", innvilget.duFaar[1].begrunnelser[0])

        assertEquals("INNVILGET", innvilget.duFaar[2].begrunnelseType)
        assertEquals(innvilgetPeriode2Fom.førsteDagIInneværendeMåned().tilDagMånedÅr(), innvilget.duFaar[2].fom)
        assertEquals(innvilgetPeriode2Tom.sisteDagIInneværendeMåned().tilDagMånedÅr(), innvilget.duFaar[2].tom)
        assertEquals("Innvilget begrunnelse to", innvilget.duFaar[2].begrunnelser[0])

        assertEquals("OPPHØR", innvilget.duFaar[3].begrunnelseType)
        assertEquals(innvilgetPeriode2Tom.plusMonths(1).førsteDagIInneværendeMåned().tilDagMånedÅr(), innvilget.duFaar[3].fom)
        assertEquals("", innvilget.duFaar[3].tom)
        assertEquals("Opphør begrunnelse to", innvilget.duFaar[3].begrunnelser[0])

    }
}