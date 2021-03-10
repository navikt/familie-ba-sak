package no.nav.familie.ba.sak.infotrygd

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.steg.BehandlingSteg
import no.nav.familie.ba.sak.behandling.steg.RegistrerPersongrunnlag
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.skyggesak.SkyggesakService
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks

@ExtendWith(MockKExtension::class)
class MigreringServiceTest {

    lateinit var steg: List<BehandlingSteg<*>>
    @MockK
    lateinit var loggService: LoggService
    @MockK(relaxed = true)
    lateinit var behandlingService: BehandlingService
    @MockK
    lateinit var søknadGrunnlagService: SøknadGrunnlagService
    @MockK(relaxed = true)
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
    @MockK
    lateinit var envService: EnvService
    @MockK
    lateinit var skyggesakService: SkyggesakService
    @MockK(relaxed = true)
    lateinit var tilgangService: TilgangService

    @MockK(relaxed = true)
    lateinit var totrinnskontrollService: TotrinnskontrollService
    @MockK(relaxed = true)
    lateinit var fagsakService: FagsakService
    @MockK(relaxed = true)
    lateinit var vilkårsvurderingService: VilkårsvurderingService
    @MockK
    lateinit var infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient
    @MockK
    lateinit var vedtakService: VedtakService
    @MockK
    lateinit var taskRepository: TaskRepository
    @MockK
    lateinit var vilkårService: VilkårService
    @MockK(relaxed = true)
    lateinit var persongrunnlagService: PersongrunnlagService

    lateinit var stegService: StegService
    lateinit var migreringService: MigreringService

    val infotrygdSak = Sak("12345678910", status = "FB", stønadList = listOf(Stønad(1)))

    @Test
    fun migrer() {

        steg = listOf(RegistrerPersongrunnlag(behandlingService, persongrunnlagService, vilkårService))

        stegService = StegService(steg,
                                  loggService,
                                  fagsakService,
                                  behandlingService,
                                  søknadGrunnlagService,
                                  personopplysningGrunnlagRepository,
                                  envService,
                                  skyggesakService,
                                  tilgangService)

        migreringService = MigreringService(infotrygdBarnetrygdClient,
                                            fagsakService,
                                            stegService,
                                            vedtakService,
                                            taskRepository,
                                            vilkårService,
                                            vilkårsvurderingService,
                                            totrinnskontrollService,
                                            loggService)

        every { infotrygdBarnetrygdClient.hentSaker(any()) } returns InfotrygdSøkResponse(listOf(infotrygdSak), listOf())

        every { behandlingService.opprettBehandling(any()) } returns lagBehandling(førsteSteg = StegType.REGISTRERE_PERSONGRUNNLAG)

        migreringService.migrer("12345678910")
    }




}