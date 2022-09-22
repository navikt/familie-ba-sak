package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.AbstractVerdikjedetest
import no.nav.familie.ba.sak.kjerne.verdikjedetester.fullførBehandlingFraVilkårsvurderingAlleVilkårOppfylt
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

internal class RettOffsetIAndelTilkjentYtelseTaskTest(
    @Autowired private val task: RettOffsetIAndelTilkjentYtelseTask,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val efSakRestClient: EfSakRestClient,
    @Autowired private val transactionManager: PlatformTransactionManager
) : AbstractVerdikjedetest() {

    @Test
    fun køyrTest() {
        val barnFødselsdato = LocalDate.now().minusYears(2)

        val personScenario = lagScenario(barnFødselsdato)
        val fagsak = familieBaSakKlient().opprettFagsak(søkersIdent = personScenario.søker.ident!!).data!!
        fullførBehandling(
            fagsak = fagsak,
            personScenario = personScenario,
            barnFødselsdato = barnFødselsdato
        )

        val behandling2 = fullførRevurderingMedOvergangstonad(
            fagsak = fagsak,
            personScenario = personScenario,
            barnFødselsdato = barnFødselsdato
        ).id

        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling2)
            .also { it[5].periodeOffset = 5 }
            .also { andelTilkjentYtelseRepository.save(it[5]) }

        assertThat(
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling2)
                .map { it.periodeOffset }
        ).isEqualTo(listOf(5L, 0L, 1L, 2L, 3L, 5L))

        val input = RettOffsetIAndelTilkjentYtelseDto(kunSiste = true, simuler = false)

        TransactionTemplate(transactionManager).execute {
            task.doTask(Task(type = "", payload = objectMapper.writeValueAsString(input)))
        }

        assertThat(
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling2)
                .map { it.periodeOffset }
        ).isEqualTo(listOf(5L, 0L, 1L, 2L, 3L, 4L))
    }

    fun lagScenario(barnFødselsdato: LocalDate): RestScenario = mockServerKlient().lagScenario(
        RestScenario(
            søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
            barna = listOf(
                RestScenarioPerson(
                    fødselsdato = barnFødselsdato.toString(),
                    fornavn = "Barn",
                    etternavn = "Barnesen",
                    bostedsadresser = emptyList()
                )
            )
        )
    )

    fun fullførBehandling(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate
    ): Behandling {
        val behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = emptyList()
        )

        val restBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = fagsak.søkerFødselsnummer,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                behandlingType = behandlingType
            )
        val behandling = behandlingHentOgPersisterService.hent(restBehandling.data!!.behandlingId)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = fagsak.søkerFødselsnummer,
                    barnasIdenter = personScenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.UTVIDET
                ),
                bekreftEndringerViaFrontend = false
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = behandling.id,
                restRegistrerSøknad = restRegistrerSøknad
            )

        return fullførBehandlingFraVilkårsvurderingAlleVilkårOppfylt(
            restUtvidetBehandling = restUtvidetBehandling.data!!,
            personScenario = personScenario,
            fagsak = fagsak,
            familieBaSakKlient = familieBaSakKlient(),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            lagToken = ::token
        )
    }

    fun fullførRevurderingMedOvergangstonad(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate
    ): Behandling {
        val behandlingType = BehandlingType.REVURDERING
        val behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = listOf(
                PeriodeOvergangsstønad(
                    personIdent = personScenario.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )

        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = fagsak.søkerFødselsnummer,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak
            )

        return fullførBehandlingFraVilkårsvurderingAlleVilkårOppfylt(
            restUtvidetBehandling = restUtvidetBehandling.data!!,
            personScenario = personScenario,
            fagsak = fagsak,
            familieBaSakKlient = familieBaSakKlient(),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            lagToken = ::token
        )
    }
}
