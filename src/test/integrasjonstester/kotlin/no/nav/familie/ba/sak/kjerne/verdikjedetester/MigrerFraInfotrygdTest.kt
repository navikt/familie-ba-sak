package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.slot
import no.nav.familie.ba.sak.config.EfSakRestClientMock
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.KanIkkeMigrereException
import no.nav.familie.ba.sak.integrasjoner.infotrygd.MigreringService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.MigreringsfeilType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class MigrerFraInfotrygdTest(
    @Autowired private val migreringService: MigreringService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val kompetanseRepository: KompetanseRepository,
    @Autowired private val efSakRestClient: EfSakRestClient
) : AbstractVerdikjedetest() {

    @AfterEach
    fun ryddOpp() {
        EfSakRestClientMock.clearEfSakRestMocks(efSakRestClient)
    }

    @Test
    fun `skal migrere fagsak selv om ikke alle barn i infotrygd ligger i PDL`() {
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
                                HALV_BARNETRYGD_SATS_UNDER_6_ÅR * 2,
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

        val behandling = behandlingRepository.finnBehandling(migreringsresponse.behandlingId)
        assertThat(behandling.kategori).isEqualTo(BehandlingKategori.NASJONAL)
        assertThat(behandling.underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        assertThat(kompetanseRepository.finnFraBehandlingId(migreringsresponse.behandlingId)).isEmpty()
    }

    @Test
    fun `skal migrere delt bosted med 1 barn under 6 år`() {
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

        val behandling = behandlingRepository.finnBehandling(migreringsresponse.behandlingId)
        assertThat(behandling.kategori).isEqualTo(BehandlingKategori.NASJONAL)
        assertThat(behandling.underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        assertThat(kompetanseRepository.finnFraBehandlingId(migreringsresponse.behandlingId)).isEmpty()
    }

    @Test
    fun `skal migrere delt bosted med 1 barn over 6 år`() {
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

        val behandling = behandlingRepository.finnBehandling(migreringsresponse.behandlingId)
        assertThat(behandling.kategori).isEqualTo(BehandlingKategori.NASJONAL)
        assertThat(behandling.underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        assertThat(kompetanseRepository.finnFraBehandlingId(migreringsresponse.behandlingId)).isEmpty()
    }

    @Test
    fun `skal migrere delt bosted med 3 barn over 6 år`() {
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

        val behandling = behandlingRepository.finnBehandling(migreringsresponse.behandlingId)
        assertThat(behandling.kategori).isEqualTo(BehandlingKategori.NASJONAL)
        assertThat(behandling.underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        assertThat(kompetanseRepository.finnFraBehandlingId(migreringsresponse.behandlingId)).isEmpty()
    }

    @Test
    fun `skal migrere delt bosted med 2 barn over 6 år og 1 under`() {
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
        assertThat(kompetanseRepository.finnFraBehandlingId(migreringsresponse.behandlingId)).isEmpty()
    }

    @Test
    fun `skal feile migrere fordi beregnet beløp er ulikt beregnet beløp i ba-sak `() {
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

        val behandling = behandlingRepository.finnBehandling(migreringsresponse.behandlingId)
        assertThat(behandling.kategori).isEqualTo(BehandlingKategori.NASJONAL)
        assertThat(behandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
    }

    @Test
    fun `skal migrere utvidet barnetrygd for ett barn under 3 år`() {
        EfSakRestClientMock.clearEfSakRestMocks(efSakRestClient)
        val hentPerioderMedFullOvergangsstønadSlot = slot<String>()
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(capture(hentPerioderMedFullOvergangsstønadSlot)) } answers {
            PerioderOvergangsstønadResponse(
                perioder = listOf(
                    PeriodeOvergangsstønad(
                        personIdent = hentPerioderMedFullOvergangsstønadSlot.captured,
                        fomDato = LocalDate.now().minusYears(2),
                        datakilde = PeriodeOvergangsstønad.Datakilde.EF,
                        tomDato = LocalDate.now().plusMonths(3)
                    )
                )
            )
        }

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

        val behandling = behandlingRepository.finnBehandling(migreringsresponse.behandlingId)
        assertThat(behandling.kategori).isEqualTo(BehandlingKategori.NASJONAL)
        assertThat(behandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        assertThat(kompetanseRepository.finnFraBehandlingId(migreringsresponse.behandlingId)).isEmpty()
    }

    @Test
    fun `skal feile migrering av utvidet barnetrygd med delt bosted pga ulikt antall barn når et av barna er over 18`() {
        val barnPåInfotrygdSøknadScenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "1996-01-12",
                    fornavn = "Mor",
                    etternavn = "Søker"
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(19).toString(),
                        fornavn = "Barn1",
                        etternavn = "Barnesen1"
                    ),
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
        assertThat(exception.feiltype).isEqualTo(MigreringsfeilType.OPPGITT_ANTALL_BARN_ULIKT_ANTALL_BARNIDENTER)
    }

    @Test
    fun `skal feile migrering dersom ikke småbarnstillegg stemmer overens`() {
        val barnPåInfotrygdSøknadScenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "1996-01-12",
                    fornavn = "Mor",
                    etternavn = "Søker"
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(11).toString(),
                        fornavn = "Barn1",
                        etternavn = "Barnesen1"
                    ),
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
                                HALV_UTVIDET_BARNETRYGD_SATS + HALV_BARNETRYGD_SATS_OVER_6_ÅR * 2,
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
                                "UT",
                                "MD"
                            ).let {
                                it.copy(
                                    stønad = it.stønad!!.copy(
                                        delytelse = listOf(
                                            it.stønad!!.delytelse.first().copy(typeDelytelse = SMÅBARNSTILLEGG_KODE)
                                        )
                                    )
                                )
                            }
                        ),
                        barn = emptyList()
                    )
                ),
                barna = emptyList()
            )
        )

        val exception =
            assertThrows<KanIkkeMigrereException> { migreringService.migrer(sakKlarForMigreringScenario.søker.ident!!) }
        assertThat(exception.feiltype).isEqualTo(MigreringsfeilType.SMÅBARNSTILLEGG_INFOTRYGD_IKKE_BA_SAK)
    }

    @Test
    fun `skal migrere EØS ordinær primærland med 1 barn under 6`() {
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
                                HALV_BARNETRYGD_SATS_UNDER_6_ÅR * 2,
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
                                "OR",
                                "EU"
                            )
                        ),
                        barn = emptyList()
                    )
                ),
                barna = emptyList()
            )
        )

        kjørOgAssertEØS(sakKlarForMigreringScenario.søker.ident!!, BehandlingUnderkategori.ORDINÆR)
    }

    @Test
    fun `skal migrere EØS ordinær primærland med 2 barn over 6 `() {
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
                        fornavn = "barn 1",
                        etternavn = "Barnesen2"
                    ),
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(8).toString(),
                        fornavn = "barn 2",
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
                                HALV_BARNETRYGD_SATS_OVER_6_ÅR * 2 * 2,
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
                                "OR",
                                "EU"
                            )
                        ),
                        barn = emptyList()
                    )
                ),
                barna = emptyList()
            )
        )

        kjørOgAssertEØS(sakKlarForMigreringScenario.søker.ident!!, BehandlingUnderkategori.ORDINÆR)
    }

    @Test
    fun `skal migrere EØS utvidet primærland`() {
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
                                HALV_UTVIDET_BARNETRYGD_SATS * 2 + HALV_BARNETRYGD_SATS_OVER_6_ÅR * 2,
                                barnPåInfotrygdSøknadScenario.barna.map { it.ident.toString() },
                                "UT",
                                "EU"
                            )
                        ),
                        barn = emptyList()
                    )
                ),
                barna = emptyList()
            )
        )

        kjørOgAssertEØS(sakKlarForMigreringScenario.søker.ident!!, BehandlingUnderkategori.UTVIDET)
    }

    private fun kjørOgAssertEØS(
        ident: String,
        behandlingUnderkategori: BehandlingUnderkategori
    ) {
        val migreringsresponse = migreringService.migrer(ident)
        val restFagsakEtterBehandlingAvsluttet =
            familieBaSakKlient().hentFagsak(fagsakId = migreringsresponse.fagsakId)

        val behandling = behandlingRepository.finnBehandling(migreringsresponse.behandlingId)
        assertThat(behandling.kategori).isEqualTo(BehandlingKategori.EØS)
        assertThat(behandling.underkategori).isEqualTo(behandlingUnderkategori)

        generellAssertFagsak(
            restFagsak = restFagsakEtterBehandlingAvsluttet,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG
        )

        assertThat(kompetanseRepository.finnFraBehandlingId(migreringsresponse.behandlingId)).hasSize(1)
            .extracting("resultat").contains(KompetanseResultat.NORGE_ER_PRIMÆRLAND)
    }

    companion object {
        const val HALV_BARNETRYGD_SATS_UNDER_6_ÅR = 838.0
        const val HALV_BARNETRYGD_SATS_OVER_6_ÅR = 527.0
        const val HALV_UTVIDET_BARNETRYGD_SATS = 527.0
        const val SMÅBARNSTILLEGG = 660.0
        const val SMÅBARNSTILLEGG_KODE = "SM"
    }
}
