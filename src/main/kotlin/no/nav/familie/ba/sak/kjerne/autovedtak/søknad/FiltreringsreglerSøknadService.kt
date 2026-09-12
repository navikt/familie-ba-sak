package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.micrometer.core.instrument.Counter
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.erOppfylt
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FILTRERINGSREGLER_SØKNAD
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsregelEvaluator
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFakta
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFaktaSøknad
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFødselshendelseService.Companion.logger
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service
import kotlin.collections.forEach

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
) {
    val filtreringsreglerMetrics = mutableMapOf<String, Counter>()
    val filtreringsreglerFørsteUtfallMetrics = mutableMapOf<String, Counter>()

    fun kjørFiltreringsregler(
        filtrerAutomatiskBehandlingData: FiltrerAutomatiskBehandlingData,
        behandling: Behandling,
    ): List<FiltreringResultat> {
        val aktørMor = personidentService.hentAktør(filtrerAutomatiskBehandlingData.søkersIdent)
        val aktørBarna = personidentService.hentAktørIder(filtrerAutomatiskBehandlingData.barnasIdenter)

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        val barnaFraSøknad = personopplysningGrunnlag.barna.filter { aktørBarna.contains(it.aktør) }

        val fakta =
            FiltreringsreglerFaktaSøknad(
                søker = personopplysningGrunnlag.søker,
                søkerMottarLøpendeUtvidet = behandling.underkategori == BehandlingUnderkategori.UTVIDET,
                søkerOppfyllerVilkårForUtvidetBarnetrygd =
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato(
                        behandling,
                        barnaFraSøknad,
                    ),
                søkerMottarEøsBarnetrygd = behandling.kategori == BehandlingKategori.EØS,
                barnaSomSkalVurderes = barnaFraSøknad,
                søkerLever = !personopplysningGrunnlag.søker.erDød(),
                barnaLever = barnaFraSøknad.none { it.erDød() },
                søkerHarVerge = personopplysningerService.harVerge(aktørMor).harVerge,
                løperBarnetrygdForBarnetPåAnnenForelder =
                    tilkjentYtelseValideringService.barnetrygdLøperForAnnenForelder(
                        behandling = behandling,
                        barna = barnaFraSøknad,
                    ),
                søkerHarIkkeLøpendeUtbetalingOgHarAldriHattUtbetaling = false, // TODO Fix me
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
                    evalueringsårsaker = it.evalueringÅrsaker.map { evalueringÅrsak -> evalueringÅrsak.toString() },
                    regelInput = fakta.convertDataClassToJson(),
                )
            },
        )
}
