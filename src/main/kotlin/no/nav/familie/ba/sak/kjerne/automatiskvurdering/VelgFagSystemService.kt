package no.nav.familie.ba.sak.kjerne.automatiskvurdering


import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FagsystemUtfall.DAGLIG_KVOTE_OG_NORSK_STATSBORGER
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FagsystemUtfall.IVERKSATTE_BEHANDLINGER_I_BA_SAK
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FagsystemUtfall.LØPENDE_SAK_I_INFOTRYGD
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FagsystemUtfall.SAKER_I_INFOTRYGD_MEN_IKKE_LØPENDE_UTBETALINGER
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FagsystemUtfall.STANDARDUTFALL
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VelgFagsystemService(
        private val fagsakService: FagsakService,
        private val infotrygdService: InfotrygdService,
        private val behandlingService: BehandlingService,
        private val personopplysningerService: PersonopplysningerService,
        private val envService: EnvService,
) {

    val utfallForValgAvFagsystem = mutableMapOf<FagsystemUtfall, Counter>()

    init {
        FagsystemUtfall.values().forEach {
            utfallForValgAvFagsystem[it] = Metrics.counter("familie.ba.sak.velgfagsystem",
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

    internal fun morHarLøpendeSakIInfotrygd(morsIdent: String): Boolean {
        return infotrygdService.harLøpendeSakIInfotrygd(mutableListOf(morsIdent))
    }

    internal fun erDagensFørsteFødselshendelse(): Boolean = if (envService.erProd()) {
        behandlingService.hentDagensFødselshendelser().isEmpty()
    } else behandlingService.hentDagensFødselshendelser().size <= 1000

    internal fun harMorGyldigNorskstatsborger(morsIdent: Ident): Boolean {
        return personopplysningerService.hentStatsborgerskap(morsIdent).any {
            it.land == "NOR" && it.gyldigFraOgMed?.isBefore(LocalDate.now()) == true && (it.gyldigTilOgMed
                                                                                         ?: LocalDate.MAX).isAfter(
                    LocalDate.now())
        }

    }


    fun velgFagsystem(nyBehandlingHendelse: NyBehandlingHendelse): FagsystemRegelVurdering {
        val morsPersonIdent = PersonIdent(nyBehandlingHendelse.morsIdent)
        val fagsak = fagsakService.hent(morsPersonIdent)

        val (fagsystemUtfall: FagsystemUtfall, fagsystem: FagsystemRegelVurdering) = when {
            morHarLøpendeEllerTidligereUtbetalinger(fagsak) -> Pair(IVERKSATTE_BEHANDLINGER_I_BA_SAK,
                                                                    FagsystemRegelVurdering.SEND_TIL_BA)
            morHarLøpendeSakIInfotrygd(nyBehandlingHendelse.morsIdent) -> Pair(LØPENDE_SAK_I_INFOTRYGD,
                                                                               FagsystemRegelVurdering.SEND_TIL_INFOTRYGD)
            morHarSakerMenIkkeLøpendeIInfotrygd(nyBehandlingHendelse.morsIdent) -> Pair(
                    SAKER_I_INFOTRYGD_MEN_IKKE_LØPENDE_UTBETALINGER,
                    FagsystemRegelVurdering.SEND_TIL_INFOTRYGD)
            erDagensFørsteFødselshendelse() && harMorGyldigNorskstatsborger(Ident(morsPersonIdent.ident)) -> Pair(
                    DAGLIG_KVOTE_OG_NORSK_STATSBORGER,
                    FagsystemRegelVurdering.SEND_TIL_BA)

            else -> Pair(STANDARDUTFALL, FagsystemRegelVurdering.SEND_TIL_INFOTRYGD)
        }

        secureLogger.info("Sender fødselshendelse for ${nyBehandlingHendelse.morsIdent} til $fagsystem med utfall $fagsystemUtfall")
        utfallForValgAvFagsystem[fagsystemUtfall]?.increment()

        return fagsystem
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
    SAKER_I_INFOTRYGD_MEN_IKKE_LØPENDE_UTBETALINGER("Mor har saker i infotrygd, men ikke løpende utbetalinger"),
    DAGLIG_KVOTE_OG_NORSK_STATSBORGER("Daglig kvote er ikke nådd og mor har gyldig norsk statsborgerskap"),
    STANDARDUTFALL("Ingen av de tidligere reglene slo til, sender til Infotrygd")
}

internal fun morHarLøpendeUtbetalingerIBA(fagsak: Fagsak?): Boolean {
    return fagsak?.status == FagsakStatus.LØPENDE
}

internal fun morHarSakerMenIkkeLøpendeUtbetalingerIBA(fagsak: Fagsak?): Boolean {
    return fagsak != null && fagsak.status != FagsakStatus.LØPENDE
}