package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFiltreringResultat
import no.nav.familie.ba.sak.datagenerator.lagFiltreringsreglerFaktaSøknad
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FILTRERINGSREGLER_SØKNAD
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsregelEvaluator
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FiltreringsreglerSøknadServiceTest {
    private val filtreringsregelEvaluator = mockk<FiltreringsregelEvaluator>()
    private val filtreringResultatRepository = mockk<FiltreringResultatRepository>()
    private val faktaOppretter = mockk<FaktaOppretter>()
    private val metrikker = mockk<Metrikker>()

    private val filtreringsreglerSøknadService =
        FiltreringsreglerSøknadService(
            filtreringResultatRepository = filtreringResultatRepository,
            filtreringsregelEvaluator = filtreringsregelEvaluator,
            faktaOppretter = faktaOppretter,
            metrikker = metrikker,
        )

    @BeforeEach
    fun setup() {
        every { metrikker.oppdaterMetrikker(any()) } just Runs
        every { filtreringResultatRepository.saveAll(any<List<FiltreringResultat>>()) } answers { firstArg() }
    }

    @Test
    fun `skal evaluere filtreringsreglene med fakta fra FaktaOppretter og lagre resultatene`() {
        // Arrange
        val behandling = lagBehandling()
        val data = FiltrerAutomatiskBehandlingData(randomFnr(), listOf(randomFnr()))
        val fakta = lagFiltreringsreglerFaktaSøknad()
        val evaluering =
            Evaluering(
                resultat = Resultat.OPPFYLT,
                evalueringÅrsaker = emptyList(),
                begrunnelse = "",
                identifikator = FILTRERINGSREGLER_SØKNAD.first().identifikator.name,
            )
        val lagredeResultaterSlot = slot<List<FiltreringResultat>>()

        every { faktaOppretter.opprettFakta(data, behandling) } returns fakta
        every { filtreringsregelEvaluator.evaluerFiltreringsregler(FILTRERINGSREGLER_SØKNAD, fakta) } returns listOf(evaluering)
        every { filtreringResultatRepository.saveAll(capture(lagredeResultaterSlot)) } answers { firstArg() }

        // Act
        val resultater =
            filtreringsreglerSøknadService.kjørFiltreringsregler(
                filtrerAutomatiskBehandlingData = data,
                behandling = behandling,
            )

        // Assert
        assertThat(resultater).hasSize(1)
        assertThat(lagredeResultaterSlot.captured).hasSize(1)
        assertThat(lagredeResultaterSlot.captured.single().behandlingId).isEqualTo(behandling.id)
    }

    @Test
    fun `skal nummerere begrunnelsene til alle filtreringsregler som ikke er oppfylt`() {
        // Arrange
        val behandlingId = 1L
        every { filtreringResultatRepository.finnFiltreringResultater(behandlingId) } returns
            listOf(
                lagFiltreringResultat(behandlingId = behandlingId, resultat = Resultat.OPPFYLT),
                lagFiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.Identifikator.SØKER_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
                    resultat = Resultat.IKKE_OPPFYLT,
                    begrunnelse = "Søker har EØS-barnetrygd",
                ),
                lagFiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.Identifikator.SØKNADEN_INNEHOLDER_IKKE_VEDLEGG,
                    resultat = Resultat.IKKE_OPPFYLT,
                    begrunnelse = "Søknaden inneholder vedlegg",
                ),
                lagFiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.Identifikator.SØKER_HAR_IKKE_VERGE,
                    resultat = Resultat.IKKE_VURDERT,
                    begrunnelse = "Ikke vurdert",
                ),
            )

        // Act
        val begrunnelse = filtreringsreglerSøknadService.hentBegrunnelseForStoppIFiltreringsreglene(behandlingId)

        // Assert
        assertThat(begrunnelse).isEqualTo("1) Søker har EØS-barnetrygd 2) Søknaden inneholder vedlegg")
    }

    @Test
    fun `skal utelate filtreringsregler om adressebeskyttelse fra begrunnelsen`() {
        // Arrange
        val behandlingId = 1L
        every { filtreringResultatRepository.finnFiltreringResultater(behandlingId) } returns
            listOf(
                lagFiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.Identifikator.SØKER_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19,
                    resultat = Resultat.IKKE_OPPFYLT,
                    begrunnelse = "Søker har adressebeskyttelse gradering 6 eller 19",
                ),
                lagFiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.Identifikator.BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19,
                    resultat = Resultat.IKKE_OPPFYLT,
                    begrunnelse = "Barn har adressebeskyttelse gradering 6 eller 19",
                ),
                lagFiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.Identifikator.SØKNADEN_INNEHOLDER_IKKE_VEDLEGG,
                    resultat = Resultat.IKKE_OPPFYLT,
                    begrunnelse = "Søknaden inneholder vedlegg",
                ),
            )

        // Act
        val begrunnelse = filtreringsreglerSøknadService.hentBegrunnelseForStoppIFiltreringsreglene(behandlingId)

        // Assert
        assertThat(begrunnelse).isEqualTo("Søknaden inneholder vedlegg")
    }

    @Test
    fun `skal ikke gi begrunnelse når bare filtreringsregler om adressebeskyttelse ikke er oppfylt`() {
        // Arrange
        val behandlingId = 1L
        every { filtreringResultatRepository.finnFiltreringResultater(behandlingId) } returns
            listOf(
                lagFiltreringResultat(behandlingId = behandlingId, resultat = Resultat.OPPFYLT),
                lagFiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.Identifikator.BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19,
                    resultat = Resultat.IKKE_OPPFYLT,
                    begrunnelse = "Barn har adressebeskyttelse gradering 6 eller 19",
                ),
                lagFiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.Identifikator.SØKNADEN_INNEHOLDER_IKKE_VEDLEGG,
                    resultat = Resultat.IKKE_VURDERT,
                    begrunnelse = "Ikke vurdert",
                ),
            )

        // Act
        val begrunnelse = filtreringsreglerSøknadService.hentBegrunnelseForStoppIFiltreringsreglene(behandlingId)

        // Assert
        assertThat(begrunnelse).isNull()
    }
}
