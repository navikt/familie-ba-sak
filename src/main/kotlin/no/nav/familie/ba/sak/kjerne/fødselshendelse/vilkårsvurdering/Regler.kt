package no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.isBetween
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.utfall.VilkårKanskjeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.utfall.VilkårOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.sisteAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate

interface Vilkårsregel {

    fun vurder(): Evaluering
}

data class VurderPersonErBosattIRiket(
    val adresser: List<GrBostedsadresse>,
    val vurderFra: LocalDate
) : Vilkårsregel {

    override fun vurder(): Evaluering {
        if (adresser.any { !it.harGyldigFom() }) {
            val person = adresser.first().person
            secureLogger.info(
                "Har ugyldige adresser på person (${person?.personIdent?.ident}, ${person?.type}): ${
                adresser.filter { !it.harGyldigFom() }
                    .map { "(${it.periode?.fom}, ${it.periode?.tom}): ${it.toSecureString()}" }
                }"
            )
        }

        val adresserMedGyldigFom = adresser.filter { it.harGyldigFom() }

        /**
         * En person med registrert bostedsadresse er bosatt i Norge.
         * En person som mangler registrert bostedsadresse er utflyttet.
         * See: https://navikt.github.io/pdl/#_utflytting
         */
        return if (adresserMedGyldigFom.isNotEmpty() && erPersonBosattFraVurderingstidspunktet(
                adresserMedGyldigFom,
                vurderFra
            )
        )
            Evaluering.oppfylt(VilkårOppfyltÅrsak.BOR_I_RIKET)
        else Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.BOR_IKKE_I_RIKET)
    }

    private fun erPersonBosattFraVurderingstidspunktet(adresser: List<GrBostedsadresse>, vurderFra: LocalDate) =
        hentMaxAvstandAvDagerMellomPerioder(
            adresser.mapNotNull { it.periode },
            vurderFra,
            LocalDate.now()
        ) == 0L

    companion object {

        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

data class VurderBarnErUnder18(
    val alder: Int
) : Vilkårsregel {

    override fun vurder(): Evaluering =
        if (alder < 18) Evaluering.oppfylt(VilkårOppfyltÅrsak.ER_UNDER_18_ÅR)
        else Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.ER_IKKE_UNDER_18_ÅR)
}

data class VurderBarnErBosattMedSøker(
    val søkerAdresser: List<GrBostedsadresse>,
    val barnAdresser: List<GrBostedsadresse>
) : Vilkårsregel {

    override fun vurder(): Evaluering {
        return if (barnAdresser.isNotEmpty() && barnAdresser.all {
            søkerAdresser.any { søkerAdresse ->
                val søkerAdresseFom = søkerAdresse.periode?.fom ?: TIDENES_MORGEN
                val søkerAdresseTom = søkerAdresse.periode?.tom ?: TIDENES_ENDE

                val barnAdresseFom = it.periode?.fom ?: TIDENES_MORGEN
                val barnAdresseTom = it.periode?.tom ?: TIDENES_ENDE

                søkerAdresseFom.isSameOrBefore(barnAdresseFom) &&
                    søkerAdresseTom.isSameOrAfter(barnAdresseTom) &&
                    GrBostedsadresse.erSammeAdresse(søkerAdresse, it)
            }
        }
        ) Evaluering.oppfylt(VilkårOppfyltÅrsak.BARNET_BOR_MED_MOR)
        else Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.BARNET_BOR_IKKE_MED_MOR)
    }
}

data class VurderBarnErUgift(
    val sivilstander: List<GrSivilstand>
) : Vilkårsregel {

    override fun vurder(): Evaluering {
        val sivilstanderMedGyldigFom = sivilstander.filter { it.harGyldigFom() }

        return when {
            sivilstanderMedGyldigFom.singleOrNull { it.type == SIVILSTAND.UOPPGITT } != null ->
                Evaluering.oppfylt(VilkårOppfyltÅrsak.BARN_MANGLER_SIVILSTAND)
            sivilstanderMedGyldigFom.any { it.type == SIVILSTAND.GIFT || it.type == SIVILSTAND.REGISTRERT_PARTNER } ->
                Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.BARN_ER_GIFT_ELLER_HAR_PARTNERSKAP)
            else -> Evaluering.oppfylt(VilkårOppfyltÅrsak.BARN_ER_IKKE_GIFT_ELLER_HAR_PARTNERSKAP)
        }
    }
}

