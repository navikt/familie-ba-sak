package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.ForlengetSvartidsbrev
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ManueltBrevRequestTest {
    private val årsaker = listOf("1", "2", "3")
    private val baseRequest = ManueltBrevRequest(
        brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
        multiselectVerdier = årsaker,
        mottakerIdent = "testident",
        mottakerNavn = "testnavn",
        enhet = Enhet("testenhetId", "testenhet"),
        antallUkerSvarfrist = 3
    )

    @Test
    fun `Forlenget svartidsbrev request skal gi forlenget svartid brevmal med riktig data`() {
        val brev = baseRequest.copy(Brevmal.FORLENGET_SVARTIDSBREV).tilBrev()

        assertThat(brev::class).isEqualTo(ForlengetSvartidsbrev::class)
        brev as ForlengetSvartidsbrev

        assertThat(brev.mal).isEqualTo(Brevmal.FORLENGET_SVARTIDSBREV)

        assertThat(brev.data.flettefelter.antallUkerSvarfrist!!.single()).isEqualTo("3")
        assertThat(brev.data.flettefelter.aarsakerSvartidsbrev!!).isEqualTo(årsaker)
    }

    @Test
    fun `Forlenget svartidsbrev institusjon request skal gi forlenget svartid brevmal med riktig data`() {
        val brev = baseRequest.copy(
            brevmal = Brevmal.FORLENGET_SVARTIDSBREV_INSTITUSJON,
            mottakerIdent = "998765432",
            mottakerNavn = "Testorganisasjon",
            vedrørende = PersonITest(
                fødselsnummer = "testident",
                navn = "testnavn"
            )
        )
            .tilBrev()

        assertThat(brev::class).isEqualTo(ForlengetSvartidsbrev::class)
        brev as ForlengetSvartidsbrev

        assertThat(brev.mal).isEqualTo(Brevmal.FORLENGET_SVARTIDSBREV)

        assertThat(brev.data.flettefelter.antallUkerSvarfrist!!.single()).isEqualTo("3")
        assertThat(brev.data.flettefelter.aarsakerSvartidsbrev!!).isEqualTo(årsaker)
        assertThat(brev.data.flettefelter.organisasjonsnummer).containsExactly("998765432")
        assertThat(brev.data.flettefelter.navn).containsExactly("Testorganisasjon")
        assertThat(brev.data.flettefelter.fodselsnummer).containsExactly("testident")
        assertThat(brev.data.flettefelter.gjelder).containsExactly("testnavn")
    }

    @Test
    fun `Innhente opplysninger brev til person og institusjon`() {
        val fnr = "12345678910"
        val orgnr = "123456789"
        val brevRequestTilPerson = baseRequest.copy(
            mottakerIdent = fnr
        )
        val brevRequestTilInstitusjon = baseRequest.copy(
            brevmal = Brevmal.INNHENTE_OPPLYSNINGER_INSTITUSJON,
            mottakerIdent = orgnr,
            vedrørende = PersonITest(
                fødselsnummer = fnr,
                navn = "navn tilhørende $fnr"
            )
        )
        brevRequestTilPerson.tilBrev().data.apply {
            assertThat(flettefelter.fodselsnummer).containsExactly(brevRequestTilPerson.mottakerIdent)
            assertThat(flettefelter.navn).containsExactly(brevRequestTilPerson.mottakerNavn)
            assertThat(flettefelter.organisasjonsnummer).isNull()
            assertThat(flettefelter.gjelder).isNull()
        }
        brevRequestTilInstitusjon.tilBrev().data.apply {
            assertThat(flettefelter.organisasjonsnummer).containsExactly(brevRequestTilInstitusjon.mottakerIdent)
            assertThat(flettefelter.fodselsnummer).containsExactly(brevRequestTilInstitusjon.vedrørende?.fødselsnummer)
            assertThat(flettefelter.navn).containsExactly(brevRequestTilPerson.mottakerNavn)
            assertThat(flettefelter.gjelder).containsExactly(brevRequestTilInstitusjon.vedrørende?.navn)
        }
    }

    class PersonITest(override val fødselsnummer: String, override val navn: String) : Person
}
