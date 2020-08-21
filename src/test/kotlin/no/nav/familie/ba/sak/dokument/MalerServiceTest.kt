package no.nav.familie.ba.sak.dokument

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
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
import java.time.LocalDate


class MalerServiceTest {

    private val norg2RestClient: Norg2RestClient = mockk()
    private val beregningService: BeregningService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()

    private val malerService: MalerService = MalerService(mockk(), beregningService, persongrunnlagService, norg2RestClient)

    @Test
    fun `Skal returnere malnavn innvilget-tredjelandsborger for medlemskap TREDJELANDSBORGER og resultat INNVILGET`() {

        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(Medlemskap.TREDJELANDSBORGER,
                                                                     BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "innvilget-tredjelandsborger")
    }

    @Test
    fun `Skal returnere malnavn innvilget for medlemskap NORDEN og resultat INNVILGET`() {

        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(Medlemskap.NORDEN,
                                                                     BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "innvilget")
    }

    @Test
    fun `Skal returnere malnavn innvilget for resultat INNVILGET når medlemskap er null`() {
        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(null,
                                                                     BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "innvilget")
    }

    @Test
    fun `test mapTilBrevfelter for innvilget autovedtak med ett barn`() {
        every { norg2RestClient.hentEnhet(any()) } returns Enhet(1L, "enhet")

        val behandling = lagBehandling().copy(opprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE,
                                              fagsak = Fagsak(søkerIdenter = setOf(defaultFagsak.søkerIdenter.first()
                                                                                           .copy(personIdent = PersonIdent(søkerFnr[0])))))
        val vedtak = lagVedtak(behandling).copy(ansvarligEnhet = "enhet")
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList().subList(0,1))
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(LocalDate.now().toString(),
                                                         LocalDate.now().plusYears(1).toString(),
                                                         behandling = behandling,
                                                         person = personopplysningGrunnlag.barna.first())

        every { persongrunnlagService.hentSøker(any()) } returns personopplysningGrunnlag.søker.first()
        every { beregningService.hentTilkjentYtelseForBehandling(any()) } returns tilkjentYtelse.copy(andelerTilkjentYtelse = mutableSetOf(
                andelTilkjentYtelse))

        val brevfelter = malerService.mapTilBrevfelter(vedtak, personopplysningGrunnlag, BehandlingResultatType.INNVILGET)

        val autovedtakBrevfelter = objectMapper.readValue(brevfelter.fletteFelter, InnvilgetAutovedtak::class.java)

        assertEquals(autovedtakBrevfelter.fodselsnummer, søkerFnr[0])
        assertEquals(autovedtakBrevfelter.belop, Utils.formaterBeløp(andelTilkjentYtelse.beløp))
        assertEquals(autovedtakBrevfelter.virkningstidspunkt, LocalDate.now().tilMånedÅr())
        assertEquals(autovedtakBrevfelter.fodselsdato, personopplysningGrunnlag.barna.first().fødselsdato.tilKortString())
        assertEquals(autovedtakBrevfelter.erEtterbetaling, false)
    }

    @Test
    fun `test mapTilBrevfelter for innvilget autovedtak med flere barn og etterbetaling`() {
        every { norg2RestClient.hentEnhet(any()) } returns Enhet(1L, "enhet")

        val behandling = lagBehandling().copy(opprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE,
                                              fagsak = Fagsak(søkerIdenter = setOf(defaultFagsak.søkerIdenter.first()
                                                                                           .copy(personIdent = PersonIdent(søkerFnr[0])))))

        val vedtak = lagVedtak(behandling).copy(ansvarligEnhet = "enhet")
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList())
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val baseDato = LocalDate.now()
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

        every { persongrunnlagService.hentSøker(any()) } returns personopplysningGrunnlag.søker.first()
        every { beregningService.hentTilkjentYtelseForBehandling(any()) } returns
                tilkjentYtelse.copy(andelerTilkjentYtelse = mutableSetOf(andelTilkjentYtelseBarn1, andelTilkjentYtelseBarn2))

        val brevfelter = malerService.mapTilBrevfelter(vedtak, personopplysningGrunnlag, BehandlingResultatType.INNVILGET)

        val autovedtakBrevfelter = objectMapper.readValue(brevfelter.fletteFelter, InnvilgetAutovedtak::class.java)

        assertEquals(søkerFnr[0], autovedtakBrevfelter.fodselsnummer)
        assertEquals(Utils.formaterBeløp(andelTilkjentYtelseBarn1.beløp + andelTilkjentYtelseBarn2.beløp), autovedtakBrevfelter.belop)
        assertEquals(andelTilkjentYtelseBarn2.stønadFom.tilMånedÅr(), autovedtakBrevfelter.virkningstidspunkt)
        assertEquals("${barn1.fødselsdato.tilKortString()} og ${barn2.fødselsdato.tilKortString()}", autovedtakBrevfelter.fodselsdato)
        assertEquals(true, autovedtakBrevfelter.erEtterbetaling)
        assertEquals("12 648", autovedtakBrevfelter.etterbetalingsbelop)
        assertEquals(2, autovedtakBrevfelter.antallBarn)
    }
}