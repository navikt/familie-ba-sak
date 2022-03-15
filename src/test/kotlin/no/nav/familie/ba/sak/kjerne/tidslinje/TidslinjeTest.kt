package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.TidslinjeUtenAvhengigheter
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.rangeTo
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.eksempler.Tidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.time.temporal.ChronoUnit

internal class TidslinjeTest {

    @Test
    fun test() {
        val søker = randomAktørId()
        val barn1 = randomAktørId()
        val barn2 = randomAktørId()
        val barn3 = randomAktørId()

        val januar2020 = YearMonth.of(2020, 1)
        val stønadTom = januar2020.plusYears(17)

        val behandling = lagBehandling()

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id, søker.aktivFødselsnummer(),
                listOf(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer(), barn3.aktivFødselsnummer())
            )

        val vilkårsvurdering = VilkårsvurderingBuilder(behandling = behandling)
            .forPerson(søker, januar2020)
            .medVilkår("---------------------", Vilkår.BOSATT_I_RIKET)
            .medVilkår("---------------------", Vilkår.LOVLIG_OPPHOLD)
            .forPerson(barn1, januar2020)
            .medVilkår("----------------     ", Vilkår.UNDER_18_ÅR)
            .medVilkår("   EEE NNNN  EEEE--- ", Vilkår.BOSATT_I_RIKET)
            .medVilkår("     EEENNEEEEEEEEE  ", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEEEEEEEEEE", Vilkår.BOR_MED_SØKER)
            .medVilkår("---------------------", Vilkår.GIFT_PARTNERSKAP)
            .forPerson(barn2, januar2020)
            .medVilkår("--------->", Vilkår.UNDER_18_ÅR)
            .medVilkår(" EEEE--EE>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEE>", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("EEEENNEEE>", Vilkår.BOR_MED_SØKER)
            .medVilkår("--------->", Vilkår.GIFT_PARTNERSKAP)
            .byggVilkårsvurdering()

        val kompetanser = listOf(
            Kompetanse(
                behandlingId = behandling.id,
                fom = januar2020,
                tom = stønadTom,
                barn = setOf(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer(), barn3.aktivFødselsnummer()),
                status = KompetanseStatus.OK,
                sekundærland = "NORGE"
            )
        )

        val tidslinjer = Tidslinjer(vilkårsvurdering, personopplysningGrunnlag, kompetanser)

        println("Barn: ${barn1.aktivFødselsnummer()}")
        tidslinjer.forBarn(barn1).kompetanseValidering.perioder().forEach { println(it) }
        println("Barn: ${barn2.aktivFødselsnummer()}")
        tidslinjer.forBarn(barn2).kompetanseValidering.perioder().forEach { println(it) }
        // assertEquals(3, erEøs.perioder().size)
    }

    @Test
    fun tidslinjeTest() {
        val tidslinje = TestKalkulenendeTidslinje()
        println(tidslinje.perioder())
    }
}

data class VilkårsvurderingBuilder(
    private val behandling: Behandling = lagBehandling(),
    private val vilkårsvurdering: Vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
) {
    fun forPerson(aktør: Aktør, startMåned: YearMonth = YearMonth.of(2020, 1)): PersonResultatBuilder {
        return PersonResultatBuilder(this, startMåned, aktør)
    }

    fun byggVilkårsvurdering() = vilkårsvurdering

    data class PersonResultatBuilder(
        val vilkårsvurderingBuilder: VilkårsvurderingBuilder,
        val startMåned: YearMonth,
        private val aktør: Aktør = randomAktørId(),
        private val vilkårsresultatTidslinjer: List<Tidslinje<VilkårRegelverkResultat>> = emptyList(),
    ) {

        fun medVilkår(v: String, vilkår: Vilkår): PersonResultatBuilder {
            return copy(vilkårsresultatTidslinjer = this.vilkårsresultatTidslinjer + parseVilkår(v, vilkår))
        }

        fun forPerson(aktør: Aktør, startMåned: YearMonth = YearMonth.of(2020, 1)): PersonResultatBuilder {
            return byggPerson().forPerson(aktør, startMåned)
        }

        fun byggVilkårsvurdering(): Vilkårsvurdering = byggPerson().byggVilkårsvurdering()

        private fun byggPerson(): VilkårsvurderingBuilder {

            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurderingBuilder.vilkårsvurdering,
                aktør = aktør
            )

            val vilkårresultater = vilkårsresultatTidslinjer
                .flatMap {
                    it.perioder()
                        .filter { it.innhold != null }
                        .map { periode ->
                            VilkårResultat(
                                personResultat = personResultat,
                                vilkårType = periode.innhold?.vilkår!!,
                                resultat = periode.innhold?.resultat!!,
                                vurderesEtter = periode.innhold?.regelverk,
                                periodeFom = periode.fom.tilFørsteDagIMåneden().tilLocalDate(),
                                periodeTom = periode.tom.tilSisteDagIMåneden().tilLocalDate(),
                                begrunnelse = "",
                                behandlingId = 0
                            )
                        }
                }

            personResultat.vilkårResultater.addAll(vilkårresultater)

            vilkårsvurderingBuilder.vilkårsvurdering.personResultater =
                vilkårsvurderingBuilder.vilkårsvurdering.personResultater +
                personResultat

            return vilkårsvurderingBuilder
        }

        private fun parseVilkår(periodeString: String, vilkår: Vilkår): Tidslinje<VilkårRegelverkResultat> {
            val perioder = periodeString
                .mapIndexed { index, tegn ->
                    val måned = startMåned.plusMonths(index.toLong())
                    if (erPeriode(tegn)) {
                        lagVilkårResultatPeriode(vilkår, tegn, Tidspunkt(måned))
                    } else if (tegn == '>') {
                        lagVilkårResultatPeriode(
                            vilkår,
                            periodeString[index - 1],
                            Tidspunkt.uendeligLengeTil(måned.minusMonths(1))
                        )
                    } else if (tegn == '<') {
                        lagVilkårResultatPeriode(
                            vilkår,
                            periodeString[index + 1],
                            Tidspunkt.uendeligLengeSiden(måned.plusMonths(1))
                        )
                    } else null
                }
                .filterNotNull()

            val tidslinje = object : TidslinjeUtenAvhengigheter<VilkårRegelverkResultat>() {
                override fun tidsrom(): Tidsrom = perioder.minOf { it.fom }..perioder.maxOf { it.tom }

                override fun kalkulerInnhold(tidspunkt: Tidspunkt): VilkårRegelverkResultat? {
                    return perioder.hentUtsnitt(tidspunkt)
                }
            }

            return tidslinje
        }

        private fun lagVilkårResultatPeriode(
            vilkår: Vilkår,
            tegn: Char,
            tidspunkt: Tidspunkt
        ): Periode<VilkårRegelverkResultat>? = if (erPeriode(tegn))
            Periode(
                tidspunkt.tilFørsteDagIMåneden(), tidspunkt.tilSisteDagIMåneden(),
                VilkårRegelverkResultat(
                    vilkår = vilkår,
                    resultat = finnResultat(tegn),
                    regelverk = finnRegelverk(tegn)
                )
            )
        else
            null

        private fun erPeriode(c: Char) =
            when (c) {
                '?', 'E', 'N', '-' -> true
                else -> false
            }

        private fun finnRegelverk(gjeldendeTegn: Char?): Regelverk? =
            when (gjeldendeTegn) {
                'E' -> Regelverk.EØS_FORORDNINGEN
                'N' -> Regelverk.NASJONALE_REGLER
                else -> null
            }

        private fun finnResultat(gjeldendeTegn: Char?) =
            when (gjeldendeTegn) {
                ' ' -> Resultat.IKKE_VURDERT
                '?' -> null
                else -> Resultat.OPPFYLT
            }
    }
}

