package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
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
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrer
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val FINNMARK_OG_SVALBARD_MERKING_CUT_OFF_FOM_DATO = LocalDate.of(2025, 9, 1)

@Service
class PreutfyllBosattIRiketService(
    private val pdlRestKlient: SystemOnlyPdlRestKlient,
    private val søknadService: SøknadService,
    private val persongrunnlagService: PersongrunnlagService,
    private val featureToggleService: FeatureToggleService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
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

        val personOpplysningsgrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id).let { persongrunnlagService.oppdaterAdresserPåPersoner(it) }

        vilkårsvurdering
            .personResultater
            .filter { it.aktør.aktivFødselsnummer() in identer }
            .forEach { personResultat ->
                val erUkrainskStatsborger = hentErUkrainskStatsborger(personResultat.aktør)
                if (erUkrainskStatsborger && !behandling.erFinnmarksEllerSvalbardtillegg()) {
                    return@forEach
                }

                val fødselsdatoForBeskjæring = finnFødselsdatoForBeskjæring(personResultat)
                val adresserForPerson =
                    Adresser.opprettFra(
                        personOpplysningsgrunnlag.personer.find { it.aktør.aktørId == personResultat.aktør.aktørId }
                            ?: throw Feil("Fant ikke Person i personopplysningsgrunnlag for aktør ${personResultat.aktør.aktørId}"),
                    )

                val nyeBosattIRiketVilkårResultater =
                    if (behandling.erFinnmarksEllerSvalbardtillegg() && featureToggleService.isEnabled(FeatureToggle.NY_PREUTFYLLING_FOR_BOSATT_I_RIKET_VILKÅR_VED_AUTOVEDTAK_FINNMARK_SVALBARD)) {
                        oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                            personResultat = personResultat,
                            adresserForPerson = adresserForPerson,
                            behandling = behandling,
                        )
                    } else {
                        val bosattIRiketVilkårResultat =
                            genererBosattIRiketVilkårResultat(
                                personResultat = personResultat,
                                fødselsdatoForBeskjæring = fødselsdatoForBeskjæring,
                                adresserForPerson = adresserForPerson,
                                behandling = behandling,
                            )
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
        val personopplysningGrunnlag = persongrunnlagService.oppdaterStatsborgerskapPåPersoner(persongrunnlagService.hentAktivThrows(behandling.id)) // todo må man ha alles statsborgerskap her?
        val erBosattINorgeTidslinje = lagErBosattINorgeTidslinje(adresserForPerson, personResultat)
        val erNordiskStatsborgerTidslinje = lagErNordiskStatsborgerTidslinje(personopplysningGrunnlag)
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

        val andelForAktør = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør)

        if (behandling.erFinnmarksEllerSvalbardtillegg()) {
            validerKombinasjonerAvAdresserForFinnmarksOgSvalbardtileggbehandlinger(
                behandling = behandling,
                erDeltBostedIFinnmarkEllerNordTromsTidslinje = erDeltBostedIFinnmarkEllerNordTromsTidslinje,
                erOppholdsadressePåSvalbardTidslinje = erOppholdsadressePåSvalbardTidslinje,
                andelForAktør = andelForAktør,
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

    fun oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
        personResultat: PersonResultat,
        adresserForPerson: Adresser,
        behandling: Behandling,
    ): List<VilkårResultat> {
        val erBostedsadresseIFinnmarkEllerNordTromsTidslinje = lagErBostedsadresseIFinnmarkEllerNordTromsTidslinje(adresserForPerson, personResultat)
        val erDeltBostedIFinnmarkEllerNordTromsTidslinje = lagErDeltBostedIFinnmarkEllerNordTromsTidslinje(adresserForPerson, personResultat)
        val erOppholdsadressePåSvalbardTidslinje = lagErOppholdsadresserPåSvalbardTidslinje(adresserForPerson, personResultat)
        val andelForAktør = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør)

        validerKombinasjonerAvAdresserForFinnmarksOgSvalbardtileggbehandlinger(
            behandling = behandling,
            erDeltBostedIFinnmarkEllerNordTromsTidslinje = erDeltBostedIFinnmarkEllerNordTromsTidslinje,
            erOppholdsadressePåSvalbardTidslinje = erOppholdsadressePåSvalbardTidslinje,
            andelForAktør = andelForAktør,
        )

        val erBosattIFinnmarkEllerNordTromsTidslinje =
            erBostedsadresseIFinnmarkEllerNordTromsTidslinje.kombinerMed(erDeltBostedIFinnmarkEllerNordTromsTidslinje) { erBostedsadresseIFinnmarkEllerNordTroms, erDeltBostedIFinnmarkEllerNordTroms ->
                erBostedsadresseIFinnmarkEllerNordTroms == true || erDeltBostedIFinnmarkEllerNordTroms == true
            }

        val finnmarkEllerSvalbardmerkingTidslinje =
            erBosattIFinnmarkEllerNordTromsTidslinje
                .kombinerMed(erOppholdsadressePåSvalbardTidslinje) { erBosattIFinnmarkEllerNordTroms, erOppholdsadressePåSvalbard ->
                    when {
                        erOppholdsadressePåSvalbard == true -> listOf(BOSATT_PÅ_SVALBARD)
                        erBosattIFinnmarkEllerNordTroms == true -> listOf(BOSATT_I_FINNMARK_NORD_TROMS)
                        else -> emptyList()
                    }
                }.beskjærFraOgMed(FINNMARK_OG_SVALBARD_MERKING_CUT_OFF_FOM_DATO)

        val eksisterendeBosattIRiketVilkårResultater = personResultat.vilkårResultater.filter { it.vilkårType == BOSATT_I_RIKET }

        return eksisterendeBosattIRiketVilkårResultater
            .tilTidslinje()
            .kombinerMed(finnmarkEllerSvalbardmerkingTidslinje) { eksisterendeVilkårResultat, finnmarkEllerSvalbardmerking ->
                if (eksisterendeVilkårResultat == null) {
                    return@kombinerMed null
                }

                val gjeldendeFinnmarkEllerSvalbardMarkeringer = finnmarkEllerSvalbardmerking.orEmpty().toSet()
                val eksisterendeFinnmarkEllerSvalbardMarkeringer = eksisterendeVilkårResultat.utdypendeVilkårsvurderinger.filter { it == BOSATT_I_FINNMARK_NORD_TROMS || it == BOSATT_PÅ_SVALBARD }.toSet()
                val utdypendeVilkårsvurderingMåOppdateres = eksisterendeFinnmarkEllerSvalbardMarkeringer != gjeldendeFinnmarkEllerSvalbardMarkeringer

                if (utdypendeVilkårsvurderingMåOppdateres) {
                    val oppdaterteUtdypendeVilkårsvurderinger =
                        eksisterendeVilkårResultat.utdypendeVilkårsvurderinger
                            .filter { it != BOSATT_I_FINNMARK_NORD_TROMS && it != BOSATT_PÅ_SVALBARD }
                            .plus(gjeldendeFinnmarkEllerSvalbardMarkeringer)

                    eksisterendeVilkårResultat.copy(utdypendeVilkårsvurderinger = oppdaterteUtdypendeVilkårsvurderinger, begrunnelse = PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                } else {
                    eksisterendeVilkårResultat
                }
            }.tilPerioderIkkeNull()
            .map {
                val periodeErEndret = it.fom != it.verdi.periodeFom || it.tom != it.verdi.periodeTom
                it.verdi.copy(
                    periodeFom = it.fom,
                    periodeTom = it.tom,
                    begrunnelse = if (periodeErEndret) PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT else it.verdi.begrunnelse,
                )
            }
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

    private fun lagErBostedsadresseIFinnmarkEllerNordTromsTidslinje(
        adresser: Adresser,
        personResultat: PersonResultat,
    ): Tidslinje<Boolean> {
        val filtrerteAdresser = filtrereUgyldigeAdresser(adresser.bostedsadresser)
        return lagTidslinjeForAdresser(filtrerteAdresser, personResultat, "Bostedadresse") { it.erIFinnmarkEllerNordTroms() }
    }

    private fun lagErDeltBostedIFinnmarkEllerNordTromsTidslinje(
        adresser: Adresser,
        personResultat: PersonResultat,
    ): Tidslinje<Boolean> {
        val filtrerteAdresser = filtrereUgyldigeAdresser(adresser.delteBosteder)
        val tidslinjer =
            filtrerteAdresser.map { adresse ->
                lagTidslinjeForAdresser(listOf(adresse), personResultat, "Delt bostedadresse") { it.erIFinnmarkEllerNordTroms() }
            }

        val deltBostedTidslinje =
            tidslinjer.fold(tomTidslinje<Boolean>()) { kombinertTidslinje, nesteTidslinje ->
                kombinertTidslinje.kombinerMed(nesteTidslinje) { kombinertVerdi, nesteVerdi ->
                    (kombinertVerdi == true) || (nesteVerdi == true)
                }
            }
        return deltBostedTidslinje
    }

    private fun lagErOppholdsadresserPåSvalbardTidslinje(
        adresser: Adresser,
        personResultat: PersonResultat,
    ): Tidslinje<Boolean> {
        val adresserPåSvalbard = adresser.oppholdsadresse.filter { it.erPåSvalbard() }

        if (adresserPåSvalbard.isEmpty()) {
            return tomTidslinje()
        }

        val filtrerteAdresser = filtrereUgyldigeOppholdsadresser(adresserPåSvalbard)

        return lagTidslinjeForAdresser(filtrerteAdresser, personResultat, "Oppholdsadresse") { it.erPåSvalbard() }
    }

    private fun lagTidslinjeForAdresser(
        adresser: List<Adresse>,
        personResultat: PersonResultat,
        adressetype: String,
        operator: (Adresse) -> Boolean,
    ): Tidslinje<Boolean> {
        try {
            return adresser
                .windowed(size = 2, step = 1, partialWindows = true) {
                    val denne = it.first()
                    val neste = it.getOrNull(1)

                    Periode(
                        verdi = operator(denne),
                        fom = denne.gyldigFraOgMed,
                        tom = denne.gyldigTilOgMed ?: neste?.gyldigFraOgMed?.minusDays(1),
                    )
                }.tilTidslinje()
        } catch (e: IllegalStateException) {
            secureLogger.error("Feil ved oppretting av tidslinjer for $adressetype med adresser $adresser for person med aktørId ${personResultat.aktør.aktørId}", e)
            throw e
        } catch (e: IllegalArgumentException) {
            secureLogger.error("Feil ved oppretting av tidslinjer for $adressetype med adresser $adresser for person med aktørId ${personResultat.aktør.aktørId}", e)
            throw e
        }
    }

    private fun filtrereUgyldigeAdresser(adresser: List<Adresse>): List<Adresse> {
        val filtrert =
            adresser
                .filterNot { it.erFomOgTomNull() || it.erFomOgTomSamme() || it.erFomEtterTom() }
                .groupBy { it.gyldigFraOgMed to it.gyldigTilOgMed }
                .values
                .map { likePerioder ->
                    likePerioder.find { it.erIFinnmarkEllerNordTroms() } ?: likePerioder.first()
                }.sortedBy { it.gyldigFraOgMed }

        return forskyvTilOgMedHvisDenErLikNesteFraOgMed(filtrert)
    }

    private fun filtrereUgyldigeOppholdsadresser(adresser: List<Adresse>): List<Adresse> {
        val filtrert =
            adresser
                .filterNot { it.erFomOgTomNull() || it.erFomOgTomSamme() || it.erFomEtterTom() }
                .groupBy { it.gyldigFraOgMed to it.gyldigTilOgMed }
                .values
                .map { likePerioder ->
                    likePerioder.find { it.erPåSvalbard() } ?: likePerioder.first()
                }.sortedBy { it.gyldigFraOgMed }

        return forskyvTilOgMedHvisDenErLikNesteFraOgMed(filtrert)
    }

    private fun forskyvTilOgMedHvisDenErLikNesteFraOgMed(adresser: List<Adresse>): List<Adresse> =
        adresser
            .windowed(size = 2, step = 1, partialWindows = true)
            .map { adresser ->
                val denne = adresser.first()
                val neste = adresser.getOrNull(1)

                if (denne.gyldigTilOgMed != null &&
                    neste != null &&
                    denne.gyldigTilOgMed == neste.gyldigFraOgMed
                ) {
                    denne.copy(gyldigTilOgMed = denne.gyldigTilOgMed.minusDays(1))
                } else {
                    denne
                }
            }
}

private fun validerKombinasjonerAvAdresserForFinnmarksOgSvalbardtileggbehandlinger(
    behandling: Behandling,
    erDeltBostedIFinnmarkEllerNordTromsTidslinje: Tidslinje<Boolean>,
    erOppholdsadressePåSvalbardTidslinje: Tidslinje<Boolean>,
    andelForAktør: List<AndelTilkjentYtelse>,
) {
    val harLøpendeAndelTidslinje =
        andelForAktør
            .filter { it.kalkulertUtbetalingsbeløp > 0 && it.type == YtelseType.ORDINÆR_BARNETRYGD }
            .map { Periode(true, it.stønadFom.toLocalDate(), it.stønadTom.toLocalDate()) }
            .tilTidslinje()

    val harDeltBostedIFinnmarkOgOppholdsadressePåSvalbardISammePeriode =
        erDeltBostedIFinnmarkEllerNordTromsTidslinje
            .kombinerMed(erOppholdsadressePåSvalbardTidslinje, harLøpendeAndelTidslinje) { erDeltBostedIFinnmarkEllerNordTroms, erOppholdsadressePåSvalbard, harLøpendeAndel ->
                erDeltBostedIFinnmarkEllerNordTroms == true && erOppholdsadressePåSvalbard == true && harLøpendeAndel == true
            }.tilPerioder()
            .any { it.verdi == true }

    if (harDeltBostedIFinnmarkOgOppholdsadressePåSvalbardISammePeriode) {
        throw AutovedtakMåBehandlesManueltFeil(beskrivelse = "${behandling.opprettetÅrsak.visningsnavn} kan ikke behandles automatisk som følge av adresseendring. Barn har delt bosted i Finnmark/Nord-Troms og oppholdsadresse på Svalbard")
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
