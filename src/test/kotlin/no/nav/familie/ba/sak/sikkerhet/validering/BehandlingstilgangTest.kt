package no.nav.familie.ba.sak.sikkerhet.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BehandlingstilgangTest {

    private lateinit var client: IntegrasjonClient

    private lateinit var behandlingstilgang: Behandlingstilgang

    @BeforeEach
    fun setUp() {
        val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository = mockk()
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) }
            .returns(personopplysningsgrunnlag)
        client = mockk()
        behandlingstilgang = Behandlingstilgang(
            personopplysningGrunnlagRepository,
            client
        )
    }

    @Test
    fun `isValid returnerer true hvis sjekkTilgangTilPersoner returnerer true for alle personer knyttet til behandling`() {
        every { client.sjekkTilgangTilPersoner(personopplysningsgrunnlag.personer.map { it.personIdent.ident }) }
            .returns(
                listOf(
                    Tilgang(true),
                    Tilgang(true),
                    Tilgang(true)
                )
            )

        val harTilgang = behandlingstilgang.isValid(1, mockk())

        assertTrue(harTilgang)
    }

    @Test
    fun `isValid returnerer false hvis sjekkTilgangTilPersoner returnerer false for en person knyttet til behandling`() {
        every { client.sjekkTilgangTilPersoner(personopplysningsgrunnlag.personer.map { it.personIdent.ident }) }
            .returns(
                listOf(
                    Tilgang(true),
                    Tilgang(false),
                    Tilgang(true)
                )
            )

        val harTilgang = behandlingstilgang.isValid(1, mockk())

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
                    personopplysningGrunnlag = PersonopplysningGrunnlag(1, 1),
                    aktør = randomAktørId(),
                )
                    .apply {
                        sivilstander =
                            listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this))
                    }
            ),
            true
        )
}
