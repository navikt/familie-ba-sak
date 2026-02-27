package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.utils.erMinst6Måneder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.omfatter
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrer
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PreutfyllBosattIRiketForFødselshendelserService(
    persongrunnlagService: PersongrunnlagService,
) : AbstractPreutfyllBosattIRiketService(persongrunnlagService) {
    override fun genererBosattIRiketVilkårResultat(
        behandling: Behandling,
        personResultat: PersonResultat,
        datoForBeskjæringAvFom: LocalDate,
        person: Person,
    ): Set<VilkårResultat> {
        val adresserForPerson = Adresser.opprettFra(person)

        val erBosattINorgeEllerPåSvalbardTidslinje = adresserForPerson.lagErBosattINorgeEllerSvalbardTidslinje()
        val førsteBosattINorgeDato = erBosattINorgeEllerPåSvalbardTidslinje.filtrer { it == true }.startsTidspunkt

        val erKravForBosattIRiketOppfyltTidslinje =
            lagErKravForBosattIRiketOppfyltTidslinje(
                erBosattINorgeEllerPåSvalbardTidslinje = erBosattINorgeEllerPåSvalbardTidslinje,
                person = person,
            )

        return erKravForBosattIRiketOppfyltTidslinje
            .vurderFinnmarkOgSvalbardtillegg(adresserForPerson)
            .beskjærFraOgMed(maxOf(datoForBeskjæringAvFom, førsteBosattINorgeDato))
            .tilPerioderIkkeNull()
            .tilVilkårResultater(personResultat)
    }

    private fun lagErKravForBosattIRiketOppfyltTidslinje(
        erBosattINorgeEllerPåSvalbardTidslinje: Tidslinje<Boolean>,
        person: Person,
    ): Tidslinje<Delvilkår> =
        erBosattINorgeEllerPåSvalbardTidslinje
            .tilPerioder()
            .map { erBosattINorgePeriode ->
                Periode(
                    verdi =
                        when (erBosattINorgePeriode.verdi) {
                            true -> sjekkØvrigeKravForPeriode(erBosattINorgePeriode, person)
                            else -> IkkeOppfyltDelvilkår(ikkeOppfyltEvalueringÅrsaker = setOf(VilkårIkkeOppfyltÅrsak.BOR_IKKE_I_RIKET))
                        },
                    fom = erBosattINorgePeriode.fom,
                    tom = erBosattINorgePeriode.tom,
                )
            }.tilTidslinje()

    private fun sjekkØvrigeKravForPeriode(
        erBosattINorgePeriode: Periode<Boolean?>,
        person: Person,
    ): Delvilkår =
        when (person.type) {
            PersonType.SØKER -> {
                if (erBosattINorgePeriode.erMinst6Måneder()) {
                    OppfyltDelvilkår("- Norsk bostedsadresse i minst 6 måneder.")
                } else {
                    IkkeOppfyltDelvilkår(ikkeOppfyltEvalueringÅrsaker = setOf(VilkårIkkeOppfyltÅrsak.HAR_IKKE_BODD_I_RIKET_6_MND))
                }
            }

            PersonType.BARN -> {
                if (erBosattINorgePeriode.omfatter(person.fødselsdato)) {
                    OppfyltDelvilkår("- Bosatt i Norge siden fødsel.")
                } else {
                    IkkeOppfyltDelvilkår(ikkeOppfyltEvalueringÅrsaker = setOf(VilkårIkkeOppfyltÅrsak.BOR_IKKE_I_RIKET))
                }
            }

            else -> {
                throw IllegalStateException("Uventet person type ${person.type} ved sjekk av øvrige krav for bosatt i riket i fødselshendelse")
            }
        }
}
