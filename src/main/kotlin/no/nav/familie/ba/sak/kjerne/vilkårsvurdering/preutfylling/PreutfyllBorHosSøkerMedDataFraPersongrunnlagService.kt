package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.erSammeAdresse
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
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
class PreutfyllBorHosSøkerMedDataFraPersongrunnlagService(
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering: Vilkårsvurdering) {
        val personOpplysningsgrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id)
        val bostedsadresserSøker = Adresser.opprettFra(personOpplysningsgrunnlag.søker)

        vilkårsvurdering.personResultater
            .filterNot { it.erSøkersResultater() }
            .forEach { personResultat ->
                val personInfo = personOpplysningsgrunnlag.personer.find { it.aktør == personResultat.aktør } ?: throw Feil("Aktør ${personResultat.aktør.aktørId} har personresultat men ikke persongrunnlag")

                val adresserForPerson =
                    Adresser.opprettFra(personInfo)

                val borFastHosSøkerVilkårResultat = genererBorHosSøkerVilkårResultat(personResultat, adresserForPerson, bostedsadresserSøker)

                if (borFastHosSøkerVilkårResultat.isNotEmpty()) {
                    personResultat.vilkårResultater.removeIf { it.vilkårType == BOR_MED_SØKER }
                    personResultat.vilkårResultater.addAll(borFastHosSøkerVilkårResultat)
                }
            }
    }

    private fun genererBorHosSøkerVilkårResultat(
        personResultat: PersonResultat,
        bostedsadresserBarn: Adresser,
        bostedsadresserSøker: Adresser,
    ): Set<VilkårResultat> {
        val harSammeBostedsadresseTidslinje =
            lagBorHosSøkerTidslinje(
                bostedsadresserBarn = bostedsadresserBarn.bostedsadresser,
                bostedsadresserSøker = bostedsadresserSøker.bostedsadresser,
                cutOffFomDato = hentInnflytningsdatoForBeskjæring(bostedsadresserBarn.bostedsadresser, bostedsadresserSøker.bostedsadresser),
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
        bostedsadresserBarn: List<Adresse>,
        bostedsadresserSøker: List<Adresse>,
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
        bostedsadresserBarn: List<Adresse>,
        bostedsadresserSøker: List<Adresse>,
        cutOffFomDato: LocalDate,
    ): Tidslinje<Delvilkår> {
        val bostedsadresserBarnTidslinje = lagBostedsadresseTidslinje(bostedsadresserBarn, cutOffFomDato)
        val bostedsadresserSøkerTidslinje = lagBostedsadresseTidslinje(bostedsadresserSøker, cutOffFomDato)

        return bostedsadresserBarnTidslinje.kombinerMed(bostedsadresserSøkerTidslinje) { barnAdresse, søkerAdresse ->
            if (barnAdresse != null && harVærtSammeAdresseMinst3Mnd(barnAdresse, søkerAdresse)) {
                OppfyltDelvilkår(begrunnelse = "- Har samme bostedsadresse som søker.")
            } else {
                IkkeOppfyltDelvilkår()
            }
        }
    }

    private fun lagBostedsadresseTidslinje(
        bostedsadresser: List<Adresse>,
        cutOffFomDato: LocalDate,
    ): Tidslinje<Adresse> =
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
            .beskjærFraOgMed(cutOffFomDato)

    private fun harVærtSammeAdresseMinst3Mnd(
        barnAdresse: Adresse,
        søkerAdresse: Adresse?,
    ): Boolean =
        søkerAdresse
            ?.takeIf { barnAdresse.erSammeAdresse(it) }
            ?.let { ChronoUnit.MONTHS.between(barnAdresse.gyldigFraOgMed, barnAdresse.gyldigTilOgMed ?: LocalDate.now()) >= 3 }
            ?: false
}
