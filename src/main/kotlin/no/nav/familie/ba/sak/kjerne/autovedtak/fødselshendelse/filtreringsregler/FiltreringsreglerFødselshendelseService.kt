package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.erOppfylt
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class FiltreringsreglerFødselshendelseService(
    private val personopplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val clockProvider: ClockProvider,
    private val filtreringResultatRepository: FiltreringResultatRepository,
    private val behandlingService: BehandlingService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val filtreringsregelEvaluator: FiltreringsregelEvaluator,
) {
    val filtreringsreglerMetrics = mutableMapOf<String, Counter>()
    val filtreringsreglerFørsteUtfallMetrics = mutableMapOf<String, Counter>()

    init {
        FILTRERINGSREGLER_FØDSELSHENDELSE.map { regel ->
            Resultat.entries.forEach { resultat ->
                filtreringsreglerMetrics["${regel.identifikator.name}_${resultat.name}"] =
                    Metrics.counter(
                        "familie.ba.sak.filtreringsregler.utfall",
                        "beskrivelse",
                        regel.identifikator.name,
                        "resultat",
                        resultat.name,
                    )

                filtreringsreglerFørsteUtfallMetrics[regel.identifikator.name] =
                    Metrics.counter(
                        "familie.ba.sak.filtreringsregler.foersteutfall",
                        "beskrivelse",
                        regel.identifikator.name,
                    )
            }
        }
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
                    evalueringsårsaker = it.evalueringÅrsaker.map { evalueringÅrsak -> evalueringÅrsak.toString() },
                    regelInput = fakta.convertDataClassToJson(),
                )
            },
        )

    fun hentFødselshendelsefiltreringResultater(behandlingId: Long): List<FiltreringResultat> = filtreringResultatRepository.finnFiltreringResultater(behandlingId = behandlingId)

    fun kjørFiltreringsregler(
        filtrerAutomatiskBehandlingData: FiltrerAutomatiskBehandlingData,
        behandling: Behandling,
    ): List<FiltreringResultat> {
        val morsAktørId = personidentService.hentAktør(filtrerAutomatiskBehandlingData.søkersIdent)
        val barnasAktørId = personidentService.hentAktørIder(filtrerAutomatiskBehandlingData.barnasIdenter)

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        val barnaFraHendelse = personopplysningGrunnlag.barna.filter { barnasAktørId.contains(it.aktør) }

        val migreringsdatoPåFagsak = behandlingService.hentMigreringsdatoPåFagsak(behandling.fagsak.id)

        val sisteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id)
        val andelerPåSisteBehandling =
            sisteBehandling?.let {
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = it.id)
            } ?: emptyList()
        val sisteMånedMedBarnetrygd = andelerPåSisteBehandling.maxOfOrNull { it.stønadTom }
        val harAndelerFremoverITid = sisteMånedMedBarnetrygd != null && sisteMånedMedBarnetrygd > YearMonth.now()

        val fakta =
            FiltreringsreglerFaktaFødselshendelse(
                søker = personopplysningGrunnlag.søker,
                søkerMottarLøpendeUtvidet = behandling.underkategori == BehandlingUnderkategori.UTVIDET,
                søkerOppfyllerVilkårForUtvidetBarnetrygd =
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato(
                        behandling,
                        barnaFraHendelse,
                    ),
                søkerMottarEøsBarnetrygd = behandling.kategori == BehandlingKategori.EØS,
                barnaSomSkalVurderes = barnaFraHendelse,
                restenAvBarna = finnRestenAvBarnasPersonInfo(morsAktørId, barnaFraHendelse),
                søkerLever = !personopplysningGrunnlag.søker.erDød(),
                barnaLever = barnaFraHendelse.none { it.erDød() },
                søkerHarVerge = personopplysningerService.harVerge(morsAktørId).harVerge,
                dagensDato = LocalDate.now(clockProvider.get()),
                erFagsakenMigrertEtterBarnFødt =
                    erSakenMigrertEtterBarnFødt(
                        barnaFraHendelse,
                        migreringsdatoPåFagsak,
                    ),
                løperBarnetrygdForBarnetPåAnnenForelder =
                    tilkjentYtelseValideringService.barnetrygdLøperForAnnenForelder(
                        behandling = behandling,
                        barna = barnaFraHendelse,
                    ),
                morHarIkkeOpphørtBarnetrygd = andelerPåSisteBehandling.isEmpty() || harAndelerFremoverITid,
            )
        val evalueringer = filtreringsregelEvaluator.evaluerFiltreringsregler(FILTRERINGSREGLER_FØDSELSHENDELSE, fakta)
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

    private fun erSakenMigrertEtterBarnFødt(
        barnaFraHendelse: List<Person>,
        migreringsdatoForFagsak: LocalDate?,
    ): Boolean = migreringsdatoForFagsak?.isAfter(barnaFraHendelse.minOf { it.fødselsdato }) == true

    private fun finnRestenAvBarnasPersonInfo(
        morsAktørId: Aktør,
        barnaFraHendelse: List<Person>,
    ): List<PersonInfo> =
        personopplysningerService
            .hentPersoninfoMedRelasjonerOgRegisterinformasjon(morsAktørId)
            .forelderBarnRelasjon
            .filter {
                it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN && barnaFraHendelse.none { barn -> barn.aktør == it.aktør }
            }.map {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(it.aktør)
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

    private fun oppdaterMetrikker(evalueringer: List<Evaluering>) {
        var førsteutfall = true
        evalueringer.forEach {
            filtreringsreglerMetrics["${it.identifikator}_${it.resultat.name}"]!!.increment()
            førsteutfall = økTellereForFørsteUtfall(it, førsteutfall)
        }
    }

    private fun morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato(
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

    companion object {
        val logger = LoggerFactory.getLogger(FiltreringsreglerFødselshendelseService::class.java)
    }
}
