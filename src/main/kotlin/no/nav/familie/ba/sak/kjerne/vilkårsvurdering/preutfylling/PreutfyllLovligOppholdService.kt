package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_OM_ARBEIDSFORHOLD
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PreutfyllLovligOppholdService(
    private val pdlRestClient: PdlRestClient,
    private val statsborgerskapService: StatsborgerskapService,
    private val integrasjonClient: IntegrasjonClient,
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllLovligOpphold(vilkårsvurdering: Vilkårsvurdering) {
        val søkersResultater = vilkårsvurdering.personResultater.first { it.erSøkersResultater() }

        val fomDatoForBeskjæring = finnFomDatoForBeskjæring(søkersResultater, vilkårsvurdering) ?: PRAKTISK_TIDLIGSTE_DAG
        val erEØSBorgerOgHarArbeidsforholdTidslinjeSøker = lagErEØSBorgerOgHarArbeidsforholdTidslinje(søkersResultater, fomDatoForBeskjæring)

        vilkårsvurdering.personResultater.forEach { personResultat ->
            val lovligOppholdVilkårResultat =
                genererLovligOppholdVilkårResultat(personResultat, erEØSBorgerOgHarArbeidsforholdTidslinjeSøker)

            if (lovligOppholdVilkårResultat.isNotEmpty()) {
                personResultat.vilkårResultater.removeIf { it.vilkårType == LOVLIG_OPPHOLD }
                personResultat.vilkårResultater.addAll(lovligOppholdVilkårResultat)
            }
        }
    }

    private fun genererLovligOppholdVilkårResultat(
        personResultat: PersonResultat,
        erEØSBorgerOgHarArbeidsforholdTidslinje: Tidslinje<Boolean>,
    ): Set<VilkårResultat> {
        val erNordiskStatsborgerTidslinje = pdlRestClient.lagErNordiskStatsborgerTidslinje(personResultat)

        val fomDatoForBeskjæring = finnFomDatoForBeskjæring(personResultat, personResultat.vilkårsvurdering) ?: PRAKTISK_TIDLIGSTE_DAG

        val harLovligOppholdTidslinje =
            erNordiskStatsborgerTidslinje
                .kombinerMed(erEØSBorgerOgHarArbeidsforholdTidslinje) { erNordisk, erEØSBorgerOgArbeidsforhold ->
                    when {
                        erNordisk == true -> OppfyltDelvilkår("- Norsk/nordisk statsborgerskap.")
                        erEØSBorgerOgArbeidsforhold == true -> OppfyltDelvilkår("- EØS-borger og har arbeidsforhold i Norge.", begrunnelseForManuellKontroll = INFORMASJON_OM_ARBEIDSFORHOLD)
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
                    begrunnelse = "Fylt ut automatisk fra registerdata i PDL\n" + periode.verdi.begrunnelse,
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                    begrunnelseForManuellKontroll = periode.verdi.begrunnelseForManuellKontroll,
                )
            }.toSet()
    }

    private fun finnFomDatoForBeskjæring(
        personResultat: PersonResultat,
        vilkårsvurdering: Vilkårsvurdering,
    ): LocalDate? {
        val førsteBostedFomDato =
            pdlRestClient
                .hentBostedsadresserForPerson(fødselsnummer = personResultat.aktør.aktivFødselsnummer())
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
        personResultat: PersonResultat,
        fomDatoForBeskjæring: LocalDate,
    ): Tidslinje<Boolean> =
        lagErEØSBorgerTidslinje(personResultat)
            .kombinerMed(lagHarArbeidsforholdTidslinje(personResultat, fomDatoForBeskjæring)) { erEØSBorger, harArbeidsforhold ->
                erEØSBorger == true && harArbeidsforhold == true
            }

    private fun lagErEØSBorgerTidslinje(personResultat: PersonResultat): Tidslinje<Boolean> {
        val statsborgerskap = pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true)

        return statsborgerskap
            .windowed(size = 2, step = 1, partialWindows = true) {
                val gjeldende = it.first()
                val neste = it.getOrNull(1)

                val erEØSBorger = statsborgerskapService.hentSterkesteMedlemskap(gjeldende) == Medlemskap.EØS

                Periode(
                    verdi = erEØSBorger,
                    fom = gjeldende.gyldigFraOgMed,
                    tom = gjeldende.gyldigTilOgMed ?: neste?.gyldigFraOgMed?.minusDays(1),
                )
            }.tilTidslinje()
    }

    private fun lagHarArbeidsforholdTidslinje(
        personResultat: PersonResultat,
        datoFørsteBostedadresse: LocalDate,
    ): Tidslinje<Boolean> {
        val arbeidsforhold =
            integrasjonClient.hentArbeidsforhold(
                ident = personResultat.aktør.aktivFødselsnummer(),
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