data class VurderPersonHarLovligOpphold(
    val fakta: String = "Alltid oppfylt inntil videre"
) : Vilkårsregel {

    override fun vurder(): Evaluering = Evaluering.oppfylt(VilkårOppfyltÅrsak.NORDISK_STATSBORGER)
}

// Fra gammel implementasjon
internal fun lovligOpphold(person: Person): Evaluering {
    if (person.type == PersonType.BARN) {
        return Evaluering.oppfylt(VilkårOppfyltÅrsak.AUTOMATISK_VURDERING_BARN_LOVLIG_OPPHOLD)
    }

    val nåværendeMedlemskap = finnNåværendeMedlemskap(person.statsborgerskap)

    return when (finnSterkesteMedlemskap(nåværendeMedlemskap)) {
        Medlemskap.NORDEN -> Evaluering.oppfylt(VilkårOppfyltÅrsak.NORDISK_STATSBORGER)
        Medlemskap.EØS -> {
            sjekkLovligOppholdForEØSBorger(person)
        }
        Medlemskap.TREDJELANDSBORGER -> {
            val nåværendeOpphold = person.opphold.singleOrNull { it.gjeldendeNå() }
            if (nåværendeOpphold == null || nåværendeOpphold.type == OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER) {
                Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.TREDJELANDSBORGER_UTEN_LOVLIG_OPPHOLD)
            } else Evaluering.oppfylt(VilkårOppfyltÅrsak.TREDJELANDSBORGER_MED_LOVLIG_OPPHOLD)
        }
        Medlemskap.UKJENT, Medlemskap.STATSLØS -> {
            val nåværendeOpphold = person.opphold.singleOrNull { it.gjeldendeNå() }
            if (nåværendeOpphold == null || nåværendeOpphold.type == OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER) {
                Evaluering.ikkeOppfylt(VilkårIkkeOppfyltÅrsak.STATSLØS)
            } else Evaluering.oppfylt(VilkårOppfyltÅrsak.UKJENT_STATSBORGERSKAP_MED_LOVLIG_OPPHOLD)
        }
        else -> Evaluering.ikkeVurdert(VilkårKanskjeOppfyltÅrsak.LOVLIG_OPPHOLD_IKKE_MULIG_Å_FASTSETTE)
    }
}

fun finnNåværendeMedlemskap(statsborgerskap: List<GrStatsborgerskap>?): List<Medlemskap> =
    statsborgerskap?.filter {
        it.gyldigPeriode?.fom?.isBefore(LocalDate.now()) ?: true &&
            it.gyldigPeriode?.tom?.isAfter(LocalDate.now()) ?: true
    }
        ?.map { it.medlemskap } ?: emptyList()

fun finnSterkesteMedlemskap(medlemskap: List<Medlemskap>): Medlemskap? {
    return with(medlemskap) {
        when {
            contains(Medlemskap.NORDEN) -> Medlemskap.NORDEN
            contains(Medlemskap.EØS) -> Medlemskap.EØS
            contains(Medlemskap.TREDJELANDSBORGER) -> Medlemskap.TREDJELANDSBORGER
            contains(Medlemskap.STATSLØS) -> Medlemskap.STATSLØS
            contains(Medlemskap.UKJENT) -> Medlemskap.UKJENT
            else -> null
        }
    }
}

