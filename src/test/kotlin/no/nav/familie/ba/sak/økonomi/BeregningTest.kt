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
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import java.util.function.Predicate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
class BeregningTest(
        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val beregning: Beregning
) {

    @Test
    fun `Skal sjekke at tidslinjen for 3 barn blir riktig`() {
        val behandling = behandlingService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING, "sdf", "lagRandomSaksnummer")
        val behandlingVedtak = BehandlingVedtak(behandling = behandling, ansvarligSaksbehandler = "ansvarligSaksbehandler", vedtaksdato = LocalDate.now(), stønadFom = LocalDate.now(), stønadTom = LocalDate.now().plusDays(1), stønadBrevMarkdown = "")

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


        Assertions.assertEquals(tidslinje.size(), 6)

        // Sjekk at første periode er på 2 år
        val nå = LocalDate.now()
        val toÅrFrem = nå.plusYears(2)
        Assertions.assertEquals(
                tidslinje.datoIntervaller.pollFirst()?.totalDays(),
                Duration.between(nå.atStartOfDay(), toÅrFrem.atStartOfDay()).toDays())

        val beløp = listOf(1654, 3308, 4962, 4362, 3162, 1054)
        // Sjekk at periodene har riktig beløp
        tidslinje.toSegments().forEachIndexed { index, localDateSegment ->
            Assertions.assertEquals(localDateSegment.value, beløp[index])
        }
    }
}