package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårKanskjeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.lagErNordiskStatsborgerTidslinje
import no.nav.familie.ba.sak.kjerne.søknad.Søknad
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.utils.erMinst12Måneder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_FRA_SØKNAD
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.omfatter
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrer
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PreutfyllBosattIRiketService(
    private val søknadService: SøknadService,
    persongrunnlagService: PersongrunnlagService,
) : AbstractPreutfyllBosattIRiketService(persongrunnlagService) {
    override fun genererBosattIRiketVilkårResultat(
        behandling: Behandling,
        personResultat: PersonResultat,
        datoForBeskjæringAvFom: LocalDate,
        person: Person,
    ): Set<VilkårResultat> {
        val adresserForPerson = Adresser.opprettFra(person)

        val erNordiskStatsborgerTidslinje = lagErNordiskStatsborgerTidslinje(person.statsborgerskap)

        val erBosattINorgeEllerPåSvalbardTidslinje = adresserForPerson.lagErBosattINorgeEllerSvalbardTidslinje()

        val erNordiskStatsborgerOgBosattINorgeTidslinje =
            erNordiskStatsborgerTidslinje.kombinerMed(erBosattINorgeEllerPåSvalbardTidslinje) { erNordiskStatsborger, erBosattINorge ->
                if (erNordiskStatsborger == true && erBosattINorge == true) {
                    OppfyltDelvilkår(begrunnelse = "- Norsk/nordisk statsborgerskap")
                } else {
                    IkkeOppfyltDelvilkår()
                }
            }

        val erØvrigeKravForBosattIRiketOppfyltTidslinje =
            lagErØvrigeKravForBosattIRiketOppfyltTidslinje(
                erBosattINorgeEllerPåSvalbardTidslinje = erBosattINorgeEllerPåSvalbardTidslinje,
                personResultat = personResultat,
                person = person,
            )

        val førsteBosattINorgeDato = erBosattINorgeEllerPåSvalbardTidslinje.filtrer { it == true }.startsTidspunkt

        return erNordiskStatsborgerOgBosattINorgeTidslinje
            .kombinerMed(erØvrigeKravForBosattIRiketOppfyltTidslinje) { erNordiskOgBosatt, erØvrigeKravOppfylt ->
                when {
                    erNordiskOgBosatt is OppfyltDelvilkår -> erNordiskOgBosatt
                    erØvrigeKravOppfylt is OppfyltDelvilkår || erØvrigeKravOppfylt is IkkeVurdertVilkår -> erØvrigeKravOppfylt
                    else -> IkkeOppfyltDelvilkår(evalueringÅrsaker = (erNordiskOgBosatt?.evalueringÅrsaker.orEmpty() + erØvrigeKravOppfylt?.evalueringÅrsaker.orEmpty()))
                }
            }.vurderFinnmarkOgSvalbardtillegg(adresserForPerson)
            .beskjærFraOgMed(maxOf(datoForBeskjæringAvFom, førsteBosattINorgeDato))
            .tilPerioderIkkeNull()
            .tilVilkårResultater(personResultat)
    }

    private fun lagErØvrigeKravForBosattIRiketOppfyltTidslinje(
        erBosattINorgeEllerPåSvalbardTidslinje: Tidslinje<Boolean>,
        personResultat: PersonResultat,
        person: Person,
    ): Tidslinje<Delvilkår> =
        erBosattINorgeEllerPåSvalbardTidslinje
            .tilPerioder()
            .map { erBosattINorgePeriode ->
                Periode(
                    verdi =
                        when (erBosattINorgePeriode.verdi) {
                            true -> sjekkØvrigeKravForPeriode(erBosattINorgePeriode, personResultat, person)
                            else -> IkkeOppfyltDelvilkår(evalueringÅrsaker = setOf(VilkårIkkeOppfyltÅrsak.BOR_IKKE_I_RIKET))
                        },
                    fom = erBosattINorgePeriode.fom,
                    tom = erBosattINorgePeriode.tom,
                )
            }.tilTidslinje()

    private fun sjekkØvrigeKravForPeriode(
        erBosattINorgePeriode: Periode<Boolean?>,
        personResultat: PersonResultat,
        person: Person,
    ): Delvilkår {
        val digitalSøknad by lazy { søknadService.finnDigitalSøknad(behandlingId = personResultat.vilkårsvurdering.behandling.id) }

        return when {
            erBosattINorgePeriode.erMinst12Måneder() -> {
                OppfyltDelvilkår("- Norsk bostedsadresse i minst 12 måneder.")
            }

            erBosattINorgePeriode.omfatter(person.fødselsdato) -> {
                OppfyltDelvilkår("- Bosatt i Norge siden fødsel.")
            }

            digitalSøknad == null -> {
                IkkeVurdertVilkår(evalueringÅrsaker = setOf(VilkårKanskjeOppfyltÅrsak.BOSATT_I_RIKET_IKKE_MULIG_Å_FASTSETTE_SKAL_BO_LENGRE_ENN_12_MND))
            }

            erBosattINorgePeriode.omfatter(LocalDate.now()) && digitalSøknad != null && erOppgittAtPlanleggerÅBoINorge12Måneder(digitalSøknad!!, personResultat) -> {
                OppfyltDelvilkår("- Oppgitt i søknad at planlegger å bo i Norge i minst 12 måneder.", INFORMASJON_FRA_SØKNAD)
            }

            else -> {
                IkkeOppfyltDelvilkår(evalueringÅrsaker = setOf(VilkårIkkeOppfyltÅrsak.HAR_IKKE_BODD_I_RIKET_12_MND))
            }
        }
    }

    private fun erOppgittAtPlanleggerÅBoINorge12Måneder(
        digitalSøknad: Søknad,
        personResultat: PersonResultat,
    ): Boolean =
        if (personResultat.erSøkersResultater()) {
            digitalSøknad.søker.planleggerÅBoINorge12Mnd
        } else {
            digitalSøknad.barn.find { it.fnr == personResultat.aktør.aktivFødselsnummer() }?.planleggerÅBoINorge12Mnd ?: false
        }
}
