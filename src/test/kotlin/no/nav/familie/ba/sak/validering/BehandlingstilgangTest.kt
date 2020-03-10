package no.nav.familie.ba.sak.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.*
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import no.nav.familie.ba.sak.integrasjoner.domene.Tilgang
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BehandlingstilgangTest {


    private lateinit var onBehalfClient: IntegrasjonOnBehalfClient

    private lateinit var behandlingstilgang: Behandlingstilgang

    @BeforeEach
    fun setUp() {
        val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository = mockk()
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) }
                .returns(personopplysningsgrunnlag)
        onBehalfClient = mockk()
        behandlingstilgang = Behandlingstilgang(personopplysningGrunnlagRepository,
                                                onBehalfClient)
    }


    @Test
    fun `isValid returnerer true hvis sjekkTilgangTilPersoner returnerer true for alle personer knyttet til behandling`() {
        every { onBehalfClient.sjekkTilgangTilPersoner(personopplysningsgrunnlag.personer) }
                .returns(listOf(Tilgang(true),
                                Tilgang(true),
                                Tilgang(true)))

        val harTilgang = behandlingstilgang.isValid(1, mockk())

        assertTrue(harTilgang)
    }

    @Test
    fun `isValid returnerer false hvis sjekkTilgangTilPersoner returnerer false for en person knyttet til behandling`() {
        every { onBehalfClient.sjekkTilgangTilPersoner(personopplysningsgrunnlag.personer) }
                .returns(listOf(Tilgang(true),
                                Tilgang(false),
                                Tilgang(true)))

        val harTilgang = behandlingstilgang.isValid(1, mockk())

        assertFalse(harTilgang)
    }

    private val personopplysningsgrunnlag =
            PersonopplysningGrunnlag(1,
                                     1,
                                     mutableSetOf(Person(1,
                                                         PersonType.SØKER,
                                                         LocalDate.of(1984, 12, 16),
                                                         "Mock Mockson",
                                                         Kjønn.MANN,
                                                         PersonIdent(randomFnr()),
                                                         PersonopplysningGrunnlag(1, 1))),
                                     true)


}