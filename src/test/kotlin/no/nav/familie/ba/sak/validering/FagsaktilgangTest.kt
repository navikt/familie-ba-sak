package no.nav.familie.ba.sak.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import no.nav.familie.ba.sak.integrasjoner.domene.Tilgang
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FagsaktilgangTest {

    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    private lateinit var onBehalfClient: IntegrasjonOnBehalfClient

    private lateinit var behandlingRepository: BehandlingRepository

    private lateinit var fagsaktilgang: Fagsaktilgang

    @BeforeEach
    fun setUp() {
        behandlingRepository = mockk()
        personopplysningGrunnlagRepository = mockk()
        onBehalfClient = mockk()
        every { behandlingRepository.finnBehandlinger(any()) }
                .returns(behandlinger)
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) }
                .returns(personopplysningsgrunnlag)
        fagsaktilgang = Fagsaktilgang(behandlingRepository,
                                      personopplysningGrunnlagRepository,
                                      onBehalfClient)
    }


    @Test
    fun `isValid returnerer true om sjekkTilgangTilPersoner gir true for alle personer knyttet til behandlinger for fagsak`() {
        every { onBehalfClient.sjekkTilgangTilPersoner(personopplysningsgrunnlag.personer) }
                .returns(listOf(Tilgang(true),
                                Tilgang(true),
                                Tilgang(true)))

        val harTilgang = fagsaktilgang.isValid(1, mockk())

        assertTrue(harTilgang)
    }

    @Test
    fun `isValid returnerer false om sjekkTilgangTilPersoner gir false for en person knyttet til en behandling for fagsak`() {
        every { onBehalfClient.sjekkTilgangTilPersoner(personopplysningsgrunnlag.personer) }
                .returns(listOf(Tilgang(true),
                                Tilgang(false),
                                Tilgang(true)))

        val harTilgang = fagsaktilgang.isValid(1, mockk())

        assertFalse(harTilgang)
    }

    private val personopplysningsgrunnlag =
            PersonopplysningGrunnlag(1,
                                     1,
                                     mutableListOf(Person(1,
                                                          PersonType.SØKER,
                                                          LocalDate.of(1984, 12, 16),
                                                          PersonIdent("1984121632121"),
                                                          PersonopplysningGrunnlag(1, 1))),
                                     true)

    private val behandlinger = listOf(Behandling(id = 1,
                                                 fagsak = mockk(),
                                                 kategori = BehandlingKategori.NASJONAL,
                                                 type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                 underkategori = BehandlingUnderkategori.ORDINÆR),
                                      Behandling(id = 2,
                                                 fagsak = mockk(),
                                                 kategori = BehandlingKategori.NASJONAL,
                                                 type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                 underkategori = BehandlingUnderkategori.ORDINÆR))
}