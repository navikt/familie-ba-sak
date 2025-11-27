package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.hentSterkesteMedlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.lagErNordiskStatsborgerTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_OM_ARBEIDSFORHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_OM_OPPHOLDSTILLATELSE
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PreutfyllLovligOppholdMedLagringIPersongrunnlagService(
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllLovligOpphold(vilkårsvurdering: Vilkårsvurdering) {
        val personOpplysningsgrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id)

        val søkersResultater = vilkårsvurdering.personResultater.first { it.erSøkersResultater() }
        val søker = personOpplysningsgrunnlag.personer.find { it.aktør == søkersResultater.aktør } ?: throw Feil("Aktør ${søkersResultater.aktør.aktørId} har personresultat men ikke persongrunnlag")
        val bostedsadresserSøker = Adresser.opprettFra(søker).bostedsadresser

        val fomDatoForBeskjæring = finnFomDatoForBeskjæring(søkersResultater, vilkårsvurdering, bostedsadresserSøker) ?: PRAKTISK_TIDLIGSTE_DAG
        val erEØSBorgerOgHarArbeidsforholdTidslinjeSøker = lagErEØSBorgerOgHarArbeidsforholdTidslinje(søker.statsborgerskap.toList(), søker.arbeidsforhold.toList(), fomDatoForBeskjæring)

        vilkårsvurdering.personResultater.forEach { personResultat ->
            val person = personOpplysningsgrunnlag.personer.find { it.aktør == personResultat.aktør } ?: throw Feil("Aktør ${personResultat.aktør.aktørId} har personresultat men ikke persongrunnlag")

            val lovligOppholdVilkårResultat =
                genererLovligOppholdVilkårResultat(personResultat, person, erEØSBorgerOgHarArbeidsforholdTidslinjeSøker)

            if (lovligOppholdVilkårResultat.isNotEmpty()) {
                personResultat.vilkårResultater.removeIf { it.vilkårType == LOVLIG_OPPHOLD }
                personResultat.vilkårResultater.addAll(lovligOppholdVilkårResultat)
            }
        }
    }

    private fun genererLovligOppholdVilkårResultat(
        personResultat: PersonResultat,
        personInfo: Person,
        erEØSBorgerOgHarArbeidsforholdTidslinje: Tidslinje<Boolean>,
    ): Set<VilkårResultat> {
        val erNordiskStatsborgerTidslinje = lagErNordiskStatsborgerTidslinje(personInfo.statsborgerskap)

        val bostedsadresserForPerson = Adresser.opprettFra(personInfo).bostedsadresser
        val fomDatoForBeskjæring = finnFomDatoForBeskjæring(personResultat, personResultat.vilkårsvurdering, bostedsadresserForPerson) ?: PRAKTISK_TIDLIGSTE_DAG

        val harOppholdstillatelseTidslinje = lagHarOppholdstillatelseTidslinje(personInfo.opphold)

        val harLovligOppholdTidslinje =
            erNordiskStatsborgerTidslinje
                .kombinerMed(erEØSBorgerOgHarArbeidsforholdTidslinje, harOppholdstillatelseTidslinje) { erNordisk, erEØSBorgerOgArbeidsforhold, harOppholdstillatelse ->
                    when {
                        erNordisk == true -> OppfyltDelvilkår("- Norsk/nordisk statsborgerskap.")
                        erEØSBorgerOgArbeidsforhold == true -> OppfyltDelvilkår("- EØS-borger og har arbeidsforhold i Norge.", begrunnelseForManuellKontroll = INFORMASJON_OM_ARBEIDSFORHOLD)
                        harOppholdstillatelse == true -> OppfyltDelvilkår("- Har gyldig oppholdstillatelse i Norge.", begrunnelseForManuellKontroll = INFORMASJON_OM_OPPHOLDSTILLATELSE)
                        else -> IkkeOppfyltDelvilkår
                    }
                }.beskjærFraOgMed(fomDatoForBeskjæring)

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

    private fun finnFomDatoForBeskjæring(
        personResultat: PersonResultat,
        vilkårsvurdering: Vilkårsvurdering,
        bostedsadresser: List<Adresse>,
    ): LocalDate? {
        val førsteBostedFomDato =
            bostedsadresser
                .filter { it.vegadresse != null || it.matrikkeladresse != null || it.ukjentBosted != null }
                .mapNotNull { it.gyldigFraOgMed }
                .minOrNull()

        val barna = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id).barna

        return førsteBostedFomDato ?: if (personResultat.erSøkersResultater()) {
            barna.minOfOrNull { it.fødselsdato }
        } else {
            barna.find { it.aktør.aktørId == personResultat.aktør.aktørId }?.fødselsdato
        }
    }

    private fun lagErEØSBorgerOgHarArbeidsforholdTidslinje(
        statsborgerskap: List<GrStatsborgerskap>,
        arbeidsforhold: List<GrArbeidsforhold>,
        fomDatoForBeskjæring: LocalDate,
    ): Tidslinje<Boolean> =
        lagErEØSBorgerTidslinje(statsborgerskap)
            .kombinerMed(lagHarArbeidsforholdTidslinje(arbeidsforhold, fomDatoForBeskjæring)) { erEØSBorger, harArbeidsforhold ->
                erEØSBorger == true && harArbeidsforhold == true
            }

    private fun lagErEØSBorgerTidslinje(alleStatsborgerskap: List<GrStatsborgerskap>): Tidslinje<Boolean> =
        alleStatsborgerskap
            .windowed(size = 2, step = 1, partialWindows = true) {
                val gjeldende = it.first()
                val neste = it.getOrNull(1)

                val erEØSBorger = alleStatsborgerskap.hentSterkesteMedlemskap() == Medlemskap.EØS

                Periode(
                    verdi = erEØSBorger,
                    fom = gjeldende.gyldigPeriode?.fom,
                    tom = gjeldende.gyldigPeriode?.tom ?: neste?.gyldigPeriode?.fom?.minusDays(1),
                )
            }.tilTidslinje()

    private fun lagHarArbeidsforholdTidslinje(
        arbeidsforhold: List<GrArbeidsforhold>,
        fomDatoForBeskjæring: LocalDate,
    ): Tidslinje<Boolean> =
        arbeidsforhold
            .mapNotNull { it.periode }
            .map { Periode(verdi = true, fom = it.fom, tom = it.tom) }
            .filter { it.fom != null && it.fom!! >= fomDatoForBeskjæring }
            .tilTidslinje()

    private fun lagHarOppholdstillatelseTidslinje(oppholdstillatelse: List<GrOpphold>): Tidslinje<Boolean> =
        oppholdstillatelse
            .filter { it.type == OPPHOLDSTILLATELSE.PERMANENT || it.type == OPPHOLDSTILLATELSE.MIDLERTIDIG }
            .mapIndexed { index, it ->
                val erSiste = index == oppholdstillatelse.lastIndex
                Periode(
                    verdi = true,
                    fom = it.gyldigPeriode?.fom,
                    tom = if (erSiste) null else it.gyldigPeriode?.tom,
                )
            }.tilTidslinje()
}
