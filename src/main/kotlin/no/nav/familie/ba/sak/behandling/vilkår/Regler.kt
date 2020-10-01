package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrBostedsadresse.Companion.erSammeAdresse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.behandling.vilkår.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.behandling.vilkår.utfall.VilkårIkkeOppfyltÅrsak.*
import no.nav.familie.ba.sak.behandling.vilkår.utfall.VilkårKanskjeOppfyltÅrsak.LOVLIG_OPPHOLD_IKKE_MULIG_Å_FASTSETTE
import no.nav.familie.ba.sak.behandling.vilkår.utfall.VilkårOppfyltÅrsak.*
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.slåSammenOverlappendePerioder
import no.nav.familie.ba.sak.nare.Evaluering
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import java.time.Duration
import java.time.LocalDate

internal fun barnUnder18År(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering =
        if (faktaTilVilkårsvurdering.alder < 18)
            Evaluering.ja(ER_UNDER_18_ÅR)
        else
            Evaluering.nei(ER_IKKE_UNDER_18_ÅR)

internal fun søkerErMor(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering {
    val barn = faktaTilVilkårsvurdering.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    return when (søker.kjønn) {
        Kjønn.KVINNE -> Evaluering.ja(SØKER_ER_MOR)
        else -> Evaluering.nei(SØKER_ER_IKKE_MOR)
    }
}

internal fun barnBorMedSøker(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering {
    val barn = faktaTilVilkårsvurdering.personForVurdering
    val søker = barn.personopplysningGrunnlag.søker

    return when {
        erSammeAdresse(søker.bostedsadresse, barn.bostedsadresse) -> Evaluering.ja(BARNET_BOR_MED_MOR)
        else -> Evaluering.nei(BARNET_BOR_IKKE_MED_SØKER)
    }
}

internal fun bosattINorge(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering =
        /**
         * En person med registrert bostedsadresse er bosatt i Norge.
         * En person som mangler registrert bostedsadresse er utflyttet.
         * See: https://navikt.github.io/pdl/#_utflytting
         */
        faktaTilVilkårsvurdering.personForVurdering.bostedsadresse
                ?.let { Evaluering.ja(MOR_BOR_I_RIKET) }
        ?: Evaluering.nei(MOR_BOR_IKKE_I_RIKET)

internal fun lovligOpphold(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering {
    if (faktaTilVilkårsvurdering.behandlingOpprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE &&
        faktaTilVilkårsvurdering.personForVurdering.type == PersonType.BARN) {
        return Evaluering.ja(AUTOMATISK_VURDERING_BARN_LOVLIG_OPPHOLD)
    }

    val nåværendeMedlemskap = finnNåværendeMedlemskap(faktaTilVilkårsvurdering.personForVurdering.statsborgerskap)

    return when (finnSterkesteMedlemskap(nåværendeMedlemskap)) {
        Medlemskap.NORDEN -> Evaluering.ja(NORDISK_STATSBORGER)
        Medlemskap.EØS -> {
            sjekkLovligOppholdForEØSBorger(faktaTilVilkårsvurdering)
        }
        Medlemskap.TREDJELANDSBORGER -> {
            val nåværendeOpphold = faktaTilVilkårsvurdering.personForVurdering.opphold.singleOrNull { it.gjeldendeNå() }
            if (nåværendeOpphold == null || nåværendeOpphold.type == OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER) {
                Evaluering.nei(TREDJELANDSBORGER_UTEN_LOVLIG_OPPHOLD)
            } else Evaluering.ja(TREDJELANDSBORGER_MED_LOVLIG_OPPHOLD)
        }
        Medlemskap.UKJENT, Medlemskap.STATSLØS -> {
            val nåværendeOpphold = faktaTilVilkårsvurdering.personForVurdering.opphold.singleOrNull { it.gjeldendeNå() }
            if (nåværendeOpphold == null || nåværendeOpphold.type == OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER) {
                Evaluering.nei(STATSLØS)
            } else Evaluering.ja(UKJENT_STATSBORGERSKAP_MED_LOVLIG_OPPHOLD)
        }
        else -> Evaluering.kanskje(LOVLIG_OPPHOLD_IKKE_MULIG_Å_FASTSETTE)
    }
}

internal fun giftEllerPartnerskap(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Evaluering =
        when (faktaTilVilkårsvurdering.personForVurdering.sivilstand) {
            SIVILSTAND.UOPPGITT ->
                Evaluering.ja(BARN_MANGLER_SIVILSTAND)
            SIVILSTAND.GIFT, SIVILSTAND.REGISTRERT_PARTNER ->
                Evaluering.nei(BARN_ER_GIFT_ELLER_HAR_PARTNERSKAP)
            else -> Evaluering.ja(BARN_ER_IKKE_GIFT_ELLER_HAR_PARTNERSKAP)
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
        Evaluering.ja(EØS_MED_LØPENDE_ARBEIDSFORHOLD)
    } else {
        if (annenForelderRegistrert(faktaTilVilkårsvurdering)) {
            if (annenForelderBorMedMor(faktaTilVilkårsvurdering)) {
                with(statsborgerskapAnnenForelder(faktaTilVilkårsvurdering)) {
                    when {
                        contains(Medlemskap.NORDEN) -> Evaluering.ja(ANNEN_FORELDER_NORDISK)
                        contains(Medlemskap.EØS) -> {
                            if (personHarLøpendeArbeidsforhold(hentAnnenForelder(faktaTilVilkårsvurdering))) {
                                Evaluering.ja(ANNEN_FORELDER_EØS_MEN_MED_LØPENDE_ARBEIDSFORHOLD)
                            } else {
                                sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(faktaTilVilkårsvurdering,
                                                                                  EØS_MEDFORELDER_IKKE_I_ARBEID_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE,
                                                                                  EØS_MEDFORELDER_IKKE_I_ARBEID_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV)
                            }
                        }
                        contains(Medlemskap.TREDJELANDSBORGER) -> {
                            Evaluering.nei(EØS_MEDFORELDER_TREDJELANDSBORGER)
                        }
                        contains(Medlemskap.UKJENT) -> {
                            Evaluering.nei(EØS_MEDFORELDER_STATSLØS)
                        }
                        else -> {
                            Evaluering.nei(STATSBORGERSKAP_ANNEN_FORELDER_UKLART)
                        }
                    }
                }
            } else {
                sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(faktaTilVilkårsvurdering,
                                                                  EØS_BOR_IKKE_SAMMEN_MED_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE,
                                                                  EØS_BOR_IKKE_SAMMEN_MED_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV)
            }
        } else {
            sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(faktaTilVilkårsvurdering,
                                                              EØS_IKKE_REGISTRERT_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_ARBEIDSMENGDE,
                                                              EØS_IKKE_REGISTRERT_MEDFORELDER_OG_MOR_IKKE_INNFRIDD_BOTIDSKRAV
            )
        }
    }
}

private fun sjekkMorsHistoriskeBostedsadresseOgArbeidsforhold(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering,
                                                              arbeidsforholdAvslag: VilkårIkkeOppfyltÅrsak,
                                                              bosettelseAvslag: VilkårIkkeOppfyltÅrsak)
        : Evaluering {
    return if (morHarBoddINorgeSiste5År(faktaTilVilkårsvurdering)) {
        if (morHarJobbetINorgeSiste5År(faktaTilVilkårsvurdering)) {
            Evaluering.ja(MOR_BODD_OG_JOBBET_I_NORGE_SISTE_5_ÅR)
        } else {
            Evaluering.nei(arbeidsforholdAvslag)
        }
    } else {
        Evaluering.nei(bosettelseAvslag)
    }
}

fun personHarLøpendeArbeidsforhold(personForVurdering: Person): Boolean = personForVurdering.arbeidsforhold.any {
    it.periode?.tom == null || it.periode.tom >= LocalDate.now()
}

fun annenForelderRegistrert(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Boolean {
    val annenForelder = faktaTilVilkårsvurdering.personForVurdering.personopplysningGrunnlag.annenForelder
    return annenForelder != null
}

fun annenForelderBorMedMor(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Boolean {
    val annenForelder = hentAnnenForelder(faktaTilVilkårsvurdering)
    return erSammeAdresse(faktaTilVilkårsvurdering.personForVurdering.bostedsadresse, annenForelder.bostedsadresse)
}

fun statsborgerskapAnnenForelder(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): List<Medlemskap> {
    val annenForelder =
            hentAnnenForelder(faktaTilVilkårsvurdering)
    return finnNåværendeMedlemskap(annenForelder.statsborgerskap)
}

private fun hentAnnenForelder(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering): Person {
    return faktaTilVilkårsvurdering.personForVurdering.personopplysningGrunnlag.annenForelder
           ?: error("Persongrunnlag mangler annen forelder")
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
