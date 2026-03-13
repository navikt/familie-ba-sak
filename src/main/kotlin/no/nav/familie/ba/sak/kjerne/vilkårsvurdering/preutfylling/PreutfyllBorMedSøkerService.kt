package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.erSammeAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.tilPerson
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_OM_DELT_BOSTED
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class PreutfyllBorMedSøkerService(
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllBorMedSøker(vilkårsvurdering: Vilkårsvurdering) {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id)
        val bostedsadresserSøker = Adresser.opprettFra(personopplysningGrunnlag.søker)

        vilkårsvurdering.personResultater
            .filterNot { it.erSøkersResultater() }
            .forEach { personResultat ->
                val barn = personResultat.aktør.tilPerson(personopplysningGrunnlag)
                val adresserForBarn = Adresser.opprettFra(barn)

                val nyeBorMedSøkerVilkårResultat =
                    genererBorMedSøkerVilkårResultat(
                        personResultat = personResultat,
                        bostedsadresserBarn = adresserForBarn,
                        bostedsadresserSøker = bostedsadresserSøker,
                        barnFødselsdato = barn.fødselsdato,
                    )

                if (nyeBorMedSøkerVilkårResultat.isNotEmpty()) {
                    personResultat.vilkårResultater.removeIf { it.vilkårType == BOR_MED_SØKER }
                    personResultat.vilkårResultater.addAll(nyeBorMedSøkerVilkårResultat)
                }
            }
    }

    private fun genererBorMedSøkerVilkårResultat(
        personResultat: PersonResultat,
        bostedsadresserBarn: Adresser,
        bostedsadresserSøker: Adresser,
        barnFødselsdato: LocalDate,
    ): Set<VilkårResultat> {
        val harSammeBostedsadresseTidslinje =
            lagBorMedSøkerTidslinje(
                bostedsadresserBarn = bostedsadresserBarn,
                bostedsadresserSøker = bostedsadresserSøker,
                datoForBeskjæringAvFom = finnDatoForBeskjæringAvFom(bostedsadresserBarn.bostedsadresser, bostedsadresserSøker.bostedsadresser),
                barnFødselsdato = barnFødselsdato,
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
                    begrunnelseForManuellKontroll = periode.verdi.begrunnelseForManuellKontroll,
                )
            }.toSet()
    }

    private fun finnDatoForBeskjæringAvFom(
        bostedsadresserBarn: List<Adresse>,
        bostedsadresserSøker: List<Adresse>,
    ): LocalDate {
        val tidligsteFomDatoPåBostedadresseBarn =
            bostedsadresserBarn
                .mapNotNull { it.gyldigFraOgMed }
                .minOrNull() ?: PRAKTISK_TIDLIGSTE_DAG

        val tidligsteFomDatoPåBostedadresseSøker =
            bostedsadresserSøker
                .mapNotNull { it.gyldigFraOgMed }
                .minOrNull() ?: PRAKTISK_TIDLIGSTE_DAG

        return maxOf(tidligsteFomDatoPåBostedadresseBarn, tidligsteFomDatoPåBostedadresseSøker)
    }

    private fun lagBorMedSøkerTidslinje(
        bostedsadresserBarn: Adresser,
        bostedsadresserSøker: Adresser,
        datoForBeskjæringAvFom: LocalDate,
        barnFødselsdato: LocalDate,
    ): Tidslinje<Delvilkår> {
        val bostedsadresserBarnTidslinje = lagBostedsadresseTidslinje(bostedsadresserBarn.bostedsadresser)
        val bostedsadresserSøkerTidslinje = lagBostedsadresseTidslinje(bostedsadresserSøker.bostedsadresser)

        val deltBostedsadresserBarnTidslinjer =
            bostedsadresserBarn.delteBosteder
                .map {
                    Periode(
                        verdi = it,
                        fom = it.gyldigFraOgMed,
                        tom = it.gyldigTilOgMed,
                    ).tilTidslinje()
                }.kombiner { it.toList() }

        return bostedsadresserSøkerTidslinje
            .kombinerMed(bostedsadresserBarnTidslinje, deltBostedsadresserBarnTidslinjer) { søkerAdresse, barnBostedAdresse, barnDeltBostedAdresser ->
                when {
                    barnBostedAdresse != null && harVærtSammeAdresseMinst3MndEllerBarnUnder3MndOgBoddSidenFødsel(barnBostedAdresse, søkerAdresse, barnFødselsdato) -> {
                        OppfyltDelvilkår(begrunnelse = "- Har samme bostedsadresse som søker.")
                    }

                    !barnDeltBostedAdresser.isNullOrEmpty() && harVærtSammeDeltbostedAdresseMinst3MndEllerBarnUnder3MndOgBoddSidenFødsel(barnDeltBostedAdresser, søkerAdresse, barnFødselsdato) -> {
                        OppfyltDelvilkår(
                            begrunnelse = "- Har delt bostedsadresse hos søker.",
                            begrunnelseForManuellKontroll = INFORMASJON_OM_DELT_BOSTED,
                        )
                    }

                    else -> {
                        IkkeOppfyltDelvilkår(begrunnelse = "- Har ikke samme fast eller delt bostedsadresse som søker")
                    }
                }
            }.beskjærFraOgMed(datoForBeskjæringAvFom)
    }

    private fun lagBostedsadresseTidslinje(
        bostedsadresser: List<Adresse>,
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

    private fun harVærtSammeAdresseMinst3MndEllerBarnUnder3MndOgBoddSidenFødsel(
        barnAdresse: Adresse,
        søkerAdresse: Adresse?,
        barnFødselsdato: LocalDate,
    ): Boolean {
        if (!barnAdresse.erSammeAdresse(søkerAdresse)) return false

        val fom = barnAdresse.gyldigFraOgMed
        val tom = barnAdresse.gyldigTilOgMed ?: LocalDate.now()

        val boddMinstTreMnd = ChronoUnit.MONTHS.between(fom, tom) >= 3

        return boddMinstTreMnd || barnUnder3MndOgBoddSidenFødsel(fom, barnFødselsdato)
    }

    private fun harVærtSammeDeltbostedAdresseMinst3MndEllerBarnUnder3MndOgBoddSidenFødsel(
        barnAdresser: List<Adresse>,
        søkerAdresse: Adresse?,
        barnFødselsdato: LocalDate,
    ): Boolean =
        barnAdresser.any {
            harVærtSammeAdresseMinst3MndEllerBarnUnder3MndOgBoddSidenFødsel(it, søkerAdresse, barnFødselsdato)
        }

    private fun barnUnder3MndOgBoddSidenFødsel(
        adresseFom: LocalDate?,
        fødselsdato: LocalDate,
    ): Boolean =
        ChronoUnit.MONTHS.between(fødselsdato, LocalDate.now()) < 3 &&
            adresseFom != null &&
            adresseFom <= fødselsdato
}
