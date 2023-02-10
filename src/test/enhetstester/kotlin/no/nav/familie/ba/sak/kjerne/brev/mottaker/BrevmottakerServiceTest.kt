package no.nav.familie.ba.sak.kjerne.brev.mottaker

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class BrevmottakerServiceTest {

    @MockK
    private lateinit var brevmottakerRepository: BrevmottakerRepository

    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var personopplysningerService: PersonopplysningerService

    @InjectMockKs
    private lateinit var brevmottakerService: BrevmottakerService

    private val søkersident = "123"
    private val søkersnavn = "Test søker"

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har norsk adresse`() {
        val brevmottakere = listOf(lagBrevMottaker(mottakerType = MottakerType.FULLMEKTIG))
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere, søkersident, søkersnavn)
        assertTrue { mottakerInfo.size == 2 }

        assertEquals("John Doe", mottakerInfo.first().navn)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }

        assertEquals(søkersnavn, mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo == null }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har utenlandsk adresse`() {
        val brevmottakere = listOf(
            lagBrevMottaker(mottakerType = MottakerType.FULLMEKTIG),
            lagBrevMottaker(
                mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                poststed = "Munchen",
                landkode = "DE"
            )
        )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere, søkersident, søkersnavn)
        assertTrue { mottakerInfo.size == 2 }

        assertEquals("John Doe", mottakerInfo.first().navn)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }

        assertEquals(søkersnavn, mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.last().manuellAdresseInfo!!.landkode == "DE" }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når brevmottaker er VERGE og bruker har utenlandsk adresse`() {
        val brevmottakere = listOf(
            lagBrevMottaker(mottakerType = MottakerType.VERGE),
            lagBrevMottaker(
                mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                poststed = "Munchen",
                landkode = "DE"
            )
        )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere, søkersident, søkersnavn)
        assertTrue { mottakerInfo.size == 2 }

        assertEquals("John Doe", mottakerInfo.first().navn)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }

        assertEquals(søkersnavn, mottakerInfo.last().navn)
        assertTrue { mottakerInfo.last().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.last().manuellAdresseInfo!!.landkode == "DE" }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når bruker har utenlandsk adresse`() {
        val brevmottakere = listOf(
            lagBrevMottaker(
                mottakerType = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                poststed = "Munchen",
                landkode = "DE"
            )
        )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere, søkersident, søkersnavn)
        assertTrue { mottakerInfo.size == 1 }

        assertEquals(søkersnavn, mottakerInfo.first().navn)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }
    }

    @Test
    fun `lagMottakereFraBrevMottakere skal lage mottakere når bruker har dødsbo`() {
        val brevmottakere = listOf(
            lagBrevMottaker(
                mottakerType = MottakerType.DØDSBO,
                poststed = "Munchen",
                landkode = "DE"
            )
        )
        every { brevmottakerRepository.finnBrevMottakereForBehandling(any()) } returns brevmottakere

        val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere, søkersident, søkersnavn)
        assertTrue { mottakerInfo.size == 1 }

        assertEquals(søkersnavn, mottakerInfo.first().navn)
        assertTrue { mottakerInfo.first().manuellAdresseInfo != null }
        assertTrue { mottakerInfo.first().manuellAdresseInfo!!.landkode == "DE" }
    }

    private fun lagBrevMottaker(mottakerType: MottakerType, poststed: String = "Oslo", landkode: String = "NO") =
        Brevmottaker(
            behandlingId = 1,
            type = mottakerType,
            navn = "John Doe",
            adresselinje1 = "adresse 1",
            adresselinje2 = "adresse 2",
            postnummer = "000",
            poststed = poststed,
            landkode = landkode
        )
}
