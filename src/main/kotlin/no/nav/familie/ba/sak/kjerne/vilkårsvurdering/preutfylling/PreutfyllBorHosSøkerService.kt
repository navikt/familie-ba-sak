package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate

class PreutfyllBorHosSøkerService(
    private val pdlRestClient: SystemOnlyPdlRestClient,
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering: Vilkårsvurdering) {
        val identer = vilkårsvurdering.personResultater.map { it.aktør.aktivFødselsnummer() }
        val bostedsadresser = pdlRestClient.hentBostedsadresseOgDeltBostedForPersoner(identer)

        val søkersResultater = vilkårsvurdering.personResultater.first { it.erSøkersResultater() }
        val bostedsadresserSøker = bostedsadresser[søkersResultater.aktør.aktivFødselsnummer()]?.bostedsadresse ?: emptyList()

        vilkårsvurdering.personResultater.forEach { personResultat ->
            if (personResultat.erSøkersResultater()) {
                return@forEach
            } else {
                val bostedsadresserBarn = bostedsadresser[personResultat.aktør.aktivFødselsnummer()]?.bostedsadresse ?: emptyList()
                val borFastHosSøkerVilkårResultat = genererBorHosSøkerVilkårResultat(personResultat, bostedsadresserBarn, bostedsadresserSøker)

                if (borFastHosSøkerVilkårResultat.isNotEmpty()) {
                    personResultat.vilkårResultater.removeIf { it.vilkårType == BOR_MED_SØKER }
                    personResultat.vilkårResultater.addAll(borFastHosSøkerVilkårResultat)
                }
            }
        }
    }

    private fun genererBorHosSøkerVilkårResultat(
        personResultat: PersonResultat,
        bostedsadresserBarn: List<Bostedsadresse>,
        bostedsadresserSøker: List<Bostedsadresse>,
    ): Set<VilkårResultat> {
        val fødselsdatoForBeskjæring =
            persongrunnlagService
                .hentAktivThrows(personResultat.vilkårsvurdering.behandling.id)
                .barna
                .find { it.aktør.aktørId == personResultat.aktør.aktørId }
                ?.fødselsdato ?: PRAKTISK_TIDLIGSTE_DAG

        val harSammeBostedsadresseTidslinje = lagBorHosSøkerTidslinje(bostedsadresserBarn, bostedsadresserSøker, fødselsdatoForBeskjæring)

        return harSammeBostedsadresseTidslinje
            .tilPerioderIkkeNull()
            .map { periode ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = periode.verdi.tilResultat(),
                    vilkårType = BOR_MED_SØKER,
                    periodeFom = periode.fom,
                    periodeTom = periode.tom,
                    begrunnelse = "Fylt ut automatisk fra registerdata i PDL\n" + periode.verdi.begrunnelse,
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSet()
    }

    private fun lagBorHosSøkerTidslinje(
        bostedsadresserBarn: List<Bostedsadresse>,
        bostedsadresserSøker: List<Bostedsadresse>,
        fødselsdatoForBeskjæring: LocalDate,
    ): Tidslinje<Delvilkår> {
        val bostedsadresserBarnTidslinje = lagBostedsadresseTidslinje(bostedsadresserBarn, fødselsdatoForBeskjæring)
        val bostedsadresserSøkerTidslinje = lagBostedsadresseTidslinje(bostedsadresserSøker, fødselsdatoForBeskjæring)

        return bostedsadresserBarnTidslinje.kombinerMed(bostedsadresserSøkerTidslinje) { barnAdresse, søkerAdresse ->
            if (barnAdresse != null && barnAdresse.erSammeAdresse(søkerAdresse)) {
                OppfyltDelvilkår(begrunnelse = "- Har samme bostedsadresse som søker.")
            } else {
                IkkeOppfyltDelvilkår
            }
        }
    }

    private fun lagBostedsadresseTidslinje(
        bostedsadresser: List<Bostedsadresse>,
        fødselsdatoForBeskjæring: LocalDate,
    ): Tidslinje<Bostedsadresse> =
        bostedsadresser
            .sortedBy { it.gyldigFraOgMed }
            .windowed(size = 2, step = 1, partialWindows = true) {
                val denne = it.first()
                val neste = it.getOrNull(1)

                Periode(
                    verdi = denne,
                    fom = denne.gyldigFraOgMed,
                    tom = denne.gyldigTilOgMed ?: neste?.gyldigFraOgMed?.minusDays(1),
                )
            }.tilTidslinje()
            .beskjærFraOgMed(fødselsdatoForBeskjæring)

    private fun Bostedsadresse.erSammeAdresse(søkersAdresse: Bostedsadresse?): Boolean =
        søkersAdresse != null && (
            (this.vegadresse != null && this.vegadresse == søkersAdresse.vegadresse) ||
                (this.matrikkeladresse != null && this.matrikkeladresse == søkersAdresse.matrikkeladresse)
        )
}
