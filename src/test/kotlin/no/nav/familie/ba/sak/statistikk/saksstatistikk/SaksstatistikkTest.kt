package no.nav.familie.ba.sak.statistikk.saksstatistikk

import io.mockk.every
import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakController
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.statistikk.producer.MockKafkaProducer
import no.nav.familie.ba.sak.statistikk.producer.MockKafkaProducer.Companion.sendteMeldinger
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType.BEHANDLING
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType.SAK
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

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
        private val databaseCleanupService: DatabaseCleanupService,
        @Autowired
        private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
) : AbstractSpringIntegrationTest() {

    private lateinit var saksstatistikkScheduler: SaksstatistikkScheduler

    @BeforeAll
    fun init() {
        val kafkaProducer = MockKafkaProducer(saksstatistikkMellomlagringRepository)
        saksstatistikkScheduler = SaksstatistikkScheduler(saksstatistikkMellomlagringRepository, kafkaProducer)
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
                sakstatistikkObjectMapper.readValue(mellomlagredeStatistikkHendelser.first().json, SakDVH::class.java)

        saksstatistikkScheduler.sendSaksstatistikk()
        val oppdatertMellomlagretSaksstatistikkHendelse =
                saksstatistikkMellomlagringRepository.findByIdOrNull(mellomlagredeStatistikkHendelser.first().id)

        assertThat(oppdatertMellomlagretSaksstatistikkHendelse!!.sendtTidspunkt).isNotNull
        assertThat(sendteMeldinger["sak-$fagsakId"] as SakDVH).isEqualTo(lagretJsonSomSakDVH)
    }

    @Test
    @Tag("integration")
    fun `Skal utføre rollback på sak og saksstatistikk ved feil`() {
        val fnr = "12345678910"

        every {
            mockPersonopplysningerService.hentIdenter(Ident(fnr))
        } throws RuntimeException("Testen skal feile")


        assertThatThrownBy {
            fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        }.hasMessage("Testen skal feile")

        val mellomlagredeStatistikkHendelser = saksstatistikkMellomlagringRepository.finnMeldingerKlarForSending()

        assertThat(mellomlagredeStatistikkHendelser).hasSize(0)
    }

    @Test
    @Tag("integration")
    fun `Skal lagre saksstatistikk behandling til repository og sende meldinger`() {
        val fnr = "12345678910"

        every {
            mockPersonopplysningerService.hentIdenter(Ident(fnr))
        } returns listOf(IdentInformasjon(ident = fnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"))


        fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr, false)
        val behandling = behandlingService.opprettBehandling(
                nyOrdinærBehandling(
                        fnr
                )
        )

        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, BehandlingStatus.AVSLUTTET)

        val mellomlagretBehandling = saksstatistikkMellomlagringRepository.findByTypeAndTypeId(BEHANDLING, behandling.id)
        assertThat(mellomlagretBehandling).hasSize(2)
        assertThat(mellomlagretBehandling.first().konvertertTidspunkt).isNull()
        assertThat(mellomlagretBehandling.first().sendtTidspunkt).isNull()
        assertThat(mellomlagretBehandling.first().kontraktVersjon).isEqualTo(hentPropertyFraMaven("familie.kontrakter.saksstatistikk"))
        assertThat(mellomlagretBehandling.first().jsonToBehandlingDVH().behandlingStatus).isEqualTo("UTREDES")
        assertThat(mellomlagretBehandling.last().jsonToBehandlingDVH().behandlingStatus).isEqualTo("AVSLUTTET")

        val lagretJsonSomSakDVH: BehandlingDVH =
                sakstatistikkObjectMapper.readValue(mellomlagretBehandling.last().json, BehandlingDVH::class.java)

        saksstatistikkScheduler.sendSaksstatistikk()
        val oppdatertMellomlagretSaksstatistikkHendelse =
                saksstatistikkMellomlagringRepository.findByIdOrNull(mellomlagretBehandling.first().id)

        assertThat(oppdatertMellomlagretSaksstatistikkHendelse!!.sendtTidspunkt).isNotNull
        assertThat(sendteMeldinger["behandling-${behandling.id}"] as BehandlingDVH).isEqualTo(lagretJsonSomSakDVH)
    }


}