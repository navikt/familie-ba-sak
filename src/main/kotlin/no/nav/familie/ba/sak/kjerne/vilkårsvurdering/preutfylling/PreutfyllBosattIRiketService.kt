package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.erUkraina
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_FRA_SØKNAD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.ba.sak.task.dto.AktørId
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
import java.time.temporal.ChronoUnit

@Service
class PreutfyllBosattIRiketService(
    private val pdlRestKlient: SystemOnlyPdlRestKlient,
    private val søknadService: SøknadService,
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllBosattIRiket(
        vilkårsvurdering: Vilkårsvurdering,
        identerVilkårSkalPreutfyllesFor: List<String>? = null,
        cutOffFomDato: LocalDate? = null,
    ) {
        val behandling = vilkårsvurdering.behandling
        val identer =
            vilkårsvurdering
                .personResultater
                .map { it.aktør.aktivFødselsnummer() }
                .filter { identerVilkårSkalPreutfyllesFor?.contains(it) ?: true }

        val adresser = pdlRestKlient.hentAdresserForPersoner(identer)

        vilkårsvurdering
            .personResultater
            .filter { it.aktør.aktivFødselsnummer() in identer }
            .filterNot {
                val erUkrainskStatsborger = hentErUkrainskStatsborger(it.aktør)
                erUkrainskStatsborger && !behandling.erFinnmarksEllerSvalbardtillegg()
            }.forEach { personResultat ->

                val fødselsdatoForBeskjæring = finnFødselsdatoForBeskjæring(personResultat)
                val adresserForPerson = Adresser.opprettFra(adresser[personResultat.aktør.aktivFødselsnummer()])

                val nyeBosattIRiketVilkårResultater =
                    genererBosattIRiketVilkårResultat(
                        personResultat = personResultat,
                        fødselsdatoForBeskjæring = fødselsdatoForBeskjæring,
                        adresserForPerson = adresserForPerson,
                        behandling = behandling,
                    ).let { bosattIRiketVilkårResultat ->
                        if (cutOffFomDato != null) {
                            val eksisterendeBosattIRiketVilkårResultater = personResultat.vilkårResultater.filter { it.vilkårType == BOSATT_I_RIKET }
                            kombinerNyeOgGamleVilkårResultater(
                                nyeBosattIRiketVilkårResultaterTidslinje = bosattIRiketVilkårResultat.tilTidslinje().beskjærFraOgMed(cutOffFomDato),
                                eksisterendeBosattIRiketVilkårResultaterTidslinje = eksisterendeBosattIRiketVilkårResultater.tilTidslinje(),
                            )
                        } else {
                            bosattIRiketVilkårResultat
                        }
                    }

                if (nyeBosattIRiketVilkårResultater.isNotEmpty()) {
                    personResultat.vilkårResultater.removeIf { it.vilkårType == BOSATT_I_RIKET }
                    personResultat.vilkårResultater.addAll(nyeBosattIRiketVilkårResultater)
                }
            }
    }

    fun genererBosattIRiketVilkårResultat(
        personResultat: PersonResultat,
        fødselsdatoForBeskjæring: LocalDate = LocalDate.MIN,
        adresserForPerson: Adresser,
        behandling: Behandling,
    ): Set<VilkårResultat> {
        val erBosattINorgeTidslinje = lagErBosattINorgeTidslinje(adresserForPerson, personResultat)
        val erNordiskStatsborgerTidslinje = pdlRestKlient.lagErNordiskStatsborgerTidslinje(personResultat)
        val erBostedsadresseIFinnmarkEllerNordTromsTidslinje = lagErBostedsadresseIFinnmarkEllerNordTromsTidslinje(adresserForPerson, personResultat)
        val erDeltBostedIFinnmarkEllerNordTromsTidslinje = lagErDeltBostedIFinnmarkEllerNordTromsTidslinje(adresserForPerson, personResultat)
        val erOppholdsadressePåSvalbardTidslinje = lagErOppholdsadresserPåSvalbardTidslinje(adresserForPerson, personResultat)

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

        if (behandling.erFinnmarksEllerSvalbardtillegg()) {
            validerKombinasjonerAvAdresserForFinnmarksOgSvalbardtileggbehandlinger(
                behandling = behandling,
                erDeltBostedIFinnmarkEllerNordTromsTidslinje = erDeltBostedIFinnmarkEllerNordTromsTidslinje,
                erOppholdsadressePåSvalbardTidslinje = erOppholdsadressePåSvalbardTidslinje,
            )
        }

        val erNordiskStatsborgerOgBosattINorgeTidslinje =
            erNordiskStatsborgerTidslinje.kombinerMed(erBosattINorgeEllerPåSvalbardTidslinje) { erNordiskStatsborger, erBosattINorge ->
                if (erNordiskStatsborger == true && erBosattINorge == true) {
                    OppfyltDelvilkår(begrunnelse = "- Norsk/nordisk statsborgerskap")
                } else {
                    IkkeOppfyltDelvilkår
                }
            }

        val erØvrigeKravForBosattIRiketOppfyltTidslinje = lagErØvrigeKravForBosattIRiketOppfyltTidslinje(erBosattINorgeEllerPåSvalbardTidslinje, personResultat)

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

                        else -> erBosattIRiket
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

    private fun hentErUkrainskStatsborger(aktør: Aktør): Boolean {
        val statsborgerskap = pdlRestKlient.hentStatsborgerskap(aktør)
        return statsborgerskap.any { it.erUkraina() }
    }

    private fun lagErØvrigeKravForBosattIRiketOppfyltTidslinje(
        erBosattINorgeEllerPåSvalbardTidslinje: Tidslinje<Boolean>,
        personResultat: PersonResultat,
    ): Tidslinje<Delvilkår> =
        erBosattINorgeEllerPåSvalbardTidslinje
            .tilPerioder()
            .map { erBosattINorgePeriode ->
                Periode(
                    verdi =
                        when (erBosattINorgePeriode.verdi) {
                            true -> sjekkØvrigeKravForPeriode(erBosattINorgePeriode, personResultat)
                            else -> IkkeOppfyltDelvilkår
                        },
                    fom = erBosattINorgePeriode.fom,
                    tom = erBosattINorgePeriode.tom,
                )
            }.tilTidslinje()

    private fun sjekkØvrigeKravForPeriode(
        erBosattINorgePeriode: Periode<Boolean?>,
        personResultat: PersonResultat,
    ): Delvilkår =
        when {
            erBosattINorgePeriode.erMinst12Måneder() ->
                OppfyltDelvilkår("- Norsk bostedsadresse i minst 12 måneder.")

            erFødselsdatoIPeriode(personResultat.vilkårsvurdering.behandling.id, personResultat.aktør.aktørId, erBosattINorgePeriode) ->
                OppfyltDelvilkår("- Bosatt i Norge siden fødsel.")

            erBosattINorgePeriode.omfatter(LocalDate.now()) && erOppgittAtPlanleggerÅBoINorge12Måneder(personResultat) ->
                OppfyltDelvilkår("- Oppgitt i søknad at planlegger å bo i Norge i minst 12 måneder.", INFORMASJON_FRA_SØKNAD)

            else -> IkkeOppfyltDelvilkår
        }

    private fun Periode<*>.erMinst12Måneder(): Boolean = ChronoUnit.MONTHS.between(fom, tom ?: LocalDate.now()) >= 12

    private fun erOppgittAtPlanleggerÅBoINorge12Måneder(personResultat: PersonResultat): Boolean {
        val søknad = søknadService.finnSøknad(behandlingId = personResultat.vilkårsvurdering.behandling.id) ?: return false
        return if (personResultat.erSøkersResultater()) {
            søknad.søker.planleggerÅBoINorge12Mnd
        } else {
            søknad.barn.find { it.fnr == personResultat.aktør.aktivFødselsnummer() }?.planleggerÅBoINorge12Mnd ?: false
        }
    }

    fun finnFødselsdatoForBeskjæring(
        personResultat: PersonResultat,
    ): LocalDate {
        val barna = persongrunnlagService.hentAktivThrows(personResultat.vilkårsvurdering.behandling.id).barna
        val fødselsdatoForBeskjæring =
            if (personResultat.erSøkersResultater()) {
                barna.minOfOrNull { it.fødselsdato }
            } else {
                barna.find { it.aktør.aktørId == personResultat.aktør.aktørId }?.fødselsdato
            }
        return fødselsdatoForBeskjæring ?: LocalDate.MIN
    }

    private fun erFødselsdatoIPeriode(
        behandlingId: Long,
        aktørId: AktørId,
        erBosattINorgePeriode: Periode<Boolean?>,
    ): Boolean {
        val fødselsdato =
            persongrunnlagService
                .hentAktivThrows(behandlingId)
                .søkerOgBarn
                .find { it.aktør.aktørId == aktørId }
                ?.fødselsdato ?: throw Feil("Finner ikke barn med aktørId $aktørId i persongrunnlag for behandlingId $behandlingId")
        return erBosattINorgePeriode.omfatter(fødselsdato)
    }

    private fun lagErBosattINorgeTidslinje(
        adresser: Adresser,
        personResultat: PersonResultat,
    ): Tidslinje<Boolean> {
        val filtrerteAdresser = filtrereUgyldigeAdresser(adresser.bostedsadresser)
        return lagTidslinjeForAdresser(filtrerteAdresser, personResultat, "Bostedadresse") { it.erINorge() }
    }
}

private fun kombinerNyeOgGamleVilkårResultater(
    nyeBosattIRiketVilkårResultaterTidslinje: Tidslinje<VilkårResultat>,
    eksisterendeBosattIRiketVilkårResultaterTidslinje: Tidslinje<VilkårResultat>,
): Collection<VilkårResultat> =
    nyeBosattIRiketVilkårResultaterTidslinje
        .kombinerMed(eksisterendeBosattIRiketVilkårResultaterTidslinje) { nytt, gammelt -> nytt ?: gammelt }
        .tilPerioderIkkeNull()
        .map {
            it.verdi.copy(
                periodeFom = it.fom,
                periodeTom = it.tom,
            )
        }
