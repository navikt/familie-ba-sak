package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.KanIkkeMigrereException
import no.nav.familie.ba.sak.integrasjoner.infotrygd.MigreringService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.MigreringsfeilType
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class MigrerFraInfotrygdTest(
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
                        fødselsdato = LocalDate.now().minusYears(1).toString(),
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
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
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

    @Test
    fun `skal migrere delt bosted med 1 barn under 6 år`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SKAL_MIGRERE_FOSTERBARN, any()) } returns true
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.SKAL_MIGRERE_ORDINÆR_DELT_BOSTED,
                any()
            )
        } returns true
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
                        fødselsdato = LocalDate.now().minusYears(1).toString(),
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
                                HALV_BARNETRYGD_SATS_UNDER_6_ÅR,
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
                                "OR",
                                "MD"
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

    @Test
    fun `skal migrere delt bosted med 1 barn over 6 år`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SKAL_MIGRERE_FOSTERBARN, any()) } returns true
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.SKAL_MIGRERE_ORDINÆR_DELT_BOSTED,
                any()
            )
        } returns true
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
                        fødselsdato = LocalDate.now().minusYears(8).toString(),
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
                                HALV_BARNETRYGD_SATS_OVER_6_ÅR,
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
                                "OR",
                                "MD"
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

    @Test
    fun `skal migrere delt bosted med 3 barn over 6 år`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SKAL_MIGRERE_FOSTERBARN, any()) } returns true
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.SKAL_MIGRERE_ORDINÆR_DELT_BOSTED,
                any()
            )
        } returns true
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
                        fødselsdato = LocalDate.now().minusYears(8).toString(),
                        fornavn = "Trilling1",
                        etternavn = "Barnesen2"
                    ),
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(8).toString(),
                        fornavn = "Trilling2",
                        etternavn = "Barnesen2"
                    ),
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(8).toString(),
                        fornavn = "Trilling3",
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
                                HALV_BARNETRYGD_SATS_OVER_6_ÅR * 3,
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
                                "OR",
                                "MD"
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

    @Test
    fun `skal migrere delt bosted med 2 barn over 6 år og 1 under`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SKAL_MIGRERE_FOSTERBARN, any()) } returns true
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.SKAL_MIGRERE_ORDINÆR_DELT_BOSTED,
                any()
            )
        } returns true
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
                        fødselsdato = LocalDate.now().minusYears(2).toString(),
                        fornavn = "Trilling1",
                        etternavn = "Barnesen2"
                    ),
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(8).toString(),
                        fornavn = "Trilling2",
                        etternavn = "Barnesen2"
                    ),
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(8).toString(),
                        fornavn = "Trilling3",
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
                                HALV_BARNETRYGD_SATS_UNDER_6_ÅR + (HALV_BARNETRYGD_SATS_OVER_6_ÅR * 2),
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
                                "OR",
                                "MD"
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

    @Test
    fun `skal feile migrere fordi beregnet beløp er ulikt beregnet beløp i ba-sak `() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SKAL_MIGRERE_FOSTERBARN, any()) } returns true
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.SKAL_MIGRERE_ORDINÆR_DELT_BOSTED,
                any()
            )
        } returns true
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
                        fødselsdato = LocalDate.now().minusYears(7).toString(),
                        fornavn = "Barn",
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
                                HALV_BARNETRYGD_SATS_UNDER_6_ÅR,
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
                                "OR",
                                "MD"
                            )
                        ),
                        barn = emptyList()
                    )
                ),
                barna = emptyList()
            )
        )

        val exception =
            assertThrows<KanIkkeMigrereException> { migreringService.migrer(sakKlarForMigreringScenario.søker.ident!!) }
        assertThat(exception.feiltype).isEqualTo(MigreringsfeilType.BEREGNET_DELT_BOSTED_BELØP_ULIKT_BELØP_FRA_INFOTRYGD)
    }

    @Test
    fun `skal migrere utvidet barnetrygd med delt bosted for ett barn over 6 år`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SKAL_MIGRERE_FOSTERBARN, any()) } returns true
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.SKAL_MIGRERE_UTVIDET_DELT_BOSTED,
                any()
            )
        } returns true
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
                        fødselsdato = LocalDate.now().minusYears(8).toString(),
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
                                HALV_UTVIDET_BARNETRYGD_SATS + HALV_BARNETRYGD_SATS_OVER_6_ÅR,
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
                                "UT",
                                "MD"
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

    @Test
    fun `skal migrere utvidet barnetrygd for ett barn under 3 år`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SKAL_MIGRERE_FOSTERBARN, any()) } returns true
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.SKAL_MIGRERE_UTVIDET_DELT_BOSTED,
                any()
            )
        } returns true
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
                        fødselsdato = LocalDate.now().minusYears(1).toString(),
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
                            lagInfotrygdSakMedSmåbarnstillegg(
                                HALV_UTVIDET_BARNETRYGD_SATS * 2 + HALV_BARNETRYGD_SATS_UNDER_6_ÅR * 2,
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() }
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

    @Test
    fun `skal feile migrering av utvidet barnetrygd med delt bosted fordi det er mer enn ett barn`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SKAL_MIGRERE_FOSTERBARN, any()) } returns true
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.SKAL_MIGRERE_UTVIDET_DELT_BOSTED,
                any()
            )
        } returns true
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
                        fødselsdato = LocalDate.now().minusYears(8).toString(),
                        fornavn = "Tvilling1",
                        etternavn = "Barnesen1"
                    ),
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(8).toString(),
                        fornavn = "Tvilling2",
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
                                HALV_UTVIDET_BARNETRYGD_SATS + HALV_BARNETRYGD_SATS_OVER_6_ÅR * 2,
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
                                "UT",
                                "MD"
                            )
                        ),
                        barn = emptyList()
                    )
                ),
                barna = emptyList()
            )
        )

        val exception =
            assertThrows<KanIkkeMigrereException> { migreringService.migrer(sakKlarForMigreringScenario.søker.ident!!) }
        assertThat(exception.feiltype).isEqualTo(MigreringsfeilType.MER_ENN_ETT_BARN_PÅ_SAK_AV_TYPE_UT_MD)
    }

    companion object {
        const val HALV_BARNETRYGD_SATS_UNDER_6_ÅR = 838.0
        const val HALV_BARNETRYGD_SATS_OVER_6_ÅR = 527.0
        const val HALV_UTVIDET_BARNETRYGD_SATS = 527.0
        const val SMÅBARNSTILLEGG = 660.0
    }
}
