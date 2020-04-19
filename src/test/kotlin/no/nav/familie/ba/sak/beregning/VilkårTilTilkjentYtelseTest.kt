package no.nav.familie.ba.sak.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.beregning.domene.Sats
import no.nav.familie.ba.sak.beregning.domene.SatsRepository
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import java.time.LocalDate
import java.time.YearMonth

class VilkårTilTilkjentYtelseTest {


    lateinit var satsService: SatsService
    lateinit var tilkjentYtelseService: TilkjentYtelseService

    @BeforeEach
    fun setUp() {
        val satsRepository = mockk<SatsRepository>()

        satsService = SatsService(satsRepository)
        every { satsRepository.finnAlleSatserFor(any()) } answers {
            listOf(
                    Sats(type = SatsType.ORBA,
                         beløp = 1054,
                         gyldigFom = LocalDate.of(2019, 3, 1),
                         gyldigTom = null
                    ),
                    Sats(type = SatsType.ORBA,
                         beløp = 970,
                         gyldigFom = null,
                         gyldigTom = LocalDate.of(2019, 2, 28)
                    )
            )
        }

        tilkjentYtelseService = TilkjentYtelseService(satsService)
    }

    @ParameterizedTest
    @CsvFileSource(resources = ["/beregning/vilkår_til_tilkjent_ytelse/søker_med_ett_barn_inntil_to_perioder.csv"],
                   numLinesToSkip = 1,
                   delimiter = ';')
    fun `test søker med ett barn, inntil to perioder`(
            søkerPeriode1: String,
            søkerVilkår1: String,
            søkerPeriode2: String?,
            søkerVilkår2: String?,
            barn1Periode1: String,
            barn1Vilkår1: String,
            barn1Periode2: String?,
            barn1Vilkår2: String?,
            resultater: String?,
            barn1Andel1Beløp: String?,
            barn1Andel1Periode: String?,
            barn1Andel1Type: String?,
            barn1Andel2Beløp: String?,
            barn1Andel2Periode: String?,
            barn1Andel2Type: String?) {

        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)

        val behandlingResultat = TestBehandlingResultatBuilder()
                .medPersonVilkårPeriode(søker, søkerVilkår1, søkerPeriode1)
                .medPersonVilkårPeriode(søker, søkerVilkår2, søkerPeriode2)
                .medPersonVilkårPeriode(barn1, barn1Vilkår1, barn1Periode1)
                .medPersonVilkårPeriode(barn1, barn1Vilkår2, barn1Periode2)
                .bygg()

        val forventetTilkjentYtelse = TestTilkjentYtelseBuilder(behandlingResultat.behandling)
                .medAndelTilkjentYtelse(barn1, barn1Andel1Beløp, barn1Andel1Periode, barn1Andel1Type)
                .medAndelTilkjentYtelse(barn1, barn1Andel2Beløp, barn1Andel2Periode, barn1Andel2Type)
                .bygg()

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandlingResultat.behandling.id, søker, barn1)

        val faktiskTilkjentYtelse = tilkjentYtelseService.mapBehandlingResultatTilTilkjentYtelse(
                behandlingResultat = behandlingResultat,
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        Assertions.assertEquals(forventetTilkjentYtelse.andelerTilkjentYtelse,
                                faktiskTilkjentYtelse.andelerTilkjentYtelse)
    }
}

class TestBehandlingResultatBuilder {
    private val identPersonResultatMap = mutableMapOf<String, PersonResultat>()
    private val behandlingResultat = BehandlingResultat(behandling = lagBehandling())

