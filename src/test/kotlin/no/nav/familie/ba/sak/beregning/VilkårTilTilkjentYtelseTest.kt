package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import java.time.LocalDate
import java.time.YearMonth

class VilkårTilTilkjentYtelseTest {

    @ParameterizedTest
    @CsvFileSource(resources = ["/beregning/vilkår_til_tilkjent_ytelse/søker_med_ett_barn_inntil_to_perioder.csv"],
                   numLinesToSkip = 1,
                   delimiter = ';')
    fun `test søker med ett barn, inntil to perioder`(
            sakType: String,
            søkerPeriode1: String?,
            søkerVilkår1: String?,
            søkerPeriode2: String?,
            søkerVilkår2: String?,
            barn1Periode1: String?,
            barn1Vilkår1: String?,
            barn1Periode2: String?,
            barn1Vilkår2: String?,
            resultater: String?,
            barn1Andel1Beløp: Int?,
            barn1Andel1Periode: String?,
            barn1Andel1Type: String?,
            barn1Andel2Beløp: Int?,
            barn1Andel2Periode: String?,
            barn1Andel2Type: String?) {

        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)

        val behandlingResultat = TestBehandlingResultatBuilder(sakType)
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

        val faktiskTilkjentYtelse = TilkjentYtelseService.beregnTilkjentYtelse(
                behandlingResultat = behandlingResultat,
                sakType = SakType.valueOf(sakType),
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        Assertions.assertEquals(forventetTilkjentYtelse.andelerTilkjentYtelse,
                                faktiskTilkjentYtelse.andelerTilkjentYtelse)
    }

    @ParameterizedTest
    @CsvFileSource(resources = ["/beregning/vilkår_til_tilkjent_ytelse/søker_med_to_barn_inntil_to_perioder.csv"],
                   numLinesToSkip = 1,
                   delimiter = ';')
    fun `test søker med to barn, inntil to perioder`(
            søkerPeriode1: String?,
            søkerVilkår1: String?,
            søkerPeriode2: String?,
            søkerVilkår2: String?,
            barn1Periode1: String?,
            barn1Vilkår1: String?,
            barn1Periode2: String?,
            barn1Vilkår2: String?,
            barn2Periode1: String?,
            barn2Vilkår1: String?,
            barn2Periode2: String?,
            barn2Vilkår2: String?,
            resultater: String?,
            barn1Andel1Beløp: Int?,
            barn1Andel1Periode: String?,
            barn1Andel1Type: String?,
            barn1Andel2Beløp: Int?,
            barn1Andel2Periode: String?,
            barn1Andel2Type: String?,
            barn2Andel1Beløp: Int?,
            barn2Andel1Periode: String?,
            barn2Andel1Type: String?,
            barn2Andel2Beløp: Int?,
            barn2Andel2Periode: String?,
            barn2Andel2Type: String?) {

        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)

        val behandlingResultat = TestBehandlingResultatBuilder("NASJONAL")
                .medPersonVilkårPeriode(søker, søkerVilkår1, søkerPeriode1)
                .medPersonVilkårPeriode(søker, søkerVilkår2, søkerPeriode2)
                .medPersonVilkårPeriode(barn1, barn1Vilkår1, barn1Periode1)
                .medPersonVilkårPeriode(barn1, barn1Vilkår2, barn1Periode2)
                .medPersonVilkårPeriode(barn2, barn2Vilkår1, barn2Periode1)
                .medPersonVilkårPeriode(barn2, barn2Vilkår2, barn2Periode2)
                .bygg()

        val forventetTilkjentYtelse = TestTilkjentYtelseBuilder(behandlingResultat.behandling)
                .medAndelTilkjentYtelse(barn1, barn1Andel1Beløp, barn1Andel1Periode, barn1Andel1Type)
                .medAndelTilkjentYtelse(barn1, barn1Andel2Beløp, barn1Andel2Periode, barn1Andel2Type)
                .medAndelTilkjentYtelse(barn2, barn2Andel1Beløp, barn2Andel1Periode, barn2Andel1Type)
                .medAndelTilkjentYtelse(barn2, barn2Andel2Beløp, barn2Andel2Periode, barn2Andel2Type)
                .bygg()

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandlingResultat.behandling.id, søker, barn1, barn2)

        val faktiskTilkjentYtelse = TilkjentYtelseService.beregnTilkjentYtelse(
                behandlingResultat = behandlingResultat,
                sakType = SakType.NASJONAL,
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        Assertions.assertEquals(forventetTilkjentYtelse.andelerTilkjentYtelse,
                                faktiskTilkjentYtelse.andelerTilkjentYtelse)
    }

}

