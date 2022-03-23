package no.nav.familie.ba.sak.kjerne.logg

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTestDev
import no.nav.familie.ba.sak.config.mockHentPersoninfoForMedIdenter
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class LoggServiceTest(
    @Autowired
    private val loggService: LoggService,

    @Autowired
    private val stegService: StegService,

    @Autowired
    private val mockPersonopplysningerService: PersonopplysningerService,
) : AbstractSpringIntegrationTestDev() {

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
        assertEquals(2, loggForBehandling.size)

        val loggForBehandling1 = loggService.hentLoggForBehandling(behandling1.id)
        assertEquals(1, loggForBehandling1.size)
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
        assertEquals(2, loggForBehandling.size)
        assertTrue(loggForBehandling.any { it.type == LoggType.LIVSHENDELSE })
        assertTrue(loggForBehandling.any { it.type == LoggType.BEHANDLING_OPPRETTET })
        assertTrue(loggForBehandling.none { it.rolle != BehandlerRolle.SYSTEM })
    }

    @Test
    fun `Skal lage nye vilkårslogger og endringer`() {
        val behandling = lagBehandling()
        val vilkårsvurderingLogg = loggService.opprettVilkårsvurderingLogg(
            behandling = behandling,
            forrigeBehandlingsresultat = behandling.resultat,
            nyttBehandlingsresultat = Behandlingsresultat.INNVILGET
        )

        assertNotNull(vilkårsvurderingLogg)
        assertEquals("Vilkårsvurdering gjennomført", vilkårsvurderingLogg!!.tittel)

        behandling.resultat = Behandlingsresultat.INNVILGET
        val nyVilkårsvurderingLogg =
            loggService.opprettVilkårsvurderingLogg(
                behandling = behandling,
                forrigeBehandlingsresultat = behandling.resultat,
                nyttBehandlingsresultat = Behandlingsresultat.AVSLÅTT
            )

        assertNotNull(nyVilkårsvurderingLogg)
        assertEquals("Vilkårsvurdering endret", nyVilkårsvurderingLogg!!.tittel)

        val logger = loggService.hentLoggForBehandling(behandlingId = behandling.id)
        assertEquals(2, logger.size)
    }

    @Test
    fun `Skal ikke logge ved uforandret behandlingsresultat`() {
        val vilkårsvurderingLogg = loggService.opprettVilkårsvurderingLogg(
            behandling = lagBehandling(),
            forrigeBehandlingsresultat = Behandlingsresultat.FORTSATT_INNVILGET,
            nyttBehandlingsresultat = Behandlingsresultat.FORTSATT_INNVILGET
        )

        assertNull(vilkårsvurderingLogg)
    }

    @Test
    fun `Skal lage noen logginnslag på helmanuell migrering`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING
        )
        loggService.opprettBehandlingLogg(behandling)
        loggService.opprettVilkårsvurderingLogg(behandling, behandling.resultat, Behandlingsresultat.INNVILGET)
        loggService.opprettSendTilBeslutterLogg(behandling)
        loggService.opprettBeslutningOmVedtakLogg(behandling, Beslutning.GODKJENT, "begrunnelse")
        loggService.opprettFerdigstillBehandling(behandling)

        val logger = loggService.hentLoggForBehandling(behandling.id)
        assertEquals(5, logger.size)
        assertTrue {
            logger.any {
                it.type == LoggType.BEHANDLING_OPPRETTET && it.tittel == "Migrering fra infotrygd opprettet"
            }
        }
        assertTrue {
            logger.any {
                it.type == LoggType.VILKÅRSVURDERING &&
                    it.tittel == "Vilkårsvurdering gjennomført" && it.tekst == "Resultat ble innvilget"
            }
        }
        assertTrue {
            logger.any {
                it.type == LoggType.SEND_TIL_SYSTEM &&
                    it.tittel == "Sendt til system"
            }
        }
        assertTrue {
            logger.any {
                it.type == LoggType.MIGRERING_BEKREFTET &&
                    it.tittel == "Migrering bekreftet" &&
                    it.opprettetAv == SikkerhetContext.SYSTEM_NAVN
            }
        }
        assertTrue {
            logger.any {
                it.type == LoggType.FERDIGSTILLE_BEHANDLING &&
                    it.tittel == "Ferdigstilt behandling"
            }
        }
    }
}