    fun medPersonVilkårPeriode(person: Person, vilkår: String?, periode: String?): TestBehandlingResultatBuilder {

        if (vilkår.isNullOrEmpty() || periode.isNullOrEmpty())
            return this

        val ident = person.personIdent.ident
        val personResultat = identPersonResultatMap.getOrPut(ident, { PersonResultat(0, behandlingResultat, ident) })

        val testperiode = TestPeriode.parse(periode)

        val vilkårsresultater = TestVilkårParser.parse(vilkår).map {
            VilkårResultat(
                    personResultat = personResultat,
                    vilkårType = it,
                    resultat = Resultat.JA,
                    periodeFom = testperiode.fraOgMed,
                    periodeTom = testperiode.tilOgMed,
                    begrunnelse = "")
        }.toSet()

        personResultat.vilkårResultater = personResultat.vilkårResultater.plus(vilkårsresultater)

        return this
    }

    fun bygg(): BehandlingResultat {
        behandlingResultat.personResultater = identPersonResultatMap.values.toSet()

        return behandlingResultat
    }
}

class TestTilkjentYtelseBuilder(val behandling: Behandling) {

    private val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now())

    fun medAndelTilkjentYtelse(person: Person, beløp: String?, periode: String?, type: String?): TestTilkjentYtelseBuilder {
        if (beløp.isNullOrEmpty() || periode.isNullOrEmpty() || type.isNullOrEmpty())
            return this

        val stønadPeriode = TestPeriode.parse(periode);

        tilkjentYtelse.andelerTilkjentYtelse.add(
                AndelTilkjentYtelse(
                        behandlingId = behandling.id,
                        tilkjentYtelse = tilkjentYtelse,
                        personId = person.id,
                        stønadFom = stønadPeriode.fraOgMed,
                        stønadTom = stønadPeriode.tilOgMed!!,
                        beløp = beløp.toInt(),
                        type = Ytelsetype.valueOf(type)
                )
        )

        return this
    }

    fun bygg(): TilkjentYtelse {
        return tilkjentYtelse;
    }
}

data class TestPeriode(val fraOgMed: LocalDate, val tilOgMed: LocalDate?) {

    companion object {
        val yearMonthRegex = """^(\d{4}-\d{2}).*(\d{4}-\d{2})$""".toRegex()
        val localDateRegex = """^(\d{4}-\d{2}-\d{2}).*(\d{4}-\d{2}-\d{2})$""".toRegex()

        fun parse(s: String): TestPeriode {
            return prøvYearMonth(s) ?: prøvLocalDate(s) ?: throw IllegalArgumentException("Kunne ikke parse periode '$s'")
        }

        private fun prøvLocalDate(s: String): TestPeriode? {
            val yearMonthMatch = localDateRegex.find(s)

            if (yearMonthMatch != null && yearMonthMatch.groupValues.size == 3) {
                val fom = yearMonthMatch.groupValues.get(1).let { LocalDate.parse(it) }
                val tom = yearMonthMatch.groupValues.get(2).let { LocalDate.parse(it) }

                return TestPeriode(fom!!, tom)
            }
            return null
        }

        private fun prøvYearMonth(s: String): TestPeriode? {
            val yearMonthMatch = yearMonthRegex.find(s)

            if (yearMonthMatch != null && yearMonthMatch.groupValues.size == 3) {
                val fom = yearMonthMatch.groupValues.get(1).let { YearMonth.parse(it) }
                val tom = yearMonthMatch.groupValues.get(2).let { YearMonth.parse(it) }

                return TestPeriode(fom!!.atDay(1), tom?.atEndOfMonth())
            }
            return null
        }
    }
}

object TestVilkårParser {
    fun parse(s: String): List<Vilkår> {

        return s.split(',')
                .map {
                    when (it.trim().toLowerCase()) {
                        "opphold" -> Vilkår.LOVLIG_OPPHOLD
                        "under 18" -> Vilkår.UNDER_18_ÅR
                        "under 18 år" -> Vilkår.UNDER_18_ÅR
                        "bosatt" -> Vilkår.BOSATT_I_RIKET
                        "bor med søker" -> Vilkår.BOR_MED_SØKER
                        "gift" -> Vilkår.GIFT_PARTNERSKAP
                        "partnerskap" -> Vilkår.GIFT_PARTNERSKAP
                        else -> throw IllegalArgumentException("Ukjent vilkår: $s")
                    }
                }.toList()
    }
}