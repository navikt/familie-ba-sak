package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class PreutfyllBorHosSøkerService(
    private val pdlRestKlient: SystemOnlyPdlRestKlient,
) {
    fun preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering: Vilkårsvurdering) {
        val identer = vilkårsvurdering.personResultater.map { it.aktør.aktivFødselsnummer() }
        val bostedsadresser = pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(identer)

        val søkersResultater = vilkårsvurdering.personResultater.first { it.erSøkersResultater() }
        val bostedsadresserSøker = bostedsadresser[søkersResultater.aktør.aktivFødselsnummer()]?.bostedsadresse ?: emptyList()

        vilkårsvurdering.personResultater
            .filterNot { it.erSøkersResultater() }
            .forEach { personResultat ->
                val bostedsadresserBarn = bostedsadresser[personResultat.aktør.aktivFødselsnummer()]?.bostedsadresse ?: emptyList()
                val borFastHosSøkerVilkårResultat = genererBorHosSøkerVilkårResultat(personResultat, bostedsadresserBarn, bostedsadresserSøker)

                if (borFastHosSøkerVilkårResultat.isNotEmpty()) {
                    personResultat.vilkårResultater.removeIf { it.vilkårType == BOR_MED_SØKER }
                    personResultat.vilkårResultater.addAll(borFastHosSøkerVilkårResultat)
                }
            }
    }

    private fun genererBorHosSøkerVilkårResultat(
        personResultat: PersonResultat,
        bostedsadresserBarn: List<Bostedsadresse>,
        bostedsadresserSøker: List<Bostedsadresse>,
    ): Set<VilkårResultat> {
        val harSammeBostedsadresseTidslinje =
            lagBorHosSøkerTidslinje(
                bostedsadresserBarn = bostedsadresserBarn,
                bostedsadresserSøker = bostedsadresserSøker,
                fødselsdatoForBeskjæring = hentInnflytningsdatoForBeskjæring(bostedsadresserBarn, bostedsadresserSøker),
            )

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
                    begrunnelse = PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + periode.verdi.begrunnelse,
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                    erOpprinneligPreutfylt = true,
                )
            }.toSet()
    }

    private fun hentInnflytningsdatoForBeskjæring(
        bostedsadresserBarn: List<Bostedsadresse>,
        bostedsadresserSøker: List<Bostedsadresse>,
    ): LocalDate {
        val innflytningsdatoForBeskjæringBarn =
            bostedsadresserBarn
                .mapNotNull { it.gyldigFraOgMed }
                .minOrNull() ?: PRAKTISK_TIDLIGSTE_DAG

        val innflytningsdatoForBeskjæringSøker =
            bostedsadresserSøker
                .mapNotNull { it.gyldigFraOgMed }
                .minOrNull() ?: PRAKTISK_TIDLIGSTE_DAG

        return maxOf(innflytningsdatoForBeskjæringBarn, innflytningsdatoForBeskjæringSøker)
    }

    private fun lagBorHosSøkerTidslinje(
        bostedsadresserBarn: List<Bostedsadresse>,
        bostedsadresserSøker: List<Bostedsadresse>,
        fødselsdatoForBeskjæring: LocalDate,
    ): Tidslinje<Delvilkår> {
        val bostedsadresserBarnTidslinje = lagBostedsadresseTidslinje(bostedsadresserBarn, fødselsdatoForBeskjæring)
        val bostedsadresserSøkerTidslinje = lagBostedsadresseTidslinje(bostedsadresserSøker, fødselsdatoForBeskjæring)

        return bostedsadresserBarnTidslinje.kombinerMed(bostedsadresserSøkerTidslinje) { barnAdresse, søkerAdresse ->
            if (barnAdresse != null && harVærtSammeAdresseMinst3Mnd(barnAdresse, søkerAdresse)) {
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

    private fun harVærtSammeAdresseMinst3Mnd(
        barnAdresse: Bostedsadresse,
        søkerAdresse: Bostedsadresse?,
    ): Boolean =
        søkerAdresse
            ?.takeIf { barnAdresse.erSammeAdresse(it) }
            ?.let { ChronoUnit.MONTHS.between(barnAdresse.gyldigFraOgMed, barnAdresse.gyldigTilOgMed ?: LocalDate.now()) >= 3 }
            ?: false

    private fun Bostedsadresse.erSammeAdresse(søkersAdresse: Bostedsadresse?): Boolean =
        søkersAdresse != null && (
            (this.vegadresse != null && this.vegadresse == søkersAdresse.vegadresse) ||
                (this.matrikkeladresse != null && this.matrikkeladresse == søkersAdresse.matrikkeladresse)
        )
}
