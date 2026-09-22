package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.erStrengtFortrolig
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FILTRERINGSREGLER_SØKNAD
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsregelEvaluator
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsreglerFakta
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsreglerFaktaSøknad
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.erOppfylt
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFødselshendelseService.Companion.logger
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.iUkraina
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.tidslinje.utvidelser.verdiPåTidspunkt
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class FiltreringsreglerSøknadService(
    private val personopplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val filtreringResultatRepository: FiltreringResultatRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val filtreringsregelEvaluator: FiltreringsregelEvaluator,
    private val søknadService: SøknadService,
    private val clockProvider: ClockProvider,
) {
    val filtreringsreglerMetrics = mutableMapOf<String, Counter>()
    val filtreringsreglerFørsteUtfallMetrics = mutableMapOf<String, Counter>()

    init {
        FILTRERINGSREGLER_SØKNAD.forEach { regel ->
            Resultat.entries.forEach { resultat ->
                filtreringsreglerMetrics["${regel.identifikator.name}_${resultat.name}"] =
                    Metrics.counter(
                        "familie.ba.sak.filtreringsregler.soknad.utfall",
                        "beskrivelse",
                        regel.identifikator.name,
                        "resultat",
                        resultat.name,
                    )
            }

            filtreringsreglerFørsteUtfallMetrics[regel.identifikator.name] =
                Metrics.counter(
                    "familie.ba.sak.filtreringsregler.soknad.foersteutfall",
                    "beskrivelse",
                    regel.identifikator.name,
                )
        }
    }

    fun kjørFiltreringsregler(
        filtrerAutomatiskBehandlingData: FiltrerAutomatiskBehandlingData,
        behandling: Behandling,
    ): List<FiltreringResultat> {
        val aktørSøker = personidentService.hentAktør(filtrerAutomatiskBehandlingData.søkersIdent)

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        val søknad =
            søknadService.finnDigitalSøknad(behandling.id)
                ?: throw Feil("Fant ikke digital søknad for behandling ${behandling.id}")

        val aktørBarna = personidentService.hentAktørIder(søknad.barn.map { it.fnr })

        val barnaFraSøknad = personopplysningGrunnlag.barna.filter { aktørBarna.contains(it.aktør) }

        val personInfo =
            personopplysningerService
                .hentPersoninfoMedRelasjonerOgRegisterinformasjon(
                    aktør = aktørSøker,
                    relevanteAktører = aktørBarna.toSet(),
                )
        val forelderBarnRelasjonerForSøknadsbarna =
            personInfo.forelderBarnRelasjon.filter { it.aktør in aktørBarna }

        val fakta =
            FiltreringsreglerFaktaSøknad(
                søker = personopplysningGrunnlag.søker,
                søkerMottarLøpendeUtvidet = behandling.underkategori == BehandlingUnderkategori.UTVIDET,
                søkerOppfyllerVilkårForUtvidetBarnetrygd =
                    søkerOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato(
                        behandling,
                        barnaFraSøknad,
                    ),
                søkerMottarEøsBarnetrygd = behandling.kategori == BehandlingKategori.EØS,
                barnaSomSkalVurderes = barnaFraSøknad,
                søkerLever = !personopplysningGrunnlag.søker.erDød(),
                barnaLever = barnaFraSøknad.none { it.erDød() },
                søkerHarVerge = personopplysningerService.harVerge(aktørSøker).harVerge,
                utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned =
                    tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                        behandling = behandling,
                        barna = barnaFraSøknad,
                        måned = YearMonth.now(clockProvider.get()),
                    ),
                søkerHarKryssetPåEøsSpørsmålISøknaden = søknad.harKryssetPåEøsSpørsmål,
                søkerHarKryssetForDeltBostedISøknaden = søknad.harKryssetForDeltBostedForMinstEttBarn(),
                søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden = søknad.harKryssetForFosterhjemEllerBeredskapshjemForMinstEttBarn(),
                søknadenInneholderVedlegg = søknad.inneholderVedlegg,
                søkerHarIkkeLøpendeUtbetalingOgHarAldriHattUtbetaling = false, // TODO Fix me
                søkerHarAdressebeskyttelseGradering6Eller19 =
                    personInfo.adressebeskyttelseGradering.erStrengtFortrolig(),
                barnHarAdressebeskyttelseGradering6Eller19 =
                    forelderBarnRelasjonerForSøknadsbarna.any {
                        it.adressebeskyttelseGradering.erStrengtFortrolig()
                    },
                søkerOgBarnHarForelderBarnRelasjon =
                    barnaFraSøknad.all { barnFraSøknad ->
                        forelderBarnRelasjonerForSøknadsbarna.any {
                            it.aktør == barnFraSøknad.aktør
                        }
                    } && barnaFraSøknad.isNotEmpty(),
                søkerHarAktivNorskBostedsadresse = harAktivNorskBostedsadresse(listOf(personopplysningGrunnlag.søker), LocalDate.now(clockProvider.get())),
                barnHarAktivNorskBostedsadresse = harAktivNorskBostedsadresse(barnaFraSøknad, LocalDate.now(clockProvider.get())),
                søkerHarUkrainskStatsborgerskap = personopplysningGrunnlag.søker.statsborgerskap.iUkraina(),
                barnHarUkrainskStatsborgerskap =
                    barnaFraSøknad.any {
                        it.statsborgerskap.iUkraina()
                    },
            )

        val evalueringer = filtreringsregelEvaluator.evaluerFiltreringsregler(FILTRERINGSREGLER_SØKNAD, fakta)
        oppdaterMetrikker(evalueringer)

        logger.info("Resultater fra filtreringsregler på behandling $behandling: ${evalueringer.map { "${it.identifikator}: ${it.resultat}" }}")
        if (!evalueringer.erOppfylt()) {
            secureLogger.info("Resultater fra filtreringsregler på behandling $behandling: (Fakta: ${fakta.convertDataClassToJson()}): ${evalueringer.map { "${it.identifikator}: ${it.resultat}" }}")
        }

        return lagreFiltreringsregler(
            evalueringer = evalueringer,
            behandlingId = behandling.id,
            fakta = fakta,
        )
    }

    private fun søkerOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato(
        behandling: Behandling,
        barnaFraHendelse: List<Person>,
    ): Boolean {
        val forrigeVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
        return forrigeVedtatteBehandling?.let { vedtattBehandling ->
            vilkårsvurderingRepository.findByBehandlingAndAktiv(vedtattBehandling.id)?.let { vilkårsvurdering ->
                vilkårsvurdering.personResultater.single { personResultat -> personResultat.erSøkersResultater() }.vilkårResultater.any { vilkårResultat ->
                    vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD &&
                        vilkårResultat.erOppfylt() &&
                        barnaFraHendelse.any { barnFraHendelse ->
                            vilkårResultat.periodeTom?.isAfter(barnFraHendelse.fødselsdato) ?: true &&
                                vilkårResultat.periodeFom!!.isBefore(barnFraHendelse.fødselsdato.plusYears(18))
                        }
                }
            } ?: false
        } ?: false
    }

    private fun harAktivNorskBostedsadresse(
        personer: List<Person>,
        tidspunkt: LocalDate,
    ): Boolean =
        personer.all { person ->
            val tidslinje = Adresser.opprettFra(person = person).lagErBosattINorgeTidslinje()
            tidslinje.verdiPåTidspunkt(tidspunkt) == true
        } && personer.isNotEmpty()

    private fun oppdaterMetrikker(evalueringer: List<Evaluering>) {
        var førsteutfall = true
        evalueringer.forEach {
            filtreringsreglerMetrics["${it.identifikator}_${it.resultat.name}"]!!.increment()
            førsteutfall = økTellereForFørsteUtfall(it, førsteutfall)
        }
    }

    private fun økTellereForFørsteUtfall(
        evaluering: Evaluering,
        førsteutfall: Boolean,
    ): Boolean {
        if (evaluering.resultat == Resultat.IKKE_OPPFYLT && førsteutfall) {
            filtreringsreglerFørsteUtfallMetrics[evaluering.identifikator]!!.increment()
            return false
        }
        return førsteutfall
    }

    fun lagreFiltreringsregler(
        evalueringer: List<Evaluering>,
        behandlingId: Long,
        fakta: FiltreringsreglerFakta,
    ): List<FiltreringResultat> =
        filtreringResultatRepository.saveAll(
            evalueringer.map {
                FiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.Identifikator.valueOf(it.identifikator),
                    resultat = it.resultat,
                    begrunnelse = it.begrunnelse,
                    evalueringsårsaker = it.evalueringÅrsaker.map { evalueringÅrsak -> evalueringÅrsak.hentNavn() },
                    regelInput = fakta.convertDataClassToJson(),
                )
            },
        )
}
