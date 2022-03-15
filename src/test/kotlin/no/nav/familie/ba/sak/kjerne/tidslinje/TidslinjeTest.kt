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

        val kompetanser2 = KompetanseBuilder(behandling = behandling, januar2020)
            .medKompetanse("---PPPPP--SSSSSS", barn1, barn2, barn3)
            .medKompetanse("                SSSSS", barn1)
            .medKompetanse("                PPPPP", barn2)
            .medKompetanse("                -----", barn2)
            .byggKompetanser()

        val tidslinjer = Tidslinjer(vilkårsvurdering, personopplysningGrunnlag, kompetanser2)

        println("Barn: ${barn1.aktivFødselsnummer()}")
        tidslinjer.forBarn(barn1).kompetanseValidering.perioder().forEach { println(it) }
        println("Barn: ${barn2.aktivFødselsnummer()}")
        tidslinjer.forBarn(barn2).kompetanseValidering.perioder().forEach { println(it) }
    }
}

class KompetanseBuilder(
    val behandling: Behandling = lagBehandling(),
    val startMåned: YearMonth = YearMonth.of(2020, 1)
) {
    val kompetanser: MutableList<Kompetanse> = mutableListOf()

    fun medKompetanse(k: String, vararg barn: Aktør): KompetanseBuilder {
        val charTidslinje = CharTidslinje(k, startMåned)
        val kompetanseTidslinje = KompetanseTidslinje(charTidslinje, behandling.id, barn.toList())

        kompetanseTidslinje.perioder()
            .filter { it.innhold != null }
            .map { it.innhold!!.copy(fom = it.fom.tilYearMonth(), tom = it.tom.tilYearMonth()) }
            .all { kompetanser.add(it) }

        return this
    }

    fun byggKompetanser(): Collection<Kompetanse> = kompetanser
}

data class VilkårsvurderingBuilder(
    private val behandling: Behandling = lagBehandling(),
    private val vilkårsvurdering: Vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
) {
    val personresultater: MutableSet<PersonResultat> = mutableSetOf()

    fun forPerson(aktør: Aktør, startMåned: YearMonth = YearMonth.of(2020, 1)): PersonResultatBuilder {
        return PersonResultatBuilder(this, startMåned, aktør)
    }

    fun byggVilkårsvurdering(): Vilkårsvurdering {
        vilkårsvurdering.personResultater = personresultater
        return vilkårsvurdering
    }

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

            val vilkårresultater = vilkårsresultatTidslinjer.flatMap {
                it.perioder()
                    .filter { it.innhold != null }
                    .map { periode -> periode.tilVilkårResultat(personResultat) }
            }

            personResultat.vilkårResultater.addAll(vilkårresultater)
            vilkårsvurderingBuilder.personresultater.add(personResultat)

            return vilkårsvurderingBuilder
        }

        private fun parseVilkår(periodeString: String, vilkår: Vilkår): Tidslinje<VilkårRegelverkResultat> {
            val charTidslinje = CharTidslinje(periodeString, startMåned)
            return VilkårRegelverkResultatTidslinje(vilkår, charTidslinje)
        }
    }
}

class CharTidslinje(private val tegn: String, private val startMåned: YearMonth) : TidslinjeUtenAvhengigheter<Char>() {
    override fun tidsrom(): Tidsrom {
        val fom = when (tegn.first()) {
            '<' -> Tidspunkt.uendeligLengeSiden(startMåned)
            else -> Tidspunkt(startMåned.plusMonths(1))
        }

        val sluttMåned = startMåned.plusMonths(tegn.length.toLong())
        val tom = when (tegn.last()) {
            '>' -> Tidspunkt.uendeligLengeTil(sluttMåned.minusMonths(1))
            else -> Tidspunkt(sluttMåned)
        }

        return fom..tom
    }

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): Char? {
        val månederIMellom = ChronoUnit.MONTHS.between(
            tidsrom().start.tilFørsteDagIMåneden().tilLocalDate(),
            tidspunkt.tilFørsteDagIMåneden().tilLocalDate()
        ).toInt()
        return tegn[månederIMellom]
    }
}

class KompetanseTidslinje(
    val charTidslinje: Tidslinje<Char>,
    val behandlingId: Long,
    val barn: List<Aktør>
) : KalkulerendeTidslinje<Kompetanse>(charTidslinje) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): Kompetanse? {
        val tegn = charTidslinje.hentUtsnitt(tidspunkt)
        val barnFnr = barn.map { it.aktivFødselsnummer() }.toSet()
        val kompetanseMal = Kompetanse(behandlingId = behandlingId, fom = null, tom = null, barn = barnFnr)
        return when (tegn) {
            '-' -> kompetanseMal
            'S' -> kompetanseMal.copy(status = KompetanseStatus.OK, sekundærland = "NORGE")
            'P' -> kompetanseMal.copy(status = KompetanseStatus.OK, primærland = "NORGE")
            else -> null
        }
    }
}

class VilkårRegelverkResultatTidslinje(
    val vilkår: Vilkår,
    val charTidslinje: Tidslinje<Char>
) :
    KalkulerendeTidslinje<VilkårRegelverkResultat>(charTidslinje) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): VilkårRegelverkResultat? {
        val tegn = charTidslinje.hentUtsnitt(tidspunkt)

        return when (tegn) {
            'E' -> VilkårRegelverkResultat(vilkår, Regelverk.EØS_FORORDNINGEN, Resultat.OPPFYLT)
            'N' -> VilkårRegelverkResultat(vilkår, Regelverk.NASJONALE_REGLER, Resultat.OPPFYLT)
            '-' -> VilkårRegelverkResultat(vilkår, null, Resultat.OPPFYLT)
            else -> null
        }
    }
}

fun Periode<VilkårRegelverkResultat>.tilVilkårResultat(personResultat: PersonResultat) =
    VilkårResultat(
        personResultat = personResultat,
        vilkårType = this.innhold?.vilkår!!,
        resultat = this.innhold?.resultat!!,
        vurderesEtter = this.innhold?.regelverk,
        periodeFom = this.fom.tilFørsteDagIMåneden().tilLocalDate(),
        periodeTom = this.tom.tilSisteDagIMåneden().tilLocalDate(),
        begrunnelse = "",
        behandlingId = 0
    )