private fun sjekkLovligOppholdForEØSBorger(person: Person): Evaluering {
    return if (personHarLøpendeArbeidsforhold(person)) {
        Evaluering.oppfylt(VilkårOppfyltÅrsak.EØS_MED_LØPENDE_ARBEIDSFORHOLD)
    } else {
        if (annenForelderRegistrert(person)) {
            if (annenForelderBorMedMor(person)) {
                val annenForelderSterkerteStatsborgerskap =
                    finnSterkesteMedlemskap(statsborgerskapAnnenForelder(person))
                        ?: throw error("Ukjent medlemskap annen forelder")
                sjekkLovligOppholdForEØSBorgerSomBorMedPartner(annenForelderSterkerteStatsborgerskap, person)
            } else {
                sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(
                    person,
                    VilkårIkkeOppfyltÅrsak.EØS_BOR_IKKE_SAMMEN_MED_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE,
                    VilkårIkkeOppfyltÅrsak.EØS_BOR_IKKE_SAMMEN_MED_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV
                )
            }
        } else {
            sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(
                person,
                VilkårIkkeOppfyltÅrsak.EØS_IKKE_REGISTRERT_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE,
                VilkårIkkeOppfyltÅrsak.EØS_IKKE_REGISTRERT_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV
            )
        }
    }
}

private fun sjekkLovligOppholdForEØSBorgerSomBorMedPartner(
    annenForelderSterkesteStatsborgerskap: Medlemskap,
    person: Person
) =
    when (annenForelderSterkesteStatsborgerskap) {
        Medlemskap.NORDEN -> Evaluering.oppfylt(VilkårOppfyltÅrsak.ANNEN_FORELDER_NORDISK)
        Medlemskap.EØS -> {
            if (personHarLøpendeArbeidsforhold(hentAnnenForelder(person))) {
                Evaluering.oppfylt(VilkårOppfyltÅrsak.ANNEN_FORELDER_EØS_MEN_MED_LØPENDE_ARBEIDSFORHOLD)
            } else {
                sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(
                    person,
                    VilkårIkkeOppfyltÅrsak.EØS_MEDFORELDER_IKKE_I_ARBEID_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE,
                    VilkårIkkeOppfyltÅrsak.EØS_MEDFORELDER_IKKE_I_ARBEID_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV
                )
            }
        }
        Medlemskap.TREDJELANDSBORGER -> {
            sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(
                person,
                VilkårIkkeOppfyltÅrsak.EØS_MEDFORELDER_TREDJELANDSBORGER,
                VilkårIkkeOppfyltÅrsak.EØS_MEDFORELDER_TREDJELANDSBORGER
            )
        }
        Medlemskap.STATSLØS -> {
            sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(
                person,
                VilkårIkkeOppfyltÅrsak.EØS_MEDFORELDER_STATSLØS,
                VilkårIkkeOppfyltÅrsak.EØS_MEDFORELDER_STATSLØS
            )
        }
        Medlemskap.UKJENT -> {
            sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(
                person,
                VilkårIkkeOppfyltÅrsak.STATSBORGERSKAP_ANNEN_FORELDER_UKLART,
                VilkårIkkeOppfyltÅrsak.STATSBORGERSKAP_ANNEN_FORELDER_UKLART
            )
        }
    }

private fun sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(
    person: Person,
    arbeidsforholdAvslag: VilkårIkkeOppfyltÅrsak,
    bosettelseAvslag: VilkårIkkeOppfyltÅrsak
): Evaluering {
    return if (morHarBoddINorgeSiste5År(person)) {
        if (morHarJobbetINorgeSiste5År(person)) {
            Evaluering.oppfylt(VilkårOppfyltÅrsak.MOR_BODD_OG_JOBBET_I_NORGE_SISTE_5_ÅR)
        } else {
            Evaluering.ikkeOppfylt(arbeidsforholdAvslag)
        }
    } else {
        Evaluering.ikkeOppfylt(bosettelseAvslag)
    }
}

fun personHarLøpendeArbeidsforhold(personForVurdering: Person): Boolean = personForVurdering.arbeidsforhold.any {
    it.periode?.tom == null || it.periode.tom >= LocalDate.now()
}

