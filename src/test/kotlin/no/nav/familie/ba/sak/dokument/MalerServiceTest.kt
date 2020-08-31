package no.nav.familie.ba.sak.dokument

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.client.Enhet
import no.nav.familie.ba.sak.client.Norg2RestClient
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.ClientMocks.Companion.barnFnr
import no.nav.familie.ba.sak.config.ClientMocks.Companion.søkerFnr
import no.nav.familie.ba.sak.dokument.domene.maler.InnvilgetAutovedtak
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MalerServiceTest {

    private val norg2RestClient: Norg2RestClient = mockk()
    private val beregningService: BeregningService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()
    private val søknadGrunnlagService: SøknadGrunnlagService = mockk()

    private val malerService: MalerService = MalerService(mockk(), beregningService, persongrunnlagService, norg2RestClient, arbeidsfordelingService, søknadGrunnlagService)

    @Test
    fun `Skal returnere malnavn innvilget-tredjelandsborger for medlemskap TREDJELANDSBORGER og resultat INNVILGET`() {

        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(Medlemskap.TREDJELANDSBORGER,
                                                                      BehandlingResultatType.INNVILGET)

        assertEquals("innvilget-tredjelandsborger", malNavn)
    }

    @Test
    fun `Skal returnere malnavn innvilget for medlemskap NORDEN og resultat INNVILGET`() {

        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(Medlemskap.NORDEN,
                                                                      BehandlingResultatType.INNVILGET)

        assertEquals("innvilget", malNavn)
    }

    @Test
    fun `Skal returnere malnavn innvilget for resultat INNVILGET når medlemskap er null`() {
        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(null,
                                                                      BehandlingResultatType.INNVILGET)

        assertEquals("innvilget", malNavn)
    }

    @Test
    fun `test mapTilInnvilgetBrevfelter for innvilget autovedtak med ett barn`() {
        every { norg2RestClient.hentEnhet(any()) } returns Enhet(1L, "enhet")

        val behandling = lagBehandling().copy(opprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE,
                                              fagsak = Fagsak(søkerIdenter = setOf(defaultFagsak.søkerIdenter.first()
                                                                                           .copy(personIdent = PersonIdent(
                                                                                                   søkerFnr[0])))))
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList().subList(0, 1))
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val fødselsdato = personopplysningGrunnlag.barna.first().fødselsdato
        val vedtak = lagVedtak(behandling)//.copy(ansvarligEnhet = "enhet", vedtaksdato = fødselsdato.plusDays(7))
            vedtak.ansvarligEnhet = "enhet"
            vedtak.vedtaksdato = fødselsdato.plusDays(7)
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(fødselsdato.plusMonths(1).withDayOfMonth(1).toString(),
                                                         fødselsdato.plusYears(18).toString(),
                                                         behandling = behandling,
                                                         person = personopplysningGrunnlag.barna.first())

        every { persongrunnlagService.hentSøker(any()) } returns personopplysningGrunnlag.søker.first()
        every { persongrunnlagService.hentAktiv(any()) } returns personopplysningGrunnlag
        every { beregningService.hentTilkjentYtelseForBehandling(any()) } returns tilkjentYtelse.copy(
                andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelse))

        val brevfelter = malerService.mapTilVedtakBrevfelter(vedtak, BehandlingResultatType.INNVILGET)

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

        val behandling = lagBehandling().copy(opprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE,
                                              fagsak = Fagsak(søkerIdenter = setOf(defaultFagsak.søkerIdenter.first()
                                                                                           .copy(personIdent = PersonIdent(
                                                                                                   søkerFnr[0])))))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList())
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val barn1 = personopplysningGrunnlag.barna.first()
        val barn2 = personopplysningGrunnlag.barna.last()
        val vedtak = lagVedtak(behandling)
            vedtak.ansvarligEnhet = "enhet"
            vedtak.vedtaksdato = barn2.fødselsdato.plusMonths(6)
        val andelTilkjentYtelseBarn1 = lagAndelTilkjentYtelse(barn1.fødselsdato.plusMonths(1).withDayOfMonth(1).toString(),
                                                              barn1.fødselsdato.plusYears(18).sisteDagIMåned().toString(),
                                                              behandling = behandling,
                                                              person = barn1)
        val andelTilkjentYtelseBarn2 = lagAndelTilkjentYtelse(barn2.fødselsdato.plusMonths(1).withDayOfMonth(1).toString(),
                                                              barn2.fødselsdato.plusYears(18).sisteDagIMåned().toString(),
                                                              behandling = behandling,
                                                              person = barn2)

        every { persongrunnlagService.hentSøker(any()) } returns personopplysningGrunnlag.søker.first()
        every { persongrunnlagService.hentAktiv(any()) } returns personopplysningGrunnlag
        every { beregningService.hentTilkjentYtelseForBehandling(any()) } returns
                tilkjentYtelse.copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelseBarn1, andelTilkjentYtelseBarn2))

        val brevfelter = malerService.mapTilVedtakBrevfelter(vedtak, BehandlingResultatType.INNVILGET)

        val autovedtakBrevfelter = objectMapper.readValue(brevfelter.fletteFelter, InnvilgetAutovedtak::class.java)

        assertEquals(søkerFnr[0], autovedtakBrevfelter.fodselsnummer)
        assertEquals(Utils.formaterBeløp(andelTilkjentYtelseBarn1.beløp + andelTilkjentYtelseBarn2.beløp),
                     autovedtakBrevfelter.belop)
        assertEquals(andelTilkjentYtelseBarn2.stønadFom.tilMånedÅr(), autovedtakBrevfelter.virkningstidspunkt)
        assertEquals("${barn2.fødselsdato.tilKortString()} og ${barn1.fødselsdato.tilKortString()}",
                     autovedtakBrevfelter.fodselsdato)
        assertEquals(null, autovedtakBrevfelter.etterbetalingsbelop) // skal sende inntil vi får hentet beløpet fra simulering
        assertEquals(2, autovedtakBrevfelter.antallBarn)
    }
}