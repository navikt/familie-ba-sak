package no.nav.familie.ba.sak.kjerne.logg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class LoggService(
        private val loggRepository: LoggRepository,
        private val rolleConfig: RolleConfig
) {

    private val metrikkPerLoggType: Map<LoggType, Counter> = LoggType.values().map {
        it to Metrics.counter("behandling.logg",
                              "type",
                              it.name,
                              "beskrivelse",
                              it.visningsnavn)
    }.toMap()

    fun opprettBehandlendeEnhetEndret(behandling: Behandling,
                                      fraEnhet: Arbeidsfordelingsenhet,
                                      tilEnhet: ArbeidsfordelingPåBehandling,
                                      manuellOppdatering: Boolean,
                                      begrunnelse: String) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLENDE_ENHET_ENDRET,
                tittel = "Endret enhet på behandling",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = "Behandlende enhet ${if (manuellOppdatering) "manuelt" else "automatisk"} endret fra " +
                        "${fraEnhet.enhetId} ${fraEnhet.enhetNavn} til ${tilEnhet.behandlendeEnhetId} ${tilEnhet.behandlendeEnhetNavn}." +
                        if (begrunnelse.isNotBlank()) "\n\n${begrunnelse}" else ""
        ))
    }

    fun opprettMottattDokument(behandling: Behandling, tekst: String, mottattDato: LocalDateTime) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.DOKUMENT_MOTTATT,
                tittel = "Dokument mottatt ${mottattDato.toLocalDate().tilKortString()}",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = tekst
        ))
    }


    fun opprettRegistrertSøknadLogg(behandling: Behandling, søknadFinnesFraFør: Boolean) {
        val tittel = if (!søknadFinnesFraFør) "Søknaden ble registrert" else "Søknaden ble endret"
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.SØKNAD_REGISTRERT,
                tittel = tittel,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = ""
        ))
    }

    fun opprettVilkårsvurderingLogg(behandling: Behandling,
                                    forrigeBehandlingResultat: BehandlingResultat,
                                    nyttBehandlingResultat: BehandlingResultat): Logg? {

        val tekst = when {
            forrigeBehandlingResultat == BehandlingResultat.IKKE_VURDERT -> {
                "Resultat ble ${nyttBehandlingResultat.displayName.lowercase()}"
            }
            forrigeBehandlingResultat != nyttBehandlingResultat -> {
                "Resultat gikk fra ${forrigeBehandlingResultat.displayName.lowercase()} til ${nyttBehandlingResultat.displayName.lowercase()}"
            }
            else -> return null
        }

        return lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.VILKÅRSVURDERING,
                tittel = if (forrigeBehandlingResultat != BehandlingResultat.IKKE_VURDERT) "Vilkårsvurdering endret" else "Vilkårsvurdering gjennomført",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = tekst
        ))
    }

    fun opprettFødselshendelseLogg(behandling: Behandling) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.FØDSELSHENDELSE,
                tittel = "Mottok fødselshendelse",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = ""
        ))
    }

    fun opprettBehandlingLogg(behandling: Behandling) {
        if (behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            opprettFødselshendelseLogg(behandling)
        }

        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "${behandling.type.visningsnavn} opprettet",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = ""
        ))
    }

    fun opprettSendTilBeslutterLogg(behandling: Behandling) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.SEND_TIL_BESLUTTER,
                tittel = "Sendt til beslutter",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = ""
        ))
    }

    fun opprettBeslutningOmVedtakLogg(behandling: Behandling, beslutning: Beslutning, begrunnelse: String? = null) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.GODKJENNE_VEDTAK,
                tittel = if (beslutning.erGodkjent()) "Vedtak godkjent " else "Vedtak underkjent",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.BESLUTTER),
                tekst = if (!beslutning.erGodkjent()) "Begrunnelse: $begrunnelse" else ""
        ))
    }

    fun opprettDistribuertBrevLogg(behandlingId: Long, tekst: String, rolle: BehandlerRolle) {
        lagre(Logg(
                behandlingId = behandlingId,
                type = LoggType.DISTRIBUERE_BREV,
                tittel = "Brev sendt",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, rolle),
                tekst = tekst
        ))
    }

    fun opprettFerdigstillBehandling(behandling: Behandling) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.FERDIGSTILLE_BEHANDLING,
                tittel = "Ferdigstilt behandling",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SYSTEM),
                tekst = ""
        ))
    }

    fun opprettHenleggBehandling(behandling: Behandling, årsak: String, begrunnelse: String) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.HENLEGG_BEHANDLING,
                tittel = "Behandlingen er henlagt",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = "$årsak: $begrunnelse"
        ))
    }

    fun opprettBarnLagtTilLogg(behandling: Behandling, barn: Person) {
        val beskrivelse = "${barn.navn.uppercase()} (${barn.hentAlder()} år) | ${barn.personIdent.ident} lagt til"
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.BARN_LAGT_TIL,
                tittel = LoggType.BARN_LAGT_TIL.visningsnavn,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = beskrivelse
        ))
    }

    fun lagre(logg: Logg): Logg {
        metrikkPerLoggType[logg.type]?.increment()

        return loggRepository.save(logg)
    }

    fun hentLoggForBehandling(behandlingId: Long): List<Logg> {
        return loggRepository.hentLoggForBehandling(behandlingId)
    }
}


