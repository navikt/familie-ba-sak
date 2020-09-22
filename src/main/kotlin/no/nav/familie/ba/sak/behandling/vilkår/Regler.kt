package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrBostedsadresse.Companion.erSammeAdresse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingMetrics.Companion.økTellerForLovligOpphold
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.slåSammenOverlappendePerioder
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.nare.core.evaluations.Evaluering
import java.time.Duration
import java.time.LocalDate

internal fun barnUnder18År(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering =
        if (faktaTilVilkårsvurdering.alder < 18)
            Evaluering.ja("Barn er under 18 år")
        else
            Evaluering.nei("Barn er ikke under 18 år")

internal fun harEnSøker(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering {
    val barn = faktaTilVilkårsvurdering.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    return if (søker.size == 1)
        Evaluering.ja("Søknad har eksakt en søker")
    else
        Evaluering.nei("Søknad har mer enn en eller ingen søker")
}

internal fun søkerErMor(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering {
    val barn = faktaTilVilkårsvurdering.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    return when {
        søker.isEmpty() -> Evaluering.nei("Ingen søker")
        søker.first().kjønn == Kjønn.KVINNE -> Evaluering.ja("Søker er mor")
        else -> Evaluering.nei("Søker er ikke mor")
    }
}

internal fun barnBorMedSøker(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering {
    val barn = faktaTilVilkårsvurdering.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    return when {
        søker.isEmpty() -> Evaluering.nei("Ingen søker")
        erSammeAdresse(søker.first().bostedsadresse, barn.bostedsadresse) -> Evaluering.ja("Barnet bor med mor")
        else -> Evaluering.nei("Barnet bor ikke med mor")
    }
}

internal fun bosattINorge(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering =
        /**
         * En person med registrert bostedsadresse er bosatt i Norge.
         * En person som mangler registrert bostedsadresse er utflyttet.
         * See: https://navikt.github.io/pdl/#_utflytting
         */
        faktaTilVilkårsvurdering.personForVurdering.bostedsadresse
                ?.let { Evaluering.ja("Mor er bosatt i riket") }
        ?: Evaluering.nei("Mor er ikke bosatt i riket")

internal fun lovligOpphold(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering {
    if (faktaTilVilkårsvurdering.behandlingOpprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE &&
        faktaTilVilkårsvurdering.personForVurdering.type == PersonType.BARN) {
        return Evaluering.ja("Ikke separat oppholdsvurdering for barnet ved automatisk vedtak.")
    }

    val nåværendeMedlemskap = finnNåværendeMedlemskap(faktaTilVilkårsvurdering.personForVurdering.statsborgerskap)

    return when (finnSterkesteMedlemskap(nåværendeMedlemskap)) {
            Medlemskap.NORDEN -> Evaluering.ja("Er nordisk statsborger.")
            Medlemskap.EØS -> {
                sjekkLovligOppholdForEØSBorger(faktaTilVilkårsvurdering)
            }
            Medlemskap.TREDJELANDSBORGER -> {
                val nåværendeOpphold = faktaTilVilkårsvurdering.personForVurdering.opphold.singleOrNull { it.gjeldendeNå() }
                if (nåværendeOpphold == null || nåværendeOpphold.type == OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER) {
                    økTellerForLovligOpphold(LovligOppholdUtfall.TREDJELANDSBORGER)
                    Evaluering.nei(LovligOppholdUtfall.TREDJELANDSBORGER.begrunnelseForOppgave)
                } else Evaluering.ja("Er tredjelandsborger med lovlig opphold")
            }
            Medlemskap.UKJENT, Medlemskap.STATSLØS -> {
                val nåværendeOpphold = faktaTilVilkårsvurdering.personForVurdering.opphold.singleOrNull { it.gjeldendeNå() }
                if (nåværendeOpphold == null || nåværendeOpphold.type == OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER) {
                    økTellerForLovligOpphold(LovligOppholdUtfall.STATSLØS)
                    Evaluering.nei(LovligOppholdUtfall.STATSLØS.begrunnelseForOppgave)
                } else Evaluering.ja("Er statsløs eller mangler statsborgerskap med lovlig opphold")
            }
            else -> Evaluering.kanskje("Kan ikke avgjøre om personen har lovlig opphold.")
    }
}

internal fun giftEllerPartnerskap(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering =
        when (faktaTilVilkårsvurdering.personForVurdering.sivilstand) {
            SIVILSTAND.UOPPGITT ->
                if (faktaTilVilkårsvurdering.behandlingOpprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE)
                    Evaluering.ja("Person mangler informasjon om sivilstand.")
                else
                    Evaluering.kanskje("Person mangler informasjon om sivilstand.")
            SIVILSTAND.GIFT, SIVILSTAND.REGISTRERT_PARTNER ->
                Evaluering.nei("Person er gift eller har registrert partner")
            else -> Evaluering.ja("Person er ikke gift eller har registrert partner")
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


fun Evaluering.toJson(): String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

private fun sjekkLovligOppholdForEØSBorger(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering {
    return if (personHarLøpendeArbeidsforhold(faktaTilVilkårsvurdering.personForVurdering)) {
        Evaluering.ja("Mor er EØS-borger, men har et løpende arbeidsforhold i Norge.")
    } else {
        if (annenForelderRegistrert(faktaTilVilkårsvurdering)) {
            if (annenForelderBorMedMor(faktaTilVilkårsvurdering)) {
                with(statsborgerskapAnnenForelder(faktaTilVilkårsvurdering)) {
                    when {
                        contains(Medlemskap.NORDEN) -> Evaluering.ja("Annen forelder er norsk eller nordisk statsborger.")
                        contains(Medlemskap.EØS) -> {
                            if (personHarLøpendeArbeidsforhold(hentAnnenForelder(faktaTilVilkårsvurdering).first())) {
                                Evaluering.ja("Annen forelder er fra EØS, men har et løpende arbeidsforhold i Norge.")
                            } else {
                                sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(faktaTilVilkårsvurdering,
                                                                                  LovligOppholdUtfall.EØS_MEDFORELDER_IKKE_I_ARBEID_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE,
                                                                                  LovligOppholdUtfall.EØS_MEDFORELDER_IKKE_I_ARBEID_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV)
                            }
                        }
                        contains(Medlemskap.TREDJELANDSBORGER) -> {
                            økTellerForLovligOpphold(LovligOppholdUtfall.EØS_MEDFORELDER_TREDJELANDSBORGER)
                            Evaluering.nei(LovligOppholdUtfall.EØS_MEDFORELDER_TREDJELANDSBORGER.begrunnelseForOppgave)
                        }
                        contains(Medlemskap.UKJENT) -> {
                            økTellerForLovligOpphold(LovligOppholdUtfall.EØS_MEDFORELDER_STATSLØS)
                            Evaluering.nei(LovligOppholdUtfall.EØS_MEDFORELDER_STATSLØS.begrunnelseForOppgave)
                        }
                        else -> {
                            Evaluering.nei("Statsborgerskap for annen forelder kan ikke avgjøres.")
                        }
                    }
                }
            } else {
                sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(faktaTilVilkårsvurdering,
                                                                  LovligOppholdUtfall.EØS_BOR_IKKE_SAMMEN_MED_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE,
                                                                  LovligOppholdUtfall.EØS_BOR_IKKE_SAMMEN_MED_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV)
            }
        } else {
            sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(faktaTilVilkårsvurdering,
                                                              LovligOppholdUtfall.EØS_IKKE_REGISTRERT_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE,
                                                              LovligOppholdUtfall.EØS_IKKE_REGISTRERT_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV
            )
        }
    }
}

private fun sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering,
                                                              arbeidsforholdAvslag: LovligOppholdUtfall,
                                                              bosettelseAvslag: LovligOppholdUtfall)
        : Evaluering {
    return if (morHarBoddINorgeSiste5År(faktaTilVilkårsvurdering)) {
        if (morHarJobbetINorgeSiste5År(faktaTilVilkårsvurdering)) {
            Evaluering.ja("Mor har bodd og jobbet i Norge siste 5 år.")
        } else {
            økTellerForLovligOpphold(arbeidsforholdAvslag)
            Evaluering.nei(arbeidsforholdAvslag.begrunnelseForOppgave)
        }
    } else {
        økTellerForLovligOpphold(bosettelseAvslag)
        Evaluering.nei(bosettelseAvslag.begrunnelseForOppgave)
    }
}

fun personHarLøpendeArbeidsforhold(personForVurdering: Person): Boolean = personForVurdering.arbeidsforhold.any {
    it.periode?.tom == null || it.periode.tom >= LocalDate.now()
}

fun annenForelderRegistrert(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Boolean {
    val annenForelder = hentAnnenForelder(faktaTilVilkårsvurdering).firstOrNull()
    return annenForelder != null
}

fun annenForelderBorMedMor(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Boolean {
    val annenForelder = hentAnnenForelder(faktaTilVilkårsvurdering).first()
    return erSammeAdresse(faktaTilVilkårsvurdering.personForVurdering.bostedsadresse, annenForelder.bostedsadresse)
}

fun statsborgerskapAnnenForelder(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): List<Medlemskap> {
    val annenForelder =
            hentAnnenForelder(faktaTilVilkårsvurdering).first()
    return finnNåværendeMedlemskap(annenForelder.statsborgerskap)
}

private fun hentAnnenForelder(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering) = faktaTilVilkårsvurdering.personForVurdering.personopplysningGrunnlag.personer.filter {
    it.type == PersonType.ANNENPART
}

fun morHarBoddINorgeSiste5År(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Boolean {
    val perioder = faktaTilVilkårsvurdering.personForVurdering.bostedsadresseperiode.mapNotNull {
        it.periode
    }

    if (perioder.any { it.fom == null }) {
        return false
    }

    return hentMaxAvstandAvDagerMellomPerioder(perioder, LocalDate.now().minusYears(5), LocalDate.now()) <= 0
}

fun morHarJobbetINorgeSiste5År(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Boolean {
    val perioder = faktaTilVilkårsvurdering.personForVurdering.arbeidsforhold.mapNotNull {
        it.periode
    }

    if (perioder.any { it.fom == null }) {
        return false
    }

    return hentMaxAvstandAvDagerMellomPerioder(perioder, LocalDate.now().minusYears(5), LocalDate.now()) <= 90
}

private fun hentMaxAvstandAvDagerMellomPerioder(perioder: List<DatoIntervallEntitet>,
                                                fom: LocalDate,
                                                tom: LocalDate): Long {
    val mutablePerioder = perioder.toMutableList().apply {
        addAll(listOf(
                DatoIntervallEntitet(
                        fom.minusDays(1),
                        fom.minusDays(1)),
                DatoIntervallEntitet(
                        tom.plusDays(1),
                        tom.plusDays(1))))
    }

    val sammenslåttePerioder = slåSammenOverlappendePerioder(mutablePerioder).sortedBy { it.fom }

    return sammenslåttePerioder.zipWithNext().fold(0L) { maksimumAvstand, pairs ->
        val avstand = Duration.between(pairs.first.tom!!.atStartOfDay().plusDays(1), pairs.second.fom!!.atStartOfDay()).toDays()
        if (avstand > maksimumAvstand) {
            avstand
        } else {
            maksimumAvstand
        }
    }
}

enum class LovligOppholdUtfall(val begrunnelseForOppgave: String, val begrunnelseForMetrikker: String) {
    TREDJELANDSBORGER(
            "Mor har ikke lovlig opphold - tredjelandsborger.",
            "Mor tredjelandsborger"
    ),
    STATSLØS(
            "Mor har ikke lovlig opphold - er statsløs eller mangler statsborgerskap.",
            "Mor statsløs eller mangler statsborgerskap"
    ),
    EØS_IKKE_REGISTRERT_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV(
            "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Det er ikke registrert medforelder på barnet. Mor har ikke hatt bostedsadresse i Norge i mer enn fem år.",
            "Mor EØS. Ikke arb. MF ikke reg. Mor ikke bosatt 5 år"
    ),
    EØS_IKKE_REGISTRERT_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE(
            "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Det er ikke registrert medforelder på barnet. Mor har ikke hatt arbeidsforhold i Norge de siste fem årene.",
            "Mor EØS. Ikke arb. MF ikke reg. Mor ikke arbeid 5 år"
    ),
    EØS_BOR_IKKE_SAMMEN_MED_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV(
            "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Barnets mor og medforelder har ikke felles bostedsadresse. Mor har ikke hatt bostedsadresse i Norge i mer enn fem år.",
            "Mor EØS. Ikke arb. Bor ikke med MF. Mor ikke bosatt 5 år"
    ),
    EØS_BOR_IKKE_SAMMEN_MED_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE(
            "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Barnets mor og medforelder har ikke felles bostedsadresse. Mor har ikke hatt arbeidsforhold i Norge de siste fem årene.",
            "Mor EØS. Ikke arb. Bor ikke med MF. Mor ikke arbeid 5 år"
    ),
    EØS_MEDFORELDER_TREDJELANDSBORGER(
            "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Medforelder er tredjelandsborger.",
            "Mor EØS. Ikke arb. MF tredjelandsborger"
    ),
    EØS_MEDFORELDER_STATSLØS(
            "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Medforelder er statsløs.",
            "Mor EØS. Ikke arb. MF statsløs"
    ),
    EØS_MEDFORELDER_IKKE_I_ARBEID_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV(
            "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Medforelder er ikke registrert med arbeidsforhold i Norge. Mor har ikke hatt bostedsadresse i Norge i mer enn fem år.",
            "Mor EØS. Ikke arb. MF EØS ikke arbeid. Mor ikke bosatt 5 år"
    ),
    EØS_MEDFORELDER_IKKE_I_ARBEID_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE(
            "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Medforelder er ikke registrert med arbeidsforhold i Norge. Mor har ikke hatt arbeidsforhold i Norge de siste fem årene.",
            "Mor EØS. Ikke arb. MF EØS ikke arbeid. Mor ikke arbeid 5 år"
    ),
}
