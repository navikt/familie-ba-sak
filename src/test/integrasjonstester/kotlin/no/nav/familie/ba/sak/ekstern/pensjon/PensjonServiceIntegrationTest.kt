package no.nav.familie.ba.sak.ekstern.pensjon

import io.mockk.every
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
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

class PensjonServiceIntegrationTest : AbstractSpringIntegrationTest() {
    @Autowired
    lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var personidentService: PersonidentService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var pensjonService: PensjonService

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var behandlingHentOgPersisterService: BehandlingHentOgPersisterService

    @Autowired
    lateinit var infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient

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

    private fun leggTilAvsluttetBehandling(
        fagsak: Fagsak,
        barn1: Person,
        barnAktør: Aktør
    ) {
        with(behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))) {
            val behandling = this
            with(lagInitiellTilkjentYtelse(behandling, "utbetalingsoppdrag")) {
                val andel = lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2023-03"),
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

    private fun mockInfotrygdBarnetrygdResponse(søkerAktør: Aktør) {
        every { infotrygdBarnetrygdClient.hentBarnetrygdTilPensjon(any(), any()) } returns BarnetrygdTilPensjonResponse(
            fagsaker = listOf(
                BarnetrygdTilPensjon(
                    "", listOf(
                        BarnetrygdPeriode(
                            personIdent = søkerAktør.aktivFødselsnummer(),
                            delingsprosentYtelse = YtelseProsent.FULL,
                            ytelseTypeEkstern = YtelseTypeEkstern.ORDINÆR_BARNETRYGD,
                            stønadFom = YearMonth.now(),
                            stønadTom = YearMonth.now(),
                            kildesystem = "Infotrygd"
                        )
                    )
                )
            )
        )
    }
}
