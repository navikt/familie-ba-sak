package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.lagErNordiskStatsborgerTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.tilPerson
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_OM_ARBEIDSFORHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_OM_OPPHOLDSTILLATELSE
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE.MIDLERTIDIG
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE.PERMANENT
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PreutfyllLovligOppholdService(
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllLovligOpphold(vilkårsvurdering: Vilkårsvurdering) {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id)

        val datoForBeskjæringAvFom = finnDatoForBeskjæringAvFom(personopplysningGrunnlag.søker, personopplysningGrunnlag)
        val søkerErEøsBorgerOgHarArbeidsforholdTidslinje = lagErEøsBorgerOgHarArbeidsforholdTidslinje(personopplysningGrunnlag.søker, datoForBeskjæringAvFom)

        vilkårsvurdering.personResultater
            .forEach { personResultat ->
                val person = personResultat.aktør.tilPerson(personopplysningGrunnlag)

                val datoForBeskjæringAvFom = finnDatoForBeskjæringAvFom(person, personopplysningGrunnlag)

                val nyeLovligOppholdVilkårResultat =
                    genererLovligOppholdVilkårResultat(
                        personResultat = personResultat,
                        person = person,
                        søkerErEøsBorgerOgHarArbeidsforholdTidslinje = søkerErEøsBorgerOgHarArbeidsforholdTidslinje,
                        datoForBeskjæringAvFom = datoForBeskjæringAvFom,
                    )

                if (nyeLovligOppholdVilkårResultat.isNotEmpty()) {
                    personResultat.vilkårResultater.removeIf { it.vilkårType == LOVLIG_OPPHOLD }
                    personResultat.vilkårResultater.addAll(nyeLovligOppholdVilkårResultat)
                }
            }
    }

    private fun genererLovligOppholdVilkårResultat(
        personResultat: PersonResultat,
        person: Person,
        søkerErEøsBorgerOgHarArbeidsforholdTidslinje: Tidslinje<Boolean>,
        datoForBeskjæringAvFom: LocalDate,
    ): Set<VilkårResultat> {
        val erNordiskStatsborgerTidslinje = lagErNordiskStatsborgerTidslinje(person.statsborgerskap)
        val harOppholdstillatelseTidslinje = lagHarOppholdstillatelseTidslinje(person.opphold)

        val harLovligOppholdTidslinje =
            erNordiskStatsborgerTidslinje
                .kombinerMed(søkerErEøsBorgerOgHarArbeidsforholdTidslinje, harOppholdstillatelseTidslinje) { erNordisk, erEØSBorgerOgArbeidsforhold, harOppholdstillatelse ->
                    when {
                        erNordisk == true -> OppfyltDelvilkår("- Norsk/nordisk statsborgerskap.")
                        erEØSBorgerOgArbeidsforhold == true -> OppfyltDelvilkår("- EØS-borger og har arbeidsforhold i Norge.", begrunnelseForManuellKontroll = INFORMASJON_OM_ARBEIDSFORHOLD)
                        harOppholdstillatelse == true -> OppfyltDelvilkår("- Har gyldig oppholdstillatelse i Norge.", begrunnelseForManuellKontroll = INFORMASJON_OM_OPPHOLDSTILLATELSE)
                        else -> IkkeOppfyltDelvilkår()
                    }
                }.beskjærFraOgMed(datoForBeskjæringAvFom)

        return harLovligOppholdTidslinje
            .tilPerioderIkkeNull()
            .map { periode ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = periode.verdi.tilResultat(),
                    vilkårType = LOVLIG_OPPHOLD,
                    periodeFom = periode.fom,
                    periodeTom = periode.tom,
                    begrunnelse = PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + periode.verdi.begrunnelse,
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                    begrunnelseForManuellKontroll = periode.verdi.begrunnelseForManuellKontroll,
                    erOpprinneligPreutfylt = true,
                )
            }.toSet()
    }

    private fun finnDatoForBeskjæringAvFom(
        person: Person,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ): LocalDate =
        maxOf(
            personopplysningGrunnlag.eldsteBarnSinFødselsdato ?: PRAKTISK_TIDLIGSTE_DAG,
            person.fødselsdato,
        )

    private fun lagErEøsBorgerOgHarArbeidsforholdTidslinje(
        person: Person,
        datoForBeskjæringAvFom: LocalDate,
    ): Tidslinje<Boolean> {
        val erEøsBorgerTidslinje = lagErEøsBorgerTidslinje(person.statsborgerskap)
        val harArbeidsforholdTidslinje = lagHarArbeidsforholdTidslinje(person.arbeidsforhold)

        return erEøsBorgerTidslinje
            .kombinerMed(harArbeidsforholdTidslinje) { erEØSBorger, harArbeidsforhold ->
                erEØSBorger == true && harArbeidsforhold == true
            }.beskjærFraOgMed(datoForBeskjæringAvFom)
    }

    private fun lagErEøsBorgerTidslinje(alleStatsborgerskap: List<GrStatsborgerskap>): Tidslinje<Boolean> =
        alleStatsborgerskap
            .map { statsborgerskap ->
                listOf(
                    Periode(
                        verdi = statsborgerskap.medlemskap == Medlemskap.EØS,
                        fom = statsborgerskap.gyldigPeriode?.fom,
                        tom = statsborgerskap.gyldigPeriode?.tom,
                    ),
                ).tilTidslinje()
            }.kombiner { it.any() }

    private fun lagHarArbeidsforholdTidslinje(
        arbeidsforhold: List<GrArbeidsforhold>,
    ): Tidslinje<Boolean> =
        arbeidsforhold
            .mapNotNull { it.periode }
            .map { Periode(verdi = true, fom = it.fom, tom = it.tom) }
            .tilTidslinje()

    private fun lagHarOppholdstillatelseTidslinje(oppholdstillatelse: List<GrOpphold>): Tidslinje<Boolean> =
        oppholdstillatelse
            .filter { it.type in setOf(PERMANENT, MIDLERTIDIG) }
            .mapIndexed { index, it ->
                Periode(
                    verdi = true,
                    fom = it.gyldigPeriode?.fom,
                    tom = it.gyldigPeriode?.tom.takeUnless { index == oppholdstillatelse.lastIndex },
                )
            }.tilTidslinje()
}
