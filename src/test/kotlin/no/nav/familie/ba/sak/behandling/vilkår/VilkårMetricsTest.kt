package no.nav.familie.ba.sak.behandling.vilkår

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VilkårMetricsTest(
        @Autowired
        private val vilkårService: VilkårService
) {
    val mockBorMedSøkerSuksessCounter = mockk<Counter>(relaxed = true)
    val mockBorMedSøkerFeilCounter = mockk<Counter>(relaxed = true)
    val mockOtherCounter = mockk<Counter>(relaxed = true)

    private fun genererPerson(type: PersonType,
                              personopplysningGrunnlag: PersonopplysningGrunnlag,
                              bostedsadresse: GrBostedsadresse?,
                              kjønn: Kjønn = Kjønn.KVINNE): Person {
        return Person(aktørId = randomAktørId(),
                      personIdent = PersonIdent(randomFnr()),
                      type = type,
                      personopplysningGrunnlag = personopplysningGrunnlag,
                      fødselsdato = LocalDate.of(1991, 1, 1),
                      navn = "navn",
                      kjønn = kjønn,
                      bostedsadresse = bostedsadresse)
    }

    fun mockMetrics(){
        mockkStatic(Metrics::class)
        every {
            Metrics.counter(any(),
                            any(),
                            not("BOR_MED_SØKER"),
                            any(),
                            any())
        } returns mockOtherCounter

        every {
            Metrics.counter("behandling.vilkår.suksess",
                            "vilkår",
                            "BOR_MED_SØKER",
                            any(),
                            any())
        } returns mockBorMedSøkerSuksessCounter

        every {
            Metrics.counter("behandling.vilkår.feil",
                            "vilkår",
                            "BOR_MED_SØKER",
                            any(),
                            any())
        } returns mockBorMedSøkerFeilCounter
    }

    @Test
    fun `Sjekk vilkår metrics`() {
        val søkerAddress = GrVegadresse(1234, "11", "B", "H022",
                                        "St. Olavsvegen", "1232", "whatever", "4322")
        val barnAddress = GrVegadresse(1234, "11", "B", "H024",
                                       "St. Oldavsvegen", "1232", "whatever", "4322")
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 1)

        val søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, søkerAddress)
        personopplysningGrunnlag.personer.add(søker)

        val barn1 = genererPerson(PersonType.BARN, personopplysningGrunnlag, søkerAddress, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn1)

        val barn2 = genererPerson(PersonType.BARN, personopplysningGrunnlag, barnAddress, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn2)

        val barn3 = genererPerson(PersonType.BARN, personopplysningGrunnlag, null, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn3)

        mockMetrics()
        vilkårService.addEvalueringsResultatTilMatrikkel(listOf(Vilkår.BOR_MED_SØKER.spesifikasjon.evaluer(Fakta(barn1))))
        vilkårService.addEvalueringsResultatTilMatrikkel(listOf(Vilkår.BOR_MED_SØKER.spesifikasjon.evaluer(Fakta(barn2))))
        vilkårService.addEvalueringsResultatTilMatrikkel(listOf(Vilkår.BOR_MED_SØKER.spesifikasjon.evaluer(Fakta(barn3))))

        verify(exactly = 1) {
            mockBorMedSøkerSuksessCounter.increment()
        }
        verify(exactly = 2) {
            mockBorMedSøkerFeilCounter.increment()
        }
        verify(exactly = 0) {
            mockOtherCounter.increment()
        }
    }
}