package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.SnittTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.TidslinjeUtenAvhengigheter
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.komprimer
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.rangeTo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.eksempler.Tidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class TidslinjeTest {

    @Test
    fun testTidsrom() {
        val fom = Tidspunkt.uendeligLengeSiden(YearMonth.of(2020, 1))
        val tom = Tidspunkt.uendeligLengeTil(YearMonth.of(2020, 10))
        val tidsrom = fom..tom

        assertEquals(10, tidsrom.count())
        assertEquals(fom, tidsrom.first())
        assertEquals(tom, tidsrom.last())
    }

    @Test
    fun testCharTidsline() {
        val tegn = "---------------"
        val charTidslinje = CharTidslinje(tegn, YearMonth.of(2020, 1))
        assertEquals(tegn.length, charTidslinje.perioder().size)
        val perioder = charTidslinje.komprimer().perioder()
        perioder.forEach { println(it) }
        assertEquals(1, perioder.size)
    }

    @Test
    fun testUendeligTidslinjee() {
        val tegn = "<--->"
        val charTidslinje = CharTidslinje(tegn, YearMonth.of(2020, 1))
        charTidslinje.perioder().forEach { println(it) }
        assertEquals(tegn.length, charTidslinje.perioder().size)
        val perioder = charTidslinje.komprimer().perioder()
        perioder.forEach { println(it) }
        assertEquals(1, perioder.size)
    }

    @Test
    fun test() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        val januar2020 = YearMonth.of(2020, 1)
        val behandling = lagBehandling()

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            søker, barn1, barn2, barn3
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

        val kompetanser = KompetanseBuilder(behandling = behandling, januar2020)
            .medKompetanse("---SSSPP--SSPPSS", barn1, barn2, barn3)
            .medKompetanse("                SSSSS", barn1)
            .medKompetanse("                PPPPP", barn2)
            .medKompetanse("                -----", barn3)
            .byggKompetanser()

        val tidslinjer = Tidslinjer(
            vilkårsvurdering,
            personopplysningGrunnlag,
            kompetanser
        )

        tidslinjer.forBarn(barn2).barnetsVilkårsresultatTidslinjer.print()
        // tidslinjer.forBarn(barn2).erEøsTidslinje.print()
        // tidslinjer.forBarn(barn2).kompetanseTidslinje.print()

        println("Søker")
        tidslinjer.søkerOppfyllerVilkårTidslinje.print()
        println("Barn: ${barn1.aktør.aktivFødselsnummer()}")
        tidslinjer.forBarn(barn1).erEøsTidslinje.print()
        println("Barn: ${barn2.aktør.aktivFødselsnummer()}")

        tidslinjer.forBarn(barn2).erEøsTidslinje.print()
        tidslinjer.forBarn(barn2).kompetanseValideringTidslinje.perioder().size
        tidslinjer.forBarn(barn2).erSekundærlandTidslinje.perioder().size
    }
}

fun Iterable<Tidslinje<*>>.print() = this.forEach { it.print() }
fun Tidslinje<*>.print() {
    println("${this.tidsrom()} ${this.javaClass.name}")
    this.perioder().forEach { println(it) }
}

class KompetanseBuilder(
    val behandling: Behandling = lagBehandling(),
    val startMåned: YearMonth = YearMonth.of(2020, 1)
) {
    val kompetanser: MutableList<Kompetanse> = mutableListOf()

    fun medKompetanse(k: String, vararg barn: Person): KompetanseBuilder {
        val charTidslinje = CharTidslinje(k, startMåned).komprimer()
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

    fun forPerson(person: Person, startMåned: YearMonth = YearMonth.of(2020, 1)): PersonResultatBuilder {
        return PersonResultatBuilder(this, startMåned, person)
    }

    fun byggVilkårsvurdering(): Vilkårsvurdering {
        vilkårsvurdering.personResultater = personresultater
        return vilkårsvurdering
    }

    data class PersonResultatBuilder(
        val vilkårsvurderingBuilder: VilkårsvurderingBuilder,
        val startMåned: YearMonth,
        private val person: Person = tilfeldigPerson(),
        private val vilkårsresultatTidslinjer: List<Tidslinje<VilkårRegelverkResultat>> = emptyList(),
    ) {
        fun medVilkår(v: String, vilkår: Vilkår): PersonResultatBuilder {
            return copy(vilkårsresultatTidslinjer = this.vilkårsresultatTidslinjer + parseVilkår(v, vilkår))
        }

        fun forPerson(person: Person, startMåned: YearMonth = YearMonth.of(2020, 1)): PersonResultatBuilder {
            return byggPerson().forPerson(person, startMåned)
        }

        fun byggVilkårsvurdering(): Vilkårsvurdering = byggPerson().byggVilkårsvurdering()

        private fun byggPerson(): VilkårsvurderingBuilder {

            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurderingBuilder.vilkårsvurdering,
                aktør = person.aktør
            )

            val vilkårresultater = vilkårsresultatTidslinjer.flatMap {
                it.perioder()
                    .filter { it.innhold != null }
                    .flatMap { periode -> periode.tilVilkårResultater(personResultat) }
            }

            personResultat.vilkårResultater.addAll(vilkårresultater)
            vilkårsvurderingBuilder.personresultater.add(personResultat)

            return vilkårsvurderingBuilder
        }

        private fun parseVilkår(periodeString: String, vilkår: Vilkår): Tidslinje<VilkårRegelverkResultat> {
            val charTidslinje = CharTidslinje(periodeString, startMåned).komprimer()
            val vilkårRegelverkResultatTidslinje = VilkårRegelverkResultatTidslinje(vilkår, charTidslinje)
            // val charPeridoer = charTidslinje.perioder()
            // val vilkårPerioder = vilkårRegelverkResultatTidslinje.perioder()
            // vilkårRegelverkResultatTidslinje.print()

            return vilkårRegelverkResultatTidslinje
        }
    }
}

class CharTidslinje(private val tegn: String, private val startMåned: YearMonth) : TidslinjeUtenAvhengigheter<Char>() {
    override fun tidsrom(): Tidsrom {
        val fom = when (tegn.first()) {
            '<' -> Tidspunkt.uendeligLengeSiden(startMåned)
            else -> Tidspunkt.med(startMåned)
        }

        val sluttMåned = startMåned.plusMonths(tegn.length.toLong() - 1)
        val tom = when (tegn.last()) {
            '>' -> Tidspunkt.uendeligLengeTil(sluttMåned)
            else -> Tidspunkt.med(sluttMåned)
        }

        return fom..tom
    }

    override fun perioder(): Collection<Periode<Char>> {

        return tidsrom().mapIndexed { index, tidspunkt ->
            val c = when (index) {
                0 -> if (tegn[index] == '<') tegn[index + 1] else tegn[index]
                tegn.length - 1 -> if (tegn[index] == '>') tegn[index - 1] else tegn[index]
                else -> tegn[index]
            }
            Periode(tidspunkt.somFraOgMed(), tidspunkt.somTilOgMed(), c)
        }
    }
}

class KompetanseTidslinje(
    val charTidslinje: Tidslinje<Char>,
    val behandlingId: Long,
    val barn: List<Person>
) : SnittTidslinje<Kompetanse>(charTidslinje) {
    override fun beregnSnitt(tidspunkt: Tidspunkt): Kompetanse? {
        val tegn = charTidslinje.hentUtsnitt(tidspunkt)
        val barnFnr = barn.map { it.aktør.aktivFødselsnummer() }.toSet()
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
    SnittTidslinje<VilkårRegelverkResultat>(charTidslinje) {
    override fun beregnSnitt(tidspunkt: Tidspunkt): VilkårRegelverkResultat? {
        val tegn = charTidslinje.hentUtsnitt(tidspunkt)

        return when (tegn) {
            'E' -> VilkårRegelverkResultat(vilkår, Regelverk.EØS_FORORDNINGEN, Resultat.OPPFYLT)
            'N' -> VilkårRegelverkResultat(vilkår, Regelverk.NASJONALE_REGLER, Resultat.OPPFYLT)
            '-' -> VilkårRegelverkResultat(vilkår, null, Resultat.OPPFYLT)
            else -> null
        }
    }
}

fun Periode<VilkårRegelverkResultat>.tilVilkårResultater(personResultat: PersonResultat): Collection<VilkårResultat> {
    return listOf(
        VilkårResultat(
            personResultat = personResultat,
            vilkårType = this.innhold?.vilkår!!,
            resultat = this.innhold?.resultat!!,
            vurderesEtter = this.innhold?.regelverk,
            periodeFom = this.fom.tilLocalDateEllerNull(),
            periodeTom = this.tom.tilLocalDateEllerNull(),
            begrunnelse = "",
            behandlingId = 0
        )
    )
}

fun <T> Periode<T>.dekomponerUendelig(): Iterable<Periode<T>> {
    val fraUendelig = if (this.fom.erUendeligLengeSiden())
        this.copy(fom = this.fom.forrige(), tom = this.fom.forrige().somEndelig())
    else
        null

    val endelig = this.copy(fom.somEndelig(), tom.somEndelig())

    val tilUendelig = if (this.tom.erUendeligLengeTil())
        this.copy(fom = this.tom.neste().somEndelig(), tom = this.tom.neste())
    else
        null

    return listOf(fraUendelig, endelig, tilUendelig).filterNotNull()
}
