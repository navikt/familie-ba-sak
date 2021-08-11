package no.nav.familie.ba.sak.kjerne.fødselshendelse


import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FagsystemUtfall.DAGLIG_KVOTE_OG_NORSK_STATSBORGER
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FagsystemUtfall.FAGSAK_UTEN_IVERKSATTE_BEHANDLINGER_I_BA_SAK
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FagsystemUtfall.IVERKSATTE_BEHANDLINGER_I_BA_SAK
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FagsystemUtfall.LØPENDE_SAK_I_INFOTRYGD
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FagsystemUtfall.SAKER_I_INFOTRYGD_MEN_IKKE_LØPENDE_UTBETALINGER
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FagsystemUtfall.STANDARDUTFALL_INFOTRYGD
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FagsystemUtfall.values
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.sisteStatsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class VelgFagSystemService(
        private val fagsakService: FagsakService,
        private val infotrygdService: InfotrygdService,
        private val behandlingService: BehandlingService,
        private val personopplysningerService: PersonopplysningerService,
        private val featureToggleService: FeatureToggleService,
        @Value("\${DAGLIG_KVOTE_FØDSELSHENDELSER}") val dagligKvote: Long = 0
) {

    val utfallForValgAvFagsystem = mutableMapOf<FagsystemUtfall, Counter>()
    val foreslåttUtfallForValgAvFagsystem = mutableMapOf<FagsystemUtfall, Counter>()

    init {
        values().forEach {
            utfallForValgAvFagsystem[it] = Metrics.counter("familie.ba.sak.velgfagsystem",
                                                           "navn",
                                                           it.name,
                                                           "beskrivelse",
                                                           it.beskrivelse)
            foreslåttUtfallForValgAvFagsystem[it] = Metrics.counter("familie.ba.sak.foreslaatt.velgfagsystem",
                                                                    "navn",
                                                                    it.name,
                                                                    "beskrivelse",
                                                                    it.beskrivelse)
        }
    }

    internal fun morHarLøpendeEllerTidligereUtbetalinger(fagsak: Fagsak?): Boolean {
        return if (fagsak == null) false
        else if (behandlingService.hentBehandlinger(fagsakId = fagsak.id).any { it.status == BehandlingStatus.UTREDES }) true
        else behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsak.id) != null
    }


    internal fun morHarSakerMenIkkeLøpendeIInfotrygd(morsIdent: String): Boolean {
        return infotrygdService.harÅpenSakIInfotrygd(mutableListOf(morsIdent)) && !infotrygdService.harLøpendeSakIInfotrygd(
                mutableListOf(morsIdent))
    }

    internal fun morEllerBarnHarLøpendeSakIInfotrygd(morsIdent: String, barnasIdenter: List<String>): Boolean {
        val morsIdenter = personopplysningerService.hentIdenter(Ident(morsIdent))
                .filter { it.gruppe == "FOLKEREGISTERIDENT" }
                .map { it.ident }
        val alleBarnasIdenter = barnasIdenter.flatMap {
            personopplysningerService.hentIdenter(Ident(it))
                    .filter { identinfo -> identinfo.gruppe == "FOLKEREGISTERIDENT" }
                    .map { identinfo -> identinfo.ident }
        }

        return infotrygdService.harLøpendeSakIInfotrygd(morsIdenter, alleBarnasIdenter)
    }

    internal fun erUnderDagligKvote(): Boolean = behandlingService.hentDagensFødselshendelser().size < dagligKvote

    internal fun harMorGyldigNorskstatsborger(morsIdent: Ident): Boolean {
        val statsborgerskap = personopplysningerService.hentStatsborgerskap(morsIdent)

        secureLogger.info("Siste statsborgerskap for $morsIdent=(${statsborgerskap.sisteStatsborgerskap()?.land}, bekreftelsesdato=${statsborgerskap.sisteStatsborgerskap()?.bekreftelsesdato}, gyldigFom=${statsborgerskap.sisteStatsborgerskap()?.gyldigFraOgMed}, gyldigTom=${statsborgerskap.sisteStatsborgerskap()?.gyldigTilOgMed})")
        return statsborgerskap.sisteStatsborgerskap()?.land == "NOR"
    }


    fun velgFagsystem(nyBehandlingHendelse: NyBehandlingHendelse): FagsystemRegelVurdering {
        val behandlingIBaSakErPåskrudd = featureToggleService.isEnabled(FeatureToggleConfig.AUTOMATISK_FØDSELSHENDELSE)

        val morsPersonIdent = PersonIdent(nyBehandlingHendelse.morsIdent)
        val fagsak = fagsakService.hent(morsPersonIdent)

        val (fagsystemUtfall: FagsystemUtfall, fagsystem: FagsystemRegelVurdering) = when {
            morHarLøpendeEllerTidligereUtbetalinger(fagsak) -> Pair(IVERKSATTE_BEHANDLINGER_I_BA_SAK,
                                                                    FagsystemRegelVurdering.SEND_TIL_BA)
            morEllerBarnHarLøpendeSakIInfotrygd(nyBehandlingHendelse.morsIdent, nyBehandlingHendelse.barnasIdenter) -> Pair(
                    LØPENDE_SAK_I_INFOTRYGD,
                    FagsystemRegelVurdering.SEND_TIL_INFOTRYGD)
            fagsak != null -> Pair(
                    FAGSAK_UTEN_IVERKSATTE_BEHANDLINGER_I_BA_SAK,
                    FagsystemRegelVurdering.SEND_TIL_BA)
            morHarSakerMenIkkeLøpendeIInfotrygd(nyBehandlingHendelse.morsIdent) -> Pair(
                    SAKER_I_INFOTRYGD_MEN_IKKE_LØPENDE_UTBETALINGER,
                    FagsystemRegelVurdering.SEND_TIL_INFOTRYGD)
            erUnderDagligKvote() && harMorGyldigNorskstatsborger(Ident(morsPersonIdent.ident)) -> Pair(
                    DAGLIG_KVOTE_OG_NORSK_STATSBORGER,
                    FagsystemRegelVurdering.SEND_TIL_BA)

            else -> Pair(STANDARDUTFALL_INFOTRYGD, FagsystemRegelVurdering.SEND_TIL_INFOTRYGD)
        }

        foreslåttUtfallForValgAvFagsystem[fagsystemUtfall]?.increment()

        return if (behandlingIBaSakErPåskrudd) {
            secureLogger.info("Sender fødselshendelse for ${nyBehandlingHendelse.morsIdent} til $fagsystem med utfall $fagsystemUtfall")
            utfallForValgAvFagsystem[fagsystemUtfall]?.increment()
            fagsystem
        } else {
            secureLogger.info("Sender fødselshendelse for ${nyBehandlingHendelse.morsIdent} til infotrygd, men foreslått fagsystem er $fagsystem med utfall $fagsystemUtfall")
            utfallForValgAvFagsystem[STANDARDUTFALL_INFOTRYGD]?.increment()
            FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
        }
    }

    companion object {

        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

enum class FagsystemRegelVurdering {
    SEND_TIL_BA,
    SEND_TIL_INFOTRYGD
}

enum class FagsystemUtfall(val beskrivelse: String) {
    IVERKSATTE_BEHANDLINGER_I_BA_SAK("Mor har fagsak med tidligere eller løpende utbetalinger i ba-sak"),
    LØPENDE_SAK_I_INFOTRYGD("Mor har løpende sak i infotrygd"),
    FAGSAK_UTEN_IVERKSATTE_BEHANDLINGER_I_BA_SAK("Mor har fagsak uten iverksatte behandlinger"),
    SAKER_I_INFOTRYGD_MEN_IKKE_LØPENDE_UTBETALINGER("Mor har saker i infotrygd, men ikke løpende utbetalinger"),
    DAGLIG_KVOTE_OG_NORSK_STATSBORGER("Daglig kvote er ikke nådd og mor har gyldig norsk statsborgerskap"),
    STANDARDUTFALL_INFOTRYGD("Ingen av de tidligere reglene slo til, sender til Infotrygd")
}

internal fun morHarLøpendeUtbetalingerIBA(fagsak: Fagsak?): Boolean {
    return fagsak?.status == FagsakStatus.LØPENDE
}

internal fun morHarSakerMenIkkeLøpendeUtbetalingerIBA(fagsak: Fagsak?): Boolean {
    return fagsak != null && fagsak.status != FagsakStatus.LØPENDE
}