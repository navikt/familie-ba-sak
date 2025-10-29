package no.nav.familie.ba.sak.ekstern.pensjon

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.årMnd
import no.nav.familie.ba.sak.fake.FakeEnvService
import no.nav.familie.ba.sak.fake.FakeInfotrygdBarnetrygdKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagMinimalUtbetalingsoppdragString
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class PensjonServiceIntegrationTest(
    @Autowired
    private val personidentService: PersonidentService,
    @Autowired
    private val behandlingService: BehandlingService,
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val pensjonService: PensjonService,
    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired
    private val fakeInfotrygdBarnetrygdKlient: FakeInfotrygdBarnetrygdKlient,
    @Autowired
    private val envService: FakeEnvService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `skal finne en relaterte fagsaker per barn`() {
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak, barn1, barnAktør)

        val fagsak2 = fagsakService.hentEllerOpprettFagsakForPersonIdent(barn1.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak2, barn1, barnAktør)

        val barnetrygdTilPensjon = pensjonService.hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
        assertThat(barnetrygdTilPensjon).hasSize(2)
    }

    @Test
    fun `skal inkludere periode fra Infotrygd sammen med perioden fra BA-sak på den samme identen`() {
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak, barn1, barnAktør)

        val fagsak2 = fagsakService.hentEllerOpprettFagsakForPersonIdent(barn1.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak2, barn1, barnAktør)

        mockInfotrygdBarnetrygdResponse(søkerAktør)

        val barnetrygdTilPensjon = pensjonService.hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
        assertThat(barnetrygdTilPensjon).hasSize(2)
        assertThat(barnetrygdTilPensjon.filter { it.barnetrygdPerioder.any { it.kildesystem == "Infotrygd" } }).hasSize(1)
        assertThat(barnetrygdTilPensjon.filter { it.barnetrygdPerioder.all { it.kildesystem == "Infotrygd" } }).hasSize(0)
    }

    @Test
    fun `skal fjerne overlapp ved å kutte perioden fra Infotrygd til før perioden for den samme personen starter i BA-sak`() {
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(
            fagsak,
            barn1,
            barnAktør,
            fom = årMnd("2021-04"),
            tom = årMnd("2023-11"),
        )
        val infotrygdStønadFom = årMnd("2019-03")
        val infotrygdStønadTom = årMnd("2022-09")

        mockInfotrygdBarnetrygdResponse(
            søkerAktør,
            barnAktør,
            stønadFom = infotrygdStønadFom,
            stønadTom = infotrygdStønadTom,
        )

        val (basakPeriode, infotrygdperiode) =
            pensjonService
                .hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
                .single()
                .barnetrygdPerioder
                .partition { it.kildesystem == "BA" }
                .run { first.single() to second.single() }

        assertThat(infotrygdperiode.stønadFom).isEqualTo(infotrygdStønadFom)
        assertThat(infotrygdperiode.stønadTom).isEqualTo(basakPeriode.stønadFom.minusMonths(1))
    }

    @Test
    fun `skal finne og returnere perioder fra Infotrygd som har infotrygd sin definisjon på uendelighet`() {
        val søker = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)

        mockInfotrygdBarnetrygdResponse(søker = søkerAktør, stønadFom = YearMonth.now(), stønadTom = YearMonth.of(999999999, 12))

        val barnetrygdTilPensjon = pensjonService.hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
        assertThat(barnetrygdTilPensjon).hasSize(1)
        assertThat(barnetrygdTilPensjon.filter { it.barnetrygdPerioder.all { it.kildesystem == "Infotrygd" } }).hasSize(1)
    }

    @Test
    fun `skal finne og returnere perioder fra Infotrygd`() {
        val søker = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)

        mockInfotrygdBarnetrygdResponse(søkerAktør)

        val barnetrygdTilPensjon = pensjonService.hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
        assertThat(barnetrygdTilPensjon).hasSize(1)
        assertThat(barnetrygdTilPensjon.filter { it.barnetrygdPerioder.all { it.kildesystem == "Infotrygd" } }).hasSize(1)
    }

    private fun leggTilAvsluttetBehandling(
        fagsak: Fagsak,
        barn1: Person,
        barnAktør: Aktør,
        fom: YearMonth = årMnd("2019-04"),
        tom: YearMonth = årMnd("2023-03"),
    ) {
        with(behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))) {
            val behandling = this
            with(lagInitiellTilkjentYtelse(behandling, lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id))) {
                val andel =
                    lagAndelTilkjentYtelse(
                        fom,
                        tom,
                        YtelseType.ORDINÆR_BARNETRYGD,
                        660,
                        behandling,
                        person = barn1,
                        aktør = barnAktør,
                        tilkjentYtelse = this,
                    )
                andelerTilkjentYtelse.add(andel)
                tilkjentYtelseRepository.save(this)
            }
            avsluttOgLagreBehandling(behandling)
        }
    }

    private fun avsluttOgLagreBehandling(behandling: Behandling) {
        behandling.status = BehandlingStatus.AVSLUTTET
        behandling.leggTilBehandlingStegTilstand(StegType.BEHANDLING_AVSLUTTET)
        behandlingHentOgPersisterService.lagreEllerOppdater(behandling, false)
    }

    private fun mockInfotrygdBarnetrygdResponse(
        søker: Aktør,
        barn: Aktør? = null,
        stønadFom: YearMonth = YearMonth.now(),
        stønadTom: YearMonth = YearMonth.now(),
    ) {
        envService.setErPreprod(false)

        fakeInfotrygdBarnetrygdKlient.leggTilBarnetrygdTilPensjon(
            søker.aktivFødselsnummer(),
            BarnetrygdTilPensjonResponse(
                fagsaker =
                    listOf(
                        BarnetrygdTilPensjon(
                            søker.aktivFødselsnummer(),
                            listOf(
                                BarnetrygdPeriode(
                                    personIdent = barn?.aktivFødselsnummer() ?: søker.aktivFødselsnummer(),
                                    delingsprosentYtelse = YtelseProsent.FULL,
                                    ytelseTypeEkstern = YtelseTypeEkstern.ORDINÆR_BARNETRYGD,
                                    utbetaltPerMnd = 1054,
                                    stønadFom = stønadFom,
                                    stønadTom = stønadTom,
                                    kildesystem = "Infotrygd",
                                    sakstypeEkstern = SakstypeEkstern.NASJONAL,
                                ),
                            ),
                        ),
                    ),
            ),
        )
    }
}
