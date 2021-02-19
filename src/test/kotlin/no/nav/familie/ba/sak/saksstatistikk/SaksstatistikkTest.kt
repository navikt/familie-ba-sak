package no.nav.familie.ba.sak.saksstatistikk

import io.mockk.every
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakController
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRequest
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringType.BEHANDLING
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringType.SAK
import no.nav.familie.ba.sak.vedtak.producer.MockKafkaProducer.Companion.sendteMeldinger
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen", "mock-pdl", "mock-infotrygd-barnetrygd")
@Tag("integration")
@TestInstance(Lifecycle.PER_CLASS)
class SaksstatistikkTest(
    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val fagsakController: FagsakController,

    @Autowired
    private val mockPersonopplysningerService: PersonopplysningerService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val mockIntegrasjonClient: IntegrasjonClient,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,
    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,
    @Autowired
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
    @Autowired val saksstatistikkScheduler: SaksstatistikkScheduler
) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    @Tag("integration")
    fun `Skal lagre saksstatistikk sak til repository og sende meldinger`() {
        val fnr = "12345678910"

        every {
            mockPersonopplysningerService.hentIdenter(Ident(fnr))
        } returns listOf(IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"))


        val fagsakId = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr)).body!!.data!!.id

        val mellomlagredeStatistikkHendelser = saksstatistikkMellomlagringRepository.findByTypeAndTypeId(SAK, fagsakId)

        assertThat(mellomlagredeStatistikkHendelser).hasSize(1)
        assertThat(mellomlagredeStatistikkHendelser.first().type).isEqualTo(SAK)
        println(mellomlagredeStatistikkHendelser.first().json)
        assertThat(mellomlagredeStatistikkHendelser.first().konvertertTidspunkt).isNull()
        assertThat(mellomlagredeStatistikkHendelser.first().sendtTidspunkt).isNull()
        assertThat(mellomlagredeStatistikkHendelser.first().kontraktVersjon).isEqualTo(hentPropertyFraMaven("familie.kontrakter.saksstatistikk"))


        val lagretJsonSomSakDVH: SakDVH =
            objectMapper.readValue(mellomlagredeStatistikkHendelser.first().json, SakDVH::class.java)

        saksstatistikkScheduler.sendSaksstatistikk()
        val oppdatertMellomlagretSaksstatistikkHendelse =
            saksstatistikkMellomlagringRepository.findByIdOrNull(mellomlagredeStatistikkHendelser.first().id)

        assertThat(oppdatertMellomlagretSaksstatistikkHendelse!!.sendtTidspunkt).isNotNull
        assertThat(sendteMeldinger["sak-$fagsakId"] as SakDVH).isEqualTo(lagretJsonSomSakDVH)
    }

    @Test
    @Tag("integration")
    fun `Skal lagre saksstatistikk behandling til repository og sende meldinger`() {
        val fnr = "12345678910"

        every {
            mockPersonopplysningerService.hentIdenter(Ident(fnr))
        } returns listOf(IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"))


        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr, false)
        val behandling = behandlingService.opprettBehandling(
            nyOrdinærBehandling(
                fnr
            )
        )

        val mellomlagretBehandling = saksstatistikkMellomlagringRepository.findByTypeAndTypeId(BEHANDLING, behandling.id)
        assertThat(mellomlagretBehandling).hasSize(1)
        assertThat(mellomlagretBehandling.first().konvertertTidspunkt).isNull()
        assertThat(mellomlagretBehandling.first().sendtTidspunkt).isNull()
        assertThat(mellomlagretBehandling.first().kontraktVersjon).isEqualTo(hentPropertyFraMaven("familie.kontrakter.saksstatistikk"))

        val lagretJsonSomSakDVH: BehandlingDVH =
            objectMapper.readValue(mellomlagretBehandling.first().json, BehandlingDVH::class.java)

        saksstatistikkScheduler.sendSaksstatistikk()
        val oppdatertMellomlagretSaksstatistikkHendelse =
            saksstatistikkMellomlagringRepository.findByIdOrNull(mellomlagretBehandling.first().id)

        assertThat(oppdatertMellomlagretSaksstatistikkHendelse!!.sendtTidspunkt).isNotNull
        assertThat(sendteMeldinger["behandling-${behandling.id}"] as BehandlingDVH).isEqualTo(lagretJsonSomSakDVH)
    }


}