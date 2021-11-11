package no.nav.familie.ba.sak.sikkerhet.validering

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FagsaktilgangTest {

    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    private lateinit var client: IntegrasjonClient

    private lateinit var behandlingRepository: BehandlingRepository

    private lateinit var fagsakRepository: FagsakRepository

    private lateinit var fagsaktilgang: Fagsaktilgang

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @BeforeEach
    fun setUp() {
        behandlingRepository = mockk()
        personopplysningGrunnlagRepository = mockk()
        fagsakRepository = mockk(relaxed = true)
        client = mockk()
        every { behandlingRepository.finnBehandlinger(any()) }
            .returns(behandlinger)
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) }
            .returns(personopplysningsgrunnlag)
        fagsaktilgang = Fagsaktilgang(
            behandlingRepository,
            personopplysningGrunnlagRepository,
            client,
            fagsakRepository
        )
    }

    @Test
    fun `isValid returnerer true om sjekkTilgangTilPersoner gir true for alle personer knyttet til behandlinger for fagsak`() {
        every { client.sjekkTilgangTilPersoner(personopplysningsgrunnlag.personer.map { it.personIdent.ident }) }
            .returns(
                listOf(
                    Tilgang(true),
                    Tilgang(true),
                    Tilgang(true)
                )
            )

        val harTilgang = fagsaktilgang.isValid(1, mockk())

        assertTrue(harTilgang)
    }

    @Test
    fun `isValid returnerer false om sjekkTilgangTilPersoner gir false for en person knyttet til en behandling for fagsak`() {
        every { client.sjekkTilgangTilPersoner(personopplysningsgrunnlag.personer.map { it.personIdent.ident }) }
            .returns(
                listOf(
                    Tilgang(true),
                    Tilgang(false),
                    Tilgang(true)
                )
            )

        val harTilgang = fagsaktilgang.isValid(1, mockk())

        assertFalse(harTilgang)
    }

    private val personopplysningsgrunnlag =
        PersonopplysningGrunnlag(
            1,
            1,
            mutableSetOf(
                Person(
                    type = PersonType.SØKER,
                    fødselsdato = LocalDate.of(1984, 12, 16),
                    navn = "Mock Mockson",
                    kjønn = Kjønn.MANN,
                    personIdent = PersonIdent(randomFnr()),
                    målform = Målform.NB,
                    personopplysningGrunnlag = PersonopplysningGrunnlag(1, 1)
                )
                    .apply {
                        sivilstander =
                            listOf(GrSivilstand(type = SIVILSTAND.GIFT, person = this))
                    }
            ),
            true
        )

    private val behandlinger = listOf(
        Behandling(
            id = 1,
            fagsak = mockk(),
            kategori = BehandlingKategori.NASJONAL,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            opprettetÅrsak = BehandlingÅrsak.SØKNAD
        ).also {
            it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, FØRSTE_STEG))
        },
        Behandling(
            id = 2,
            fagsak = mockk(),
            kategori = BehandlingKategori.NASJONAL,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            opprettetÅrsak = BehandlingÅrsak.SØKNAD
        ).also {
            it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, FØRSTE_STEG))
        }
    )
}