fun annenForelderRegistrert(person: Person): Boolean {
    val annenForelder = person.personopplysningGrunnlag.annenForelder
    return annenForelder != null
}

fun annenForelderBorMedMor(person: Person): Boolean {
    val annenForelder = hentAnnenForelder(person)
    return GrBostedsadresse.erSammeAdresse(
        person.bostedsadresser.sisteAdresse(),
        annenForelder.bostedsadresser.sisteAdresse()
    )
}

fun statsborgerskapAnnenForelder(person: Person): List<Medlemskap> {
    val annenForelder =
        hentAnnenForelder(person)
    return finnNåværendeMedlemskap(annenForelder.statsborgerskap)
}

private fun hentAnnenForelder(person: Person): Person {
    return person.personopplysningGrunnlag.annenForelder
        ?: error("Persongrunnlag mangler annen forelder")
}

fun morHarBoddINorgeSiste5År(person: Person): Boolean {
    val perioder = person.bostedsadresser.mapNotNull {
        it.periode
    }

    if (perioder.any { it.fom == null }) {
        return false
    }

    return hentMaxAvstandAvDagerMellomPerioder(perioder, LocalDate.now().minusYears(5), LocalDate.now()) <= 0
}

fun morHarJobbetINorgeSiste5År(person: Person): Boolean {
    val perioder = person.arbeidsforhold.mapNotNull {
        it.periode
    }

    if (perioder.any { it.fom == null }) {
        return false
    }

    return hentMaxAvstandAvDagerMellomPerioder(perioder, LocalDate.now().minusYears(5), LocalDate.now()) <= 90
}

private fun hentMaxAvstandAvDagerMellomPerioder(
    perioder: List<DatoIntervallEntitet>,
    fom: LocalDate,
    tom: LocalDate
): Long {
    val perioderMedTilkobletTom =
        perioder.sortedBy { it.fom }
            .fold(mutableListOf()) { acc: MutableList<DatoIntervallEntitet>, datoIntervallEntitet: DatoIntervallEntitet ->
                if (acc.isNotEmpty() && acc.last().tom == null) {
                    val sisteDatoIntervall = acc.last().copy(
                        tom = datoIntervallEntitet.fom?.minusDays(1)
                    )

                    acc.removeLast()
                    acc.add(sisteDatoIntervall)
                }

                acc.add(datoIntervallEntitet)
                acc.sortBy { it.fom }
                acc
            }
            .toList()

    val perioderInnenAngittTidsrom =
        perioderMedTilkobletTom.filter {
            it.tom == null ||
                fom.isBetween(
                    Periode(
                        fom = it.fom!!,
                        tom = it.tom
                    )
                ) ||
                tom.isBetween(
                    Periode(
                        fom = it.fom,
                        tom = it.tom
                    )
                ) ||
                it.fom >= fom && it.tom <= tom
        }

    if (perioderInnenAngittTidsrom.isEmpty()) return Duration.between(fom.atStartOfDay(), tom.atStartOfDay()).toDays()

    val defaultAvstand = if (perioderInnenAngittTidsrom.first().fom!!.isAfter(fom))
        Duration.between(
            fom.atStartOfDay(),
            perioderInnenAngittTidsrom.first().fom!!.atStartOfDay()
        )
            .toDays()
    else if (perioderInnenAngittTidsrom.last().tom != null && perioderInnenAngittTidsrom.last().tom!!.isBefore(tom))
        Duration.between(
            perioderInnenAngittTidsrom.last().tom!!.atStartOfDay(),
            tom.atStartOfDay()
        ).toDays()
    else 0L

    return perioderInnenAngittTidsrom
        .zipWithNext()
        .fold(defaultAvstand) { maksimumAvstand, pairs ->
            val avstand =
                Duration.between(pairs.first.tom!!.atStartOfDay().plusDays(1), pairs.second.fom!!.atStartOfDay())
                    .toDays()
            maxOf(avstand, maksimumAvstand)
        }
}
