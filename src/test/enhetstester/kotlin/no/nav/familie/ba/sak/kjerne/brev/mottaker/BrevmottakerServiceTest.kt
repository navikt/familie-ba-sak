package no.nav.familie.ba.sak.kjerne.brev.mottaker

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.BrevmottakerDto
import no.nav.familie.ba.sak.kjerne.behandling.ValiderBrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull

internal class BrevmottakerServiceTest {
    private val brevmottakerRepository: BrevmottakerRepository = mockk()

    private val validerBrevmottakerService: ValiderBrevmottakerService = mockk()

    private val loggService: LoggService = mockk()

    private val brevmottakerService =
        BrevmottakerService(
            brevmottakerRepository = brevmottakerRepository,
            loggService = loggService,
            validerBrevmottakerService = validerBrevmottakerService,
        )

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har norsk adresse`() {
        val brevmottakere = listOf(lagBrevMottakerDb(mottakerType = MottakerType.FULLMEKTIG))
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo =
            brevmottakerService.lagMottakereFraBrevMottakere(
                brevmottakere.map { ManuellBrevmottaker(it) },
            )
        assertTrue { mottakerInfo.size == 2 }

        assertTrue { mottakerInfo.first().manuellAdresseInfo == null }

        assertEquals("John Doe", mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo != null }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har utenlandsk adresse`() {
        val brevmottakere =
            listOf(
                lagBrevMottakerDb(mottakerType = MottakerType.FULLMEKTIG),
                lagBrevMottakerDb(
                    mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
            )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo =
            brevmottakerService.lagMottakereFraBrevMottakere(
                brevmottakere.map { ManuellBrevmottaker(it) },
            )
        assertTrue { mottakerInfo.size == 2 }

        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }

        assertEquals("John Doe", mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo != null }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er VERGE og bruker har utenlandsk adresse`() {
        val brevmottakere =
            listOf(
                lagBrevMottakerDb(mottakerType = MottakerType.VERGE),
                lagBrevMottakerDb(
                    mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
            )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo =
            brevmottakerService.lagMottakereFraBrevMottakere(
                brevmottakere.map { ManuellBrevmottaker(it) },
            )
        assertTrue { mottakerInfo.size == 2 }

        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }

        assertEquals("John Doe", mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo != null }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når bruker har utenlandsk adresse`() {
        val brevmottakere =
            listOf(
                lagBrevMottakerDb(
                    mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
            )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo =
            brevmottakerService.lagMottakereFraBrevMottakere(
                brevmottakere.map { ManuellBrevmottaker(it) },
            )
        assertTrue { mottakerInfo.size == 1 }

        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når bruker har dødsbo`() {
        val brevmottakere =
            listOf(
                lagBrevMottakerDb(
                    mottakerType = MottakerType.DØDSBO,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
            )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo =
            brevmottakerService.lagMottakereFraBrevMottakere(
                brevmottakere.map { ManuellBrevmottaker(it) },
            )
        assertTrue { mottakerInfo.size == 1 }

        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal kaste feil når brevmottakere inneholder ugyldig kombinasjon`() {
        val brevmottakere =
            listOf(
                lagBrevMottakerDb(
                    mottakerType = MottakerType.VERGE,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
                lagBrevMottakerDb(
                    mottakerType = MottakerType.FULLMEKTIG,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
            )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        assertThrows<FunksjonellFeil> {
            brevmottakerService.lagMottakereFraBrevMottakere(
                brevmottakere.map { ManuellBrevmottaker(it) },
            )
        }.also {
            assertTrue(it.frontendFeilmelding!!.contains("kan ikke kombineres"))
        }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal kaste feil dersom brevmottakere inneholder to av typen BrukerMedUtenlandskAdresse`() {
        val brevmottakere =
            listOf(
                lagBrevMottakerDb(
                    mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
                lagBrevMottakerDb(
                    mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                    poststed = "Munchen",
                    landkode = "DE",
                ),
            )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        assertThrows<FunksjonellFeil> {
            brevmottakerService.lagMottakereFraBrevMottakere(
                brevmottakere.map { ManuellBrevmottaker(it) },
            )
        }.also {
            assertTrue(
                it.frontendFeilmelding!!.contains(
                    "Mottakerfeil: Det er registrert mer enn en utenlandsk adresse tilhørende bruker",
                ),
            )
        }
    }

    @Test
    fun `leggTilBrevmottaker skal lagre logg på at brevmottaker legges til`() {
        val brevmottakerDto = mockk<BrevmottakerDto>(relaxed = true)

        every {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                any(),
                any(),
                any(),
            )
        } just runs
        every { loggService.opprettBrevmottakerLogg(any(), false) } just runs
        every { brevmottakerRepository.save(any()) } returns mockk()

        brevmottakerService.leggTilBrevmottaker(brevmottakerDto, 200)

        verify { loggService.opprettBrevmottakerLogg(any(), false) }
        verify { brevmottakerRepository.save(any()) }
    }

    @Test
    fun `fjernBrevmottaker skal kaste feil dersom brevmottakeren ikke finnes`() {
        every { brevmottakerRepository.findByIdOrNull(404) } returns null

        assertThrows<Feil> {
            brevmottakerService.fjernBrevmottaker(404)
        }

        verify { brevmottakerRepository.findByIdOrNull(404) }
    }

    @Test
    fun `fjernBrevmottaker skal lagre logg på at brevmottaker fjernes`() {
        val mocketBrevmottaker = mockk<BrevmottakerDb>()

        every { brevmottakerRepository.findByIdOrNull(200) } returns mocketBrevmottaker
        every { loggService.opprettBrevmottakerLogg(mocketBrevmottaker, true) } just runs
        every { brevmottakerRepository.deleteById(200) } just runs

        brevmottakerService.fjernBrevmottaker(200)

        verify { brevmottakerRepository.findByIdOrNull(200) }
        verify { loggService.opprettBrevmottakerLogg(mocketBrevmottaker, true) }
        verify { brevmottakerRepository.deleteById(200) }
    }

    private fun lagBrevMottakerDb(
        mottakerType: MottakerType,
        poststed: String = "Oslo",
        landkode: String = "NO",
    ) = BrevmottakerDb(
        behandlingId = 1,
        type = mottakerType,
        navn = "John Doe",
        adresselinje1 = "adresse 1",
        adresselinje2 = "adresse 2",
        postnummer = "000",
        poststed = poststed,
        landkode = landkode,
    )
}
