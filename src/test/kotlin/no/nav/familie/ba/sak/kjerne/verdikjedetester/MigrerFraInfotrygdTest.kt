package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.MigreringService
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class MigrerFraInfotrygdTest(
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val migreringService: MigreringService,
    @Autowired private val mockLocalDateService: LocalDateService,
    @Autowired private val featureToggleService: FeatureToggleService
) : AbstractVerdikjedetest() {

    @Test
    fun `skal migrere fagsak selv om ikke alle barn i infotrygd ligger i PDL`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SKAL_MIGRERE_FOSTERBARN, any()) } returns true
        every { mockLocalDateService.now() } returns LocalDate.of(2021, 12, 12) andThen LocalDate.now()

        val barnPåInfotrygdSøknadScenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "1996-01-12",
                    fornavn = "Mor",
                    etternavn = "Søker"
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.of(2021, 11, 18).toString(),
                        fornavn = "Barn2",
                        etternavn = "Barnesen2"
                    )
                )
            )
        )

        val sakKlarForMigreringScenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "1996-01-12",
                    fornavn = "Mor",
                    etternavn = "Søker",
                    infotrygdSaker = InfotrygdSøkResponse(
                        bruker = listOf(
                            lagInfotrygdSak(
                                SatsService.sisteTilleggOrdinærSats.beløp.toDouble(),
                                barnPåInfotrygdSøknadScenario.barna.first().ident.toString(),
                                "OR",
                                "OS"
                            )
                        ),
                        barn = emptyList()
                    )
                ),
                barna = emptyList()
            )
        )

        val migreringsresponse = migreringService.migrer(sakKlarForMigreringScenario.søker.ident!!)

        val restFagsakEtterBehandlingAvsluttet =
            familieBaSakKlient().hentFagsak(fagsakId = migreringsresponse.fagsakId)
        generellAssertFagsak(
            restFagsak = restFagsakEtterBehandlingAvsluttet,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG
        )
    }
}