class CharTidslinje(private val tegn: String, private val startMåned: YearMonth) : TidslinjeUtenAvhengigheter<Char>() {
    override fun tidsrom(): Tidsrom {
        val fom = when (tegn.first()) {
            '<' -> Tidspunkt.uendeligLengeSiden(startMåned)
            else -> Tidspunkt(startMåned)
        }

        val sluttMåned = startMåned.plusMonths(tegn.length.toLong())
        val tom = when (tegn.last()) {
            '>' -> Tidspunkt.uendeligLengeTil(sluttMåned)
            else -> Tidspunkt(sluttMåned)
        }

        return fom..tom
    }

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): Char? {
        val månederIMellom = ChronoUnit.MONTHS.between(
            tidsrom().start.tilLocalDate(),
            tidspunkt.tilFørsteDagIMåneden().tilLocalDate()
        ).toInt()
        return tegn[månederIMellom]
    }
}

class VilkårRegelverkResultatTidslinje(
    val vilkår: Vilkår,
    val charTidslinje: Tidslinje<Char>
) :
    KalkulerendeTidslinje<VilkårRegelverkResultat>(charTidslinje) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): VilkårRegelverkResultat? {
        val tegn = charTidslinje.hentUtsnitt(tidspunkt)

        return if (erPeriode(tegn))
            VilkårRegelverkResultat(
                vilkår = vilkår,
                resultat = finnResultat(tegn),
                regelverk = finnRegelverk(tegn)
            )
        else
            null
    }

    private fun erPeriode(c: Char?) =
        when (c) {
            '?', 'E', 'N', '-' -> true
            else -> false
        }

    private fun finnRegelverk(gjeldendeTegn: Char?): Regelverk? =
        when (gjeldendeTegn) {
            'E' -> Regelverk.EØS_FORORDNINGEN
            'N' -> Regelverk.NASJONALE_REGLER
            else -> null
        }

    private fun finnResultat(gjeldendeTegn: Char?) =
        when (gjeldendeTegn) {
            ' ' -> Resultat.IKKE_VURDERT
            '?' -> null
            else -> Resultat.OPPFYLT
        }
}

class TestStatiskTidslinje : TidslinjeUtenAvhengigheter<String>() {
    override fun tidsrom(): Tidsrom =
        Tidspunkt(YearMonth.of(2019, 3))..Tidspunkt(YearMonth.of(2025, 12))

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): String? {
        return "Hello"
    }
}

class TestKalkulenendeTidslinje(
    val forrigeTidslinje: Tidslinje<String> = TestStatiskTidslinje()
) : KalkulerendeTidslinje<Boolean>(forrigeTidslinje) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): Boolean {
        return forrigeTidslinje.hentUtsnitt(tidspunkt) == "Hello"
    }
}
