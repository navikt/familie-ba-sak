package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.iUkraina
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.lagErNordiskStatsborgerTidslinje
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.utils.erMinst12Måneder
import no.nav.familie.ba.sak.kjerne.tidslinje.utils.erMinst12MånederMedNullTomSomUendelig
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_FRA_SØKNAD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
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
import kotlin.collections.filter

@Service
class PreutfyllBosattIRiketMedLagringIPersonopplyningsgrunnlagService(
    private val søknadService: SøknadService,
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllBosattIRiket(
        vilkårsvurdering: Vilkårsvurdering,
        identerVilkårSkalPreutfyllesFor: List<String>? = null,
    ) {
        val behandling = vilkårsvurdering.behandling
        val identer =
            vilkårsvurdering
                .personResultater
                .map { it.aktør.aktivFødselsnummer() }
                .filter { identerVilkårSkalPreutfyllesFor?.contains(it) ?: true }

        val personOpplysningsgrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)

        vilkårsvurdering
            .personResultater
            .filter { it.aktør.aktivFødselsnummer() in identer }
            .forEach { personResultat ->
                val person = personOpplysningsgrunnlag.personer.find { it.aktør == personResultat.aktør } ?: throw Feil("Aktør ${personResultat.aktør.aktørId} har personresultat men ikke persongrunnlag")

                if (person.statsborgerskap.iUkraina()) {
                    return@forEach
                }

                val fødselsdatoForBeskjæring = if (person.type == PersonType.SØKER) (personOpplysningsgrunnlag.eldsteBarnSinFødselsdato ?: person.fødselsdato) else person.fødselsdato

                val nyeBosattIRiketVilkårResultater =
                    genererBosattIRiketVilkårResultat(
                        behandling = behandling,
                        personResultat = personResultat,
                        fødselsdatoForBeskjæring = fødselsdatoForBeskjæring,
                        person = person,
                    )

                if (nyeBosattIRiketVilkårResultater.isNotEmpty()) {
                    personResultat.vilkårResultater.removeIf { it.vilkårType == BOSATT_I_RIKET }
                    personResultat.vilkårResultater.addAll(nyeBosattIRiketVilkårResultater)
                }
            }
    }

    private fun genererBosattIRiketVilkårResultat(
        behandling: Behandling,
        personResultat: PersonResultat,
        fødselsdatoForBeskjæring: LocalDate,
        person: Person,
    ): Set<VilkårResultat> {
        val adresserForPerson = Adresser.opprettFra(person)

        val erBosattINorgeTidslinje = adresserForPerson.lagErBosattINorgeTidslinje()

        val erNordiskStatsborgerTidslinje = lagErNordiskStatsborgerTidslinje(person.statsborgerskap)
        val erBostedsadresseIFinnmarkEllerNordTromsTidslinje = adresserForPerson.lagErBostedsadresseIFinnmarkEllerNordTromsTidslinje()
        val erDeltBostedIFinnmarkEllerNordTromsTidslinje = adresserForPerson.lagErDeltBostedIFinnmarkEllerNordTromsTidslinje()
        val erOppholdsadressePåSvalbardTidslinje = adresserForPerson.lagErOppholdsadresserPåSvalbardTidslinje()

        val erBosattINorgeEllerPåSvalbardTidslinje =
            erBosattINorgeTidslinje
                .kombinerMed(erOppholdsadressePåSvalbardTidslinje) { bosattINorge, bosattPåSvalbard ->
                    when {
                        bosattINorge == true || bosattPåSvalbard == true -> true
                        bosattINorge == false && bosattPåSvalbard == false -> false
                        else -> null
                    }
                }.tilPerioderIkkeNull()
                .tilTidslinje()

        val erNordiskStatsborgerOgBosattINorgeTidslinje =
            erNordiskStatsborgerTidslinje.kombinerMed(erBosattINorgeEllerPåSvalbardTidslinje) { erNordiskStatsborger, erBosattINorge ->
                if (erNordiskStatsborger == true && erBosattINorge == true) {
                    OppfyltDelvilkår(begrunnelse = "- Norsk/nordisk statsborgerskap")
                } else {
                    IkkeOppfyltDelvilkår
                }
            }

        val erØvrigeKravForBosattIRiketOppfyltTidslinje =
            lagErØvrigeKravForBosattIRiketOppfyltTidslinje(
                behandlingÅrsak = behandling.opprettetÅrsak,
                erBosattINorgeEllerPåSvalbardTidslinje = erBosattINorgeEllerPåSvalbardTidslinje,
                personResultat = personResultat,
                person = person,
            )

        val erBosattIFinnmarkEllerNordTromsTidslinje =
            erBostedsadresseIFinnmarkEllerNordTromsTidslinje.kombinerMed(erDeltBostedIFinnmarkEllerNordTromsTidslinje) { erBostedsadresseIFinnmarkEllerNordTroms, erDeltBostedIFinnmarkEllerNordTroms ->
                erBostedsadresseIFinnmarkEllerNordTroms == true || erDeltBostedIFinnmarkEllerNordTroms == true
            }

        val førsteBosattINorgeDato = erBosattINorgeEllerPåSvalbardTidslinje.filtrer { it == true }.startsTidspunkt

        val erBosattIRiketTidslinje =
            erNordiskStatsborgerOgBosattINorgeTidslinje
                .kombinerMed(erØvrigeKravForBosattIRiketOppfyltTidslinje) { erNordiskOgBosatt, erØvrigeKravOppfylt ->
                    when {
                        erNordiskOgBosatt is OppfyltDelvilkår -> erNordiskOgBosatt
                        erØvrigeKravOppfylt is OppfyltDelvilkår -> erØvrigeKravOppfylt
                        else -> IkkeOppfyltDelvilkår
                    }
                }.kombinerMed(erBosattIFinnmarkEllerNordTromsTidslinje, erOppholdsadressePåSvalbardTidslinje) { erBosattIRiket, erBosattIFinnmarkEllerNordTroms, erOppholdsadressePåSvalbard ->
                    when (erBosattIRiket) {
                        is OppfyltDelvilkår -> {
                            val utdypendeVilkårsvurderinger =
                                when {
                                    erOppholdsadressePåSvalbard == true -> listOf(BOSATT_PÅ_SVALBARD)
                                    erBosattIFinnmarkEllerNordTroms == true -> listOf(BOSATT_I_FINNMARK_NORD_TROMS)
                                    else -> emptyList()
                                }
                            erBosattIRiket.copy(utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger)
                        }

                        else -> {
                            erBosattIRiket
                        }
                    }
                }.beskjærFraOgMed(maxOf(fødselsdatoForBeskjæring, førsteBosattINorgeDato))

        return erBosattIRiketTidslinje
            .tilPerioderIkkeNull()
            .map { erBosattINorgePeriode ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = erBosattINorgePeriode.verdi.tilResultat(),
                    vilkårType = BOSATT_I_RIKET,
                    periodeFom = erBosattINorgePeriode.fom,
                    periodeTom = erBosattINorgePeriode.tom,
                    begrunnelse = PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + erBosattINorgePeriode.verdi.begrunnelse,
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                    begrunnelseForManuellKontroll = erBosattINorgePeriode.verdi.begrunnelseForManuellKontroll,
                    utdypendeVilkårsvurderinger = erBosattINorgePeriode.verdi.utdypendeVilkårsvurderinger,
                    erOpprinneligPreutfylt = true,
                )
            }.toSet()
    }

    private fun lagErØvrigeKravForBosattIRiketOppfyltTidslinje(
        behandlingÅrsak: BehandlingÅrsak,
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
                            true -> sjekkØvrigeKravForPeriode(behandlingÅrsak, erBosattINorgePeriode, personResultat, person)
                            else -> IkkeOppfyltDelvilkår
                        },
                    fom = erBosattINorgePeriode.fom,
                    tom = erBosattINorgePeriode.tom,
                )
            }.tilTidslinje()

    private fun sjekkØvrigeKravForPeriode(
        behandlingÅrsak: BehandlingÅrsak,
        erBosattINorgePeriode: Periode<Boolean?>,
        personResultat: PersonResultat,
        person: Person,
    ): Delvilkår =
        when {
            erBosattINorgePeriode.erMinst12Måneder() -> {
                OppfyltDelvilkår("- Norsk bostedsadresse i minst 12 måneder.")
            }

            erBosattINorgePeriode.omfatter(person.fødselsdato) -> {
                OppfyltDelvilkår("- Bosatt i Norge siden fødsel.")
            }

            erBosattINorgePeriode.omfatter(LocalDate.now()) && erOppgittAtPlanleggerÅBoINorge12Måneder(personResultat) -> {
                OppfyltDelvilkår("- Oppgitt i søknad at planlegger å bo i Norge i minst 12 måneder.", INFORMASJON_FRA_SØKNAD)
            }

            behandlingÅrsak == BehandlingÅrsak.FØDSELSHENDELSE &&
                harMinst12MånederOppholdstillatelseSamtidigSomErBosattINorge(person, erBosattINorgePeriode) &&
                person.type == PersonType.SØKER -> {
                OppfyltDelvilkår("- Søker har oppholdstillatelse i Norge samtidig som bosatt ved fødsel.")
            }

            else -> {
                IkkeOppfyltDelvilkår
            }
        }

    private fun harMinst12MånederOppholdstillatelseSamtidigSomErBosattINorge(
        person: Person,
        erBosattINorgePeriode: Periode<Boolean?>,
    ): Boolean {
        val oppholdstillatelseTidslinjeForPerson = lagOppholdstillatelsePåMinst12MånederTidslinje(person.opphold)

        val harOppholdstillatelseSamtidigSomManErBosatt =
            erBosattINorgePeriode.tilTidslinje().kombinerMed(oppholdstillatelseTidslinjeForPerson) { erBosattINorgePeriode, harOppholdstillatelse ->
                erBosattINorgePeriode == true && harOppholdstillatelse == true
            }

        return harOppholdstillatelseSamtidigSomManErBosatt.tilPerioder().any { it.verdi == true }
    }

    private fun lagOppholdstillatelsePåMinst12MånederTidslinje(
        opphold: List<GrOpphold>,
    ): Tidslinje<Boolean> =
        opphold
            // Godtar kun opphold-perioder med fom-dato og som er permanent eller midlertidig
            .filter { it.gyldigPeriode?.fom != null && (it.type == OPPHOLDSTILLATELSE.PERMANENT || it.type == OPPHOLDSTILLATELSE.MIDLERTIDIG) }
            .map {
                Periode(
                    verdi = true,
                    fom = it.gyldigPeriode?.fom,
                    tom =
                        when {
                            // Godtar null tom for permanent oppholdstillatelse
                            it.type == OPPHOLDSTILLATELSE.PERMANENT -> it.gyldigPeriode?.tom

                            // Setter dagens dato som tom dersom tom er null for midlertidig oppholdstillatelse
                            else -> it.gyldigPeriode?.tom ?: LocalDate.now()
                        },
                )
            }.tilTidslinje()
            .tilPerioderIkkeNull()
            .filter { it.erMinst12MånederMedNullTomSomUendelig() }
            .tilTidslinje()

    private fun erOppgittAtPlanleggerÅBoINorge12Måneder(personResultat: PersonResultat): Boolean {
        val søknad = søknadService.finnSøknad(behandlingId = personResultat.vilkårsvurdering.behandling.id) ?: return false
        return if (personResultat.erSøkersResultater()) {
            søknad.søker.planleggerÅBoINorge12Mnd
        } else {
            søknad.barn.find { it.fnr == personResultat.aktør.aktivFødselsnummer() }?.planleggerÅBoINorge12Mnd ?: false
        }
    }
}
