package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.Beregning
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakBarn
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
class BeregningTest(
        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val beregning: Beregning
) {

    /**
     * Testen generer 3 barn. 2 av dem er født dd. og 1 er født 2 år frem i tid.
     * Videre generer vi tidslinje for utbetaling med stønad fom og tom for samtlige barn.
     *
     * Barn 1: 18 år med barnetrygd fra sin fødselsdato
     * Barn 2: 18 år med barnetrygd fra sin fødselsdato
     * Barn 3: 15 år med barnetrygd fra 3 år etter sin fødselsdato
     *
     * Dette medfører følgende tidslinje:
     * 1 periode: 1 barn = 1054
     * 2 periode: 2 barn = 2108
     * 3 periode: 3 barn = 3162
     * 4 periode: 1 barn = 1054
     */
    @Test
    fun `Skal sjekke at tidslinjen for 3 barn blir riktig`() {
        val behandling = behandlingService.nyBehandling("0", BehandlingType.FØRSTEGANGSBEHANDLING, "sdf", "lagRandomSaksnummer")
        val behandlingVedtak = BehandlingVedtak(behandling = behandling, ansvarligSaksbehandler = "ansvarligSaksbehandler", vedtaksdato = LocalDate.now(), stønadBrevMarkdown = "")

        val barn1Fødselsdato = LocalDate.now()
        val barn2Fødselsdato = LocalDate.now().plusYears(2)
        val barn3Fødselsdato = LocalDate.now()

        val personopplysningGrunnlag = PersonopplysningGrunnlag(0L)

        val barn1 = Person(personIdent = PersonIdent("00000000001"), fødselsdato = barn1Fødselsdato, type = PersonType.BARN, personopplysningGrunnlag = personopplysningGrunnlag)
        val barn2 = Person(personIdent = PersonIdent("00000000002"), fødselsdato = barn2Fødselsdato, type = PersonType.BARN, personopplysningGrunnlag = personopplysningGrunnlag)
        val barn3 = Person(personIdent = PersonIdent("00000000003"), fødselsdato = barn2Fødselsdato, type = PersonType.BARN, personopplysningGrunnlag = personopplysningGrunnlag)

        val barnBeregning1 = BehandlingVedtakBarn(
                barn = barn1,
                stønadFom = barn1Fødselsdato,
                stønadTom = barn1Fødselsdato.plusYears(18),
                beløp = 1054,
                behandlingVedtak = behandlingVedtak
        )

        val barnBeregning2 = BehandlingVedtakBarn(
                barn = barn2,
                stønadFom = barn2Fødselsdato,
                stønadTom = barn2Fødselsdato.plusYears(18),
                beløp = 1054,
                behandlingVedtak = behandlingVedtak
        )

        val barnBeregning3 = BehandlingVedtakBarn(
                barn = barn3,
                stønadFom = barn3Fødselsdato.plusYears(3),
                stønadTom = barn3Fødselsdato.plusYears(18),
                beløp = 1054,
                behandlingVedtak = behandlingVedtak
        )

        val tidslinje = beregning.beregnUtbetalingsperioder(listOf(
                barnBeregning1,
                barnBeregning2,
                barnBeregning3
        ))


        Assertions.assertEquals(tidslinje.size(), 4)

        // Sjekk at første periode er på 2 år
        val nå = LocalDate.now()
        val toÅrFrem = nå.plusYears(2)
        Assertions.assertEquals(
                tidslinje.datoIntervaller.pollFirst()?.totalDays(),
                Duration.between(nå.atStartOfDay(), toÅrFrem.atStartOfDay()).toDays())

        val beløp = listOf(1054, 2108, 3162, 1054)
        // Sjekk at periodene har riktig beløp
        tidslinje.toSegments().forEachIndexed { index, localDateSegment ->
            Assertions.assertEquals(localDateSegment.value, beløp[index])
        }
    }
}