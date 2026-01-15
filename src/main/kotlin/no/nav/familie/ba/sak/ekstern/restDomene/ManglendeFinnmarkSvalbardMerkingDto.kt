package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.Utils.tilEtterfølgendePar
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import no.nav.familie.kontrakter.ba.finnmarkstillegg.kommuneErIFinnmarkEllerNordTroms
import no.nav.familie.kontrakter.ba.tss.SamhandlerAdresse
import no.nav.familie.kontrakter.ba.tss.SamhandlerInfo
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate
import kotlin.collections.map

private val cutOffTomDatoForVisningAvManglendeMerkinger = LocalDate.of(2025, 9, 30)

data class ManglendeFinnmarkSvalbardMerkingDto(
    val ident: String,
    val manglendeFinnmarkSvalbardMerkingPerioder: List<ManglendeFinnmarkSvalbardMerkingPeriodeDto>,
)

data class ManglendeFinnmarkSvalbardMerkingPeriodeDto(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

fun SamhandlerInfo?.tilManglendeFinnmarkmerkingPerioder(personResultater: Set<PersonResultat>?): ManglendeFinnmarkSvalbardMerkingDto? {
    if (this == null || personResultater == null) return null

    val personResultat = personResultater.singleOrNull()

    if (personResultat == null) return null

    val bosattIRiketVilkårTidslinje =
        personResultat.vilkårResultater
            .filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BOSATT_I_RIKET && vilkårResultat.periodeFom != null }
            .tilTidslinje()

    val finnmarkEllerNordTromsOppholdTidslinjeForInstitusjon = this.adresser.tilFinnmmarkEllerNordTromsOppholdTidslinje()

    val perioderMedManglendeFinnmarkMerking = finnPerioderMedManglendeMerking(bosattIRiketVilkårTidslinje, finnmarkEllerNordTromsOppholdTidslinjeForInstitusjon, UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)

    if (perioderMedManglendeFinnmarkMerking.isEmpty()) return null

    return ManglendeFinnmarkSvalbardMerkingDto(personResultat.aktør.aktivFødselsnummer(), perioderMedManglendeFinnmarkMerking)
}

fun List<Person>?.tilManglendeSvalbardmerkingPerioder(personResultater: Set<PersonResultat>?): List<ManglendeFinnmarkSvalbardMerkingDto> {
    if (this == null || personResultater == null) return emptyList()

    val bosattIRiketVilkårTidslinjePerPerson =
        personResultater.associate {
            it.aktør.aktivFødselsnummer() to
                it.vilkårResultater
                    .filter { vilkårResultat ->
                        vilkårResultat.vilkårType == Vilkår.BOSATT_I_RIKET && vilkårResultat.periodeFom != null
                    }.tilTidslinje()
        }

    val svalbardOppholdTidslinjerPerPerson =
        this.associate {
            it.aktør.aktivFødselsnummer() to Adresser.opprettFra(it).lagErOppholdsadresserPåSvalbardTidslinje()
        }

    return bosattIRiketVilkårTidslinjePerPerson.mapNotNull { (fnr, bosattIRiketVilkårTidslinje) ->
        val svalbardOppholdTidslinje = svalbardOppholdTidslinjerPerPerson[fnr] ?: return@mapNotNull null

        val perioderMedManglendeSvalbardMerking = finnPerioderMedManglendeMerking(bosattIRiketVilkårTidslinje, svalbardOppholdTidslinje, UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)

        if (perioderMedManglendeSvalbardMerking.isEmpty()) return@mapNotNull null

        ManglendeFinnmarkSvalbardMerkingDto(fnr, perioderMedManglendeSvalbardMerking)
    }
}

private fun <T> finnPerioderMedManglendeMerking(
    bosattIRiketVilkårTidslinje: Tidslinje<VilkårResultat>,
    finnmarkEllerSvalbardOppholdTidslinje: Tidslinje<T>,
    utdypendeVilkårsvurdering: UtdypendeVilkårsvurdering,
): List<ManglendeFinnmarkSvalbardMerkingPeriodeDto> {
    val beskjærtFinnmarkEllerSvalbardTidslinje = finnmarkEllerSvalbardOppholdTidslinje.beskjærEtter(bosattIRiketVilkårTidslinje)

    return bosattIRiketVilkårTidslinje
        .kombinerMed(beskjærtFinnmarkEllerSvalbardTidslinje) { bosattIRiketVilkår, finnmarkEllerSvalbard ->
            finnmarkEllerSvalbard == true && bosattIRiketVilkår != null && !bosattIRiketVilkår.utdypendeVilkårsvurderinger.contains(utdypendeVilkårsvurdering)
        }.tilPerioderIkkeNull()
        .filter { it.tom == null || it.tom!!.isSameOrAfter(cutOffTomDatoForVisningAvManglendeMerkinger) }
        .filter { it.verdi }
        .map { ManglendeFinnmarkSvalbardMerkingPeriodeDto(fom = it.fom, tom = it.tom) }
}

fun List<SamhandlerAdresse>.tilFinnmmarkEllerNordTromsOppholdTidslinje(): Tidslinje<Boolean> =
    this
        .sortedBy { it.gyldighetsperiode?.fom }
        .tilEtterfølgendePar { institusjonsinfo, nesteInstitusjonsinfo ->
            Periode(
                verdi = institusjonsinfo.kommunenummer?.let { kommunenummer -> kommuneErIFinnmarkEllerNordTroms(kommunenummer) } ?: false,
                fom = institusjonsinfo.gyldighetsperiode?.fom,
                tom = institusjonsinfo.gyldighetsperiode?.tom ?: nesteInstitusjonsinfo?.gyldighetsperiode?.fom?.minusDays(1),
            )
        }.map { it.tilTidslinje() }
        .kombiner { samletTidslinjer -> samletTidslinjer.any { boddeIFinnmarkEllerNordtroms -> boddeIFinnmarkEllerNordtroms } }
