package no.nav.familie.ba.sak.kjerne.logg

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTestDev
import no.nav.familie.ba.sak.config.AbstractTestWithJdbcTables
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.config.mockHentPersoninfoForMedIdenter
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource

@SpringBootTest
@ActiveProfiles("dev", "mock-pdl", "mock-infotrygd-barnetrygd")
@TestInstance(Lifecycle.PER_CLASS)
class LoggServiceTest(
        @Autowired
        private val loggService: LoggService,

        @Autowired
        private val stegService: StegService,

        @Autowired
        private val mockPersonopplysningerService: PersonopplysningerService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
) : AbstractTestWithJdbcTables() {

    @BeforeAll
    fun init(@Autowired dataSource: DataSource) {
        initJdbcTables(dataSource)
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal lage noen logginnslag på forskjellige behandlinger og hente dem fra databasen`() {
        val behandling: Behandling = lagBehandling()
        val behandling1: Behandling = lagBehandling()

        val logg1 = Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "Førstegangsbehandling opprettet",
                rolle = BehandlerRolle.SYSTEM,
                tekst = ""
        )
        loggService.lagre(logg1)

        val logg2 = Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "Revurdering opprettet",
                rolle = BehandlerRolle.SYSTEM,
                tekst = ""
        )
        loggService.lagre(logg2)

        val logg3 = Logg(
                behandlingId = behandling1.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "Førstegangsbehandling opprettet",
                rolle = BehandlerRolle.SYSTEM,
                tekst = ""
        )
        loggService.lagre(logg3)

        val loggForBehandling = loggService.hentLoggForBehandling(behandling.id)
        Assertions.assertEquals(2, loggForBehandling.size)

        val loggForBehandling1 = loggService.hentLoggForBehandling(behandling1.id)
        Assertions.assertEquals(1, loggForBehandling1.size)
    }

    @Test
    fun `Skal lage logginnslag ved stegflyt for automatisk behandling`() {
        val morsIdent = randomFnr()
        val barnetsIdent = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, morsIdent, barnetsIdent)

        val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(
                NyBehandlingHendelse(
                        morsIdent = morsIdent,
                        barnasIdenter = listOf(barnetsIdent)
                )
        )

        val loggForBehandling = loggService.hentLoggForBehandling(behandlingId = behandling.id)
        Assertions.assertEquals(2, loggForBehandling.size)
        Assertions.assertTrue(loggForBehandling.any { it.type == LoggType.FØDSELSHENDELSE })
        Assertions.assertTrue(loggForBehandling.any { it.type == LoggType.BEHANDLING_OPPRETTET })
        Assertions.assertTrue(loggForBehandling.none { it.rolle != BehandlerRolle.SYSTEM })
    }

    @Test
    fun `Skal lage nye vilkårslogger og endringer`() {
        val behandling = lagBehandling()
        val vilkårsvurderingLogg = loggService.opprettVilkårsvurderingLogg(
                behandling = behandling,
                forrigeBehandlingResultat = behandling.resultat,
                nyttBehandlingResultat = BehandlingResultat.INNVILGET
        )

        Assertions.assertNotNull(vilkårsvurderingLogg)
        Assertions.assertEquals("Vilkårsvurdering gjennomført", vilkårsvurderingLogg!!.tittel)


        behandling.resultat = BehandlingResultat.INNVILGET
        val nyVilkårsvurderingLogg =
                loggService.opprettVilkårsvurderingLogg(
                        behandling = behandling,
                        forrigeBehandlingResultat = behandling.resultat,
                        nyttBehandlingResultat = BehandlingResultat.AVSLÅTT
                )

        Assertions.assertNotNull(nyVilkårsvurderingLogg)
        Assertions.assertEquals("Vilkårsvurdering endret", nyVilkårsvurderingLogg!!.tittel)

        val logger = loggService.hentLoggForBehandling(behandlingId = behandling.id)
        Assertions.assertEquals(2, logger.size)
    }

    @Test
    fun `Skal ikke logge ved uforandret behandlingsresultat`() {
        val vilkårsvurderingLogg = loggService.opprettVilkårsvurderingLogg(
                behandling = lagBehandling(),
                forrigeBehandlingResultat = BehandlingResultat.FORTSATT_INNVILGET,
                nyttBehandlingResultat = BehandlingResultat.FORTSATT_INNVILGET
        )

        Assertions.assertNull(vilkårsvurderingLogg)
    }
}