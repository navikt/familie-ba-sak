package no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultat
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.domene.FødselshendelsefiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service


@Service
class FiltreringsreglerService(
        private val personopplysningerService: PersonopplysningerService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val localDateService: LocalDateService,
        private val fødselshendelsefiltreringResultatRepository: FødselshendelsefiltreringResultatRepository,
) {

    val filtreringsreglerMetrics = mutableMapOf<String, Counter>()
    val filtreringsreglerFørsteUtfallMetrics = mutableMapOf<String, Counter>()

    init {
        Filtreringsregel.values().map {
            Resultat.values().forEach { resultat ->
                filtreringsreglerMetrics["${it.name}_${resultat.name}"] =
                        Metrics.counter("familie.ba.sak.filtreringsregler.utfall",
                                        "beskrivelse",
                                        it.name,
                                        "resultat",
                                        resultat.name)

                filtreringsreglerFørsteUtfallMetrics[it.name] =
                        Metrics.counter("familie.ba.sak.filtreringsregler.foersteutfall",
                                        "beskrivelse",
                                        it.name)

            }
        }
    }

    fun lagreFiltreringsregler(evalueringer: List<Evaluering>,
                               behandlingId: Long,
                               fakta: Fakta): List<FødselshendelsefiltreringResultat> {
        return fødselshendelsefiltreringResultatRepository.saveAll(evalueringer.map {
            FødselshendelsefiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.valueOf(it.identifikator),
                    resultat = it.resultat,
                    begrunnelse = it.begrunnelse,
                    evalueringsårsaker = it.evalueringÅrsaker.map { evalueringÅrsak -> evalueringÅrsak.toString() },
                    regelInput = fakta.convertDataClassToJson()
            )
        })
    }

    fun kjørFiltreringsregler(nyBehandlingHendelse: NyBehandlingHendelse,
                              behandling: Behandling): List<Evaluering> {
        val morsIdent = nyBehandlingHendelse.morsIdent
        val barnasIdenter = nyBehandlingHendelse.barnasIdenter

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")
        val barnaFraHendelse = personopplysningGrunnlag.barna.filter { barnasIdenter.contains(it.personIdent.ident) }

        val fakta = Fakta(
                mor = personopplysningGrunnlag.søker,
                barnaFraHendelse = barnaFraHendelse,
                restenAvBarna = finnRestenAvBarnasPersonInfo(morsIdent, barnaFraHendelse),
                morLever = !personopplysningerService.hentDødsfall(Ident(morsIdent)).erDød,
                barnaLever = !barnasIdenter.any { personopplysningerService.hentDødsfall(Ident(it)).erDød },
                morHarVerge = personopplysningerService.harVerge(morsIdent).harVerge,
                dagensDato = localDateService.now()
        )
        val evalueringer = evaluerFiltreringsregler(fakta)

        oppdaterMetrikker(evalueringer)
        return evalueringer
    }

    private fun finnRestenAvBarnasPersonInfo(morsIndent: String, barnaFraHendelse: List<Person>): List<PersonInfo> {
        return personopplysningerService.hentPersoninfoMedRelasjoner(morsIndent).forelderBarnRelasjon.filter {
            it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN && barnaFraHendelse.none { barn -> barn.personIdent.ident == it.personIdent.id }
        }.map {
            personopplysningerService.hentPersoninfoMedRelasjoner(it.personIdent.id)
        }
    }

    private fun økTellereForFørsteUtfall(evaluering: Evaluering, førsteutfall: Boolean): Boolean {
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
}

