package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PreutfyllLovligOppholdService(
    private val pdlRestClient: PdlRestClient,
    private val statsborgerskapService: StatsborgerskapService,
    private val integrasjonClient: IntegrasjonClient,
) {
    fun preutfyllLovligOpphold(vilkårsvurdering: Vilkårsvurdering) {
        val søker = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }

        val datoFørsteBostedadresseNorgeSøker = finnDatoFørsteBostedsadresseINorge(søker) ?: LocalDate.MIN
        val erEØSBorgerOgHarArbeidsforholdTidslinjeSøker = lagErEØSBorgerOgHarArbeidsforholdTidslinje(søker, datoFørsteBostedadresseNorgeSøker)

        vilkårsvurdering.personResultater.forEach { personResultat ->
            val lovligOppholdVilkårResultat =
                when {
                    personResultat.erSøkersResultater() -> {
                        genererLovligOppholdVilkårResultat(personResultat, null)
                    }
                    else -> {
                        val datoFørsteBostedadresseNorgeBarn = finnDatoFørsteBostedsadresseINorge(personResultat) ?: LocalDate.MIN
                        val erEØSBorgerOgHarArbeidsforholdTidslinjeBarn = erEØSBorgerOgHarArbeidsforholdTidslinjeSøker.beskjærFraOgMed(datoFørsteBostedadresseNorgeBarn)
                        genererLovligOppholdVilkårResultat(personResultat, erEØSBorgerOgHarArbeidsforholdTidslinjeBarn)
                    }
                }

            if (lovligOppholdVilkårResultat.isNotEmpty()) {
                personResultat.vilkårResultater.removeIf { it.vilkårType == LOVLIG_OPPHOLD }
                personResultat.vilkårResultater.addAll(lovligOppholdVilkårResultat)
            }
        }
    }

    fun genererLovligOppholdVilkårResultat(
        personResultat: PersonResultat,
        erEøsBorgerOgHarArbeidsforholdTidslinjeOverride: Tidslinje<Boolean>? = null,
    ): Set<VilkårResultat> {
        val erNordiskStatsborgerTidslinje = pdlRestClient.lagErNordiskStatsborgerTidslinje(personResultat)

        val erBosattINorgeTidslinje = lagErBosattINorgeTidslinje(personResultat)

        val datoFørsteBostedadresse = finnDatoFørsteBostedsadresseINorge(personResultat) ?: LocalDate.MIN

        val erNordiskHelePerioden =
            erNordiskStatsborgerTidslinje
                .tilPerioderIkkeNull()
                .all { it.verdi == true }

        val erEØSBorgerOgHarArbeidsforholdTidslinje =
            when {
                erNordiskHelePerioden -> tomTidslinje() // Trenger ikke hente
                erEøsBorgerOgHarArbeidsforholdTidslinjeOverride != null -> erEøsBorgerOgHarArbeidsforholdTidslinjeOverride
                else -> lagErEØSBorgerOgHarArbeidsforholdTidslinje(personResultat, datoFørsteBostedadresse)
            }
//
//        val erEØSBorgerOgHarArbeidsforholdTidslinje2 =
//            erEøsBorgerOgHarArbeidsforholdTidslinjeOverride
//                ?: lagErEØSBorgerOgHarArbeidsforholdTidslinje(personResultat, datoFørsteBostedadresse)

        val harLovligOppholdTidslinje =
            erBosattINorgeTidslinje
                .kombinerMed(erNordiskStatsborgerTidslinje, erEØSBorgerOgHarArbeidsforholdTidslinje) { erBosatt, erNordisk, erEøsOgArbeidsforhold ->
                    when {
                        erBosatt == true && erNordisk == true -> OppfyltDelvilkår("- Norsk/nordisk statsborgerskap.")
                        erBosatt == true && erEøsOgArbeidsforhold == true -> OppfyltDelvilkår("- EØS-borger og har arbeidsforhold i Norge.")
                        else -> IkkeOppfyltDelvilkår
                    }
                }.beskjærFraOgMed(datoFørsteBostedadresse)

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
                    begrunnelse = "Fylt ut automatisk fra registerdata i PDL\n" + periode.verdi.begrunnelse,
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                    begrunnelseForManuellKontroll = periode.verdi.begrunnelseForManuellKontroll,
                )
            }.toSet()
    }

    private fun lagErBosattINorgeTidslinje(personResultat: PersonResultat): Tidslinje<Boolean> {
        val alleBostedsadresserForPerson =
            pdlRestClient
                .hentBostedsadresserForPerson(fødselsnummer = personResultat.aktør.aktivFødselsnummer())
                .sortedBy { it.gyldigFraOgMed }

        return alleBostedsadresserForPerson
            .windowed(size = 2, step = 1, partialWindows = true) {
                val denne = it.first()
                val neste = it.getOrNull(1)

                Periode(
                    verdi = denne.vegadresse != null || denne.matrikkeladresse != null || denne.ukjentBosted != null,
                    fom = denne.gyldigFraOgMed,
                    tom = denne.gyldigTilOgMed ?: neste?.gyldigFraOgMed?.minusDays(1),
                )
            }.tilTidslinje()
    }

    private fun finnDatoFørsteBostedsadresseINorge(personResultat: PersonResultat?): LocalDate? =
        pdlRestClient
            .hentBostedsadresserForPerson(fødselsnummer = personResultat!!.aktør.aktivFødselsnummer())
            .filter { it.vegadresse != null || it.matrikkeladresse != null || it.ukjentBosted != null }
            .mapNotNull { it.gyldigFraOgMed }
            .minByOrNull { it }

    private fun lagErEØSBorgerOgHarArbeidsforholdTidslinje(
        personResultat: PersonResultat?,
        datoFørsteBostedadresse: LocalDate,
    ): Tidslinje<Boolean> {
        val erEØSBorgerTidslinje = lagErEØSBorgerTidslinje(personResultat)
        val arbeidsforholdTidslinje = lagHarArbeidsforholdTidslinje(personResultat, datoFørsteBostedadresse)

        return erEØSBorgerTidslinje
            .kombinerMed(arbeidsforholdTidslinje) { erEøs, harArbeidsforhold ->
                erEøs == true && harArbeidsforhold == true
            }
    }

    private fun lagErEØSBorgerTidslinje(personResultat: PersonResultat?): Tidslinje<Boolean> {
        val statsborgerskap = pdlRestClient.hentStatsborgerskap(personResultat!!.aktør, historikk = true)

        return statsborgerskap
            .windowed(size = 2, step = 1, partialWindows = true) {
                val gjeldende = it.first()
                val neste = it.getOrNull(1)

                val erEøs = statsborgerskapService.hentSterkesteMedlemskap(gjeldende) == Medlemskap.EØS

                Periode(
                    verdi = erEøs,
                    fom = gjeldende.gyldigFraOgMed,
                    tom = gjeldende.gyldigTilOgMed ?: neste?.gyldigFraOgMed?.minusDays(1),
                )
            }.tilTidslinje()
    }

    private fun lagHarArbeidsforholdTidslinje(
        personResultat: PersonResultat?,
        datoFørsteBostedadresse: LocalDate,
    ): Tidslinje<Boolean> {
        val arbeidsforhold =
            integrasjonClient.hentArbeidsforhold(
                ident = personResultat!!.aktør.aktivFødselsnummer(),
                ansettelsesperiodeFom = datoFørsteBostedadresse,
            )

        return arbeidsforhold
            .mapNotNull { it.ansettelsesperiode?.periode }
            .map {
                Periode(
                    verdi = true,
                    fom = it.fom,
                    tom = it.tom,
                )
            }.tilTidslinje()
    }
}
