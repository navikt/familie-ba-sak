package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.SystemOnlyIntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_OM_ARBEIDSFORHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_OM_OPPHOLDSTILLATELSE
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
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
    private val pdlRestKlient: SystemOnlyPdlRestKlient,
    private val statsborgerskapService: StatsborgerskapService,
    private val systemOnlyIntegrasjonKlient: SystemOnlyIntegrasjonKlient,
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllLovligOpphold(vilkårsvurdering: Vilkårsvurdering) {
        val identer = vilkårsvurdering.personResultater.map { it.aktør.aktivFødselsnummer() }
        val bostedsadresser = pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(identer)
        val søkersResultater = vilkårsvurdering.personResultater.first { it.erSøkersResultater() }

        val bostedsadresserSøker = bostedsadresser[søkersResultater.aktør.aktivFødselsnummer()]?.bostedsadresse ?: emptyList()
        val fomDatoForBeskjæring = finnFomDatoForBeskjæring(søkersResultater, vilkårsvurdering, bostedsadresserSøker) ?: PRAKTISK_TIDLIGSTE_DAG
        val erEØSBorgerOgHarArbeidsforholdTidslinjeSøker = lagErEØSBorgerOgHarArbeidsforholdTidslinje(søkersResultater, fomDatoForBeskjæring)

        vilkårsvurdering.personResultater.forEach { personResultat ->
            val bostedsadresserForPerson = bostedsadresser[personResultat.aktør.aktivFødselsnummer()]?.bostedsadresse ?: emptyList()
            val lovligOppholdVilkårResultat =
                genererLovligOppholdVilkårResultat(personResultat, erEØSBorgerOgHarArbeidsforholdTidslinjeSøker, bostedsadresserForPerson)

            if (lovligOppholdVilkårResultat.isNotEmpty()) {
                personResultat.vilkårResultater.removeIf { it.vilkårType == LOVLIG_OPPHOLD }
                personResultat.vilkårResultater.addAll(lovligOppholdVilkårResultat)
            }
        }
    }

    private fun genererLovligOppholdVilkårResultat(
        personResultat: PersonResultat,
        erEØSBorgerOgHarArbeidsforholdTidslinje: Tidslinje<Boolean>,
        bostedsadresserForPerson: List<Bostedsadresse>,
    ): Set<VilkårResultat> {
        val erNordiskStatsborgerTidslinje = pdlRestKlient.lagErNordiskStatsborgerTidslinje(personResultat)

        val fomDatoForBeskjæring = finnFomDatoForBeskjæring(personResultat, personResultat.vilkårsvurdering, bostedsadresserForPerson) ?: PRAKTISK_TIDLIGSTE_DAG

        val harOppholdstillatelseTidslinje = lagHarOppholdstillatelseTidslinje(personResultat)

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
        bostedsadresser: List<Bostedsadresse>,
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
        personResultat: PersonResultat,
        fomDatoForBeskjæring: LocalDate,
    ): Tidslinje<Boolean> =
        lagErEØSBorgerTidslinje(personResultat)
            .kombinerMed(lagHarArbeidsforholdTidslinje(personResultat, fomDatoForBeskjæring)) { erEØSBorger, harArbeidsforhold ->
                erEØSBorger == true && harArbeidsforhold == true
            }

    private fun lagErEØSBorgerTidslinje(personResultat: PersonResultat): Tidslinje<Boolean> {
        val statsborgerskap = pdlRestKlient.hentStatsborgerskap(personResultat.aktør, historikk = true)

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
        fomDatoForBeskjæring: LocalDate,
    ): Tidslinje<Boolean> {
        val arbeidsforhold =
            systemOnlyIntegrasjonKlient.hentArbeidsforholdMedSystembruker(
                ident = personResultat.aktør.aktivFødselsnummer(),
                ansettelsesperiodeFom = fomDatoForBeskjæring,
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

    private fun lagHarOppholdstillatelseTidslinje(personResultat: PersonResultat): Tidslinje<Boolean> {
        val oppholdstillatelse = pdlRestKlient.hentOppholdstillatelse(personResultat.aktør, historikk = true)

        return oppholdstillatelse
            .filter { it.type == OPPHOLDSTILLATELSE.PERMANENT || it.type == OPPHOLDSTILLATELSE.MIDLERTIDIG }
            .mapIndexed { index, it ->
                val erSiste = index == oppholdstillatelse.lastIndex
                Periode(
                    verdi = true,
                    fom = it.oppholdFra,
                    tom = if (erSiste) null else it.oppholdTil,
                )
            }.tilTidslinje()
    }
}