class TestBehandlingResultatBuilder(val sakType: String) {
    private val identPersonResultatMap = mutableMapOf<String, PersonResultat>()
    private val behandlingResultat =
            BehandlingResultat(behandling = lagBehandling(
                    behandlingKategori = BehandlingKategori.valueOf(sakType)))

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
                    begrunnelse = "",
                    behandlingId = behandlingResultat.behandling.id)
        }.toSet()

        personResultat.setVilkårResultater(personResultat.vilkårResultater.plus(vilkårsresultater)
                                                   .toSet())

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

    fun medAndelTilkjentYtelse(person: Person, beløp: Int?, periode: String?, type: String?): TestTilkjentYtelseBuilder {
        if (beløp == null || periode.isNullOrEmpty() || type.isNullOrEmpty())
            return this

        val stønadPeriode = TestPeriode.parse(periode);

        tilkjentYtelse.andelerTilkjentYtelse.add(
                AndelTilkjentYtelse(
                        behandlingId = behandling.id,
                        tilkjentYtelse = tilkjentYtelse,
                        personId = person.id,
                        personIdent = person.personIdent.ident,
                        stønadFom = stønadPeriode.fraOgMed,
                        stønadTom = stønadPeriode.tilOgMed!!,
                        beløp = beløp.toInt(),
                        type = YtelseType.valueOf(type)
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
        val yearMonthRegex = """^(\d{4}-\d{2}).*?(\d{4}-\d{2})?$""".toRegex()
        val localDateRegex = """^(\d{4}-\d{2}-\d{2}).*?(\d{4}-\d{2}-\d{2})?$""".toRegex()

        fun parse(s: String): TestPeriode {
            return prøvLocalDate(s) ?: prøvYearMonth(s) ?: throw IllegalArgumentException("Kunne ikke parse periode '$s'")
        }

        private fun prøvLocalDate(s: String): TestPeriode? {
            val localDateMatch = localDateRegex.find(s)

            if (localDateMatch != null && localDateMatch.groupValues.size == 3) {
                val fom = localDateMatch.groupValues[1].let { LocalDate.parse(it) }
                val tom = localDateMatch.groupValues[2].let { if (it.length == 10) LocalDate.parse(it) else LocalDate.MAX }

                return TestPeriode(fom!!, tom)
            }
            return null
        }

        private fun prøvYearMonth(s: String): TestPeriode? {
            val yearMonthMatch = yearMonthRegex.find(s)

            if (yearMonthMatch != null && yearMonthMatch.groupValues.size == 3) {
                val fom = yearMonthMatch.groupValues[1].let { YearMonth.parse(it) }
                val tom =
                        yearMonthMatch.groupValues[2].let { if (it.length == 7) YearMonth.parse(it) else YearMonth.from(LocalDate.MAX) }

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
                    when (it.replace("""\s*""".toRegex(), "").toLowerCase()) {
                        "opphold" -> Vilkår.LOVLIG_OPPHOLD
                        "<18" -> Vilkår.UNDER_18_ÅR
                        "<18år" -> Vilkår.UNDER_18_ÅR
                        "under18" -> Vilkår.UNDER_18_ÅR
                        "under18år" -> Vilkår.UNDER_18_ÅR
                        "bosatt" -> Vilkår.BOSATT_I_RIKET
                        "bormedsøker" -> Vilkår.BOR_MED_SØKER
                        "gift" -> Vilkår.GIFT_PARTNERSKAP
                        "partnerskap" -> Vilkår.GIFT_PARTNERSKAP
                        else -> throw IllegalArgumentException("Ukjent vilkår: $s")
                    }
                }.toList()
    }
}