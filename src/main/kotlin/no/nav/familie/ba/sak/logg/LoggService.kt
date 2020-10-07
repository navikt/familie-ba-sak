package no.nav.familie.ba.sak.logg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.journalføring.domene.DokumentType
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
                                      fraEnhetId: String,
                                      tilEnhetId: String,
                                      manuellOppdatering: Boolean,
                                      begrunnelse: String) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLENDE_ENHET_ENDRET,
                tittel = "Endret enhet på behandling",
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = "Behandlende enhet ${if (manuellOppdatering) "manuelt" else "automatisk"} endret fra $fraEnhetId til $tilEnhetId." +
                        if (begrunnelse.isNotBlank()) "\n\n${begrunnelse}" else ""
        ))
    }

    fun opprettMottattDokument(behandling: Behandling, datoMottatt: LocalDateTime, dokumentType: DokumentType) {
        lagre(Logg(
                opprettetTidspunkt = datoMottatt,
                behandlingId = behandling.id,
                type = LoggType.DOKUMENT_MOTTATT,
                tittel = "${dokumentType.navn} ble mottatt",
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = ""
        ))
    }


    fun opprettRegistrertSøknadLogg(behandling: Behandling, søknadFinnesFraFør: Boolean) {
        val tittel = if (!søknadFinnesFraFør) "Søknaden ble registrert" else "Søknaden ble endret"
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.SØKNAD_REGISTRERT,
                tittel = tittel,
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = ""
        ))
    }

    fun opprettVilkårsvurderingLogg(behandling: Behandling,
                                    aktivBehandlingResultat: BehandlingResultat,
                                    nyttBehandlingResultatType: BehandlingResultatType,
                                    forrigeBehandlingResultat: BehandlingResultat?): Logg {
        val forrigeBehandlingResultatType =
                if (aktivBehandlingResultat.samletResultat != BehandlingResultatType.IKKE_VURDERT) aktivBehandlingResultat.samletResultat else forrigeBehandlingResultat?.samletResultat

        val tekst = if (forrigeBehandlingResultatType != null) {
            if (forrigeBehandlingResultatType != nyttBehandlingResultatType) {
                "Resultat gikk fra ${forrigeBehandlingResultatType.displayName.toLowerCase()} til ${nyttBehandlingResultatType.displayName.toLowerCase()}"
            } else {
                "Resultat fortsatt ${nyttBehandlingResultatType.displayName.toLowerCase()}"
            }
        } else "Resultat ble ${nyttBehandlingResultatType.displayName.toLowerCase()}"

        return lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.VILKÅRSVURDERING,
                tittel = if (forrigeBehandlingResultatType != null) "Vilkårsvurdering endret" else "Vilkårsvurdering gjennomført",
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = tekst
        ))
    }

    fun opprettFødselshendelseLogg(behandling: Behandling) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.FØDSELSHENDELSE,
                tittel = "Mottok fødselshendelse",
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = ""
        ))
    }

    fun opprettBehandlingLogg(behandling: Behandling) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "${behandling.type.visningsnavn} opprettet",
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = ""
        ))
    }

    fun opprettSendTilBeslutterLogg(behandling: Behandling) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.SEND_TIL_BESLUTTER,
                tittel = "Sendt til beslutter",
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = ""
        ))
    }

    fun opprettBeslutningOmVedtakLogg(behandling: Behandling, beslutning: Beslutning, begrunnelse: String?) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.GODKJENNE_VEDTAK,
                tittel = if (beslutning.erGodkjent()) "Godkjent vedtak" else "Underkjent vedtak",
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.BESLUTTER),
                tekst = if (beslutning.erGodkjent()) "Vedtak godkjent"
                else "Vedtak underkjent" + if (begrunnelse != null) " med begrunnelse: $begrunnelse" else ""
        ))
    }

    fun opprettDistribuertBrevLogg(behandlingId: Long, tekst: String, rolle: BehandlerRolle) {
        lagre(Logg(
                behandlingId = behandlingId,
                type = LoggType.DISTRIBUERE_BREV,
                tittel = "Dokument er sendt",
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, rolle),
                tekst = tekst
        ))
    }

    fun opprettFerdigstillBehandling(behandling: Behandling) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.FERDIGSTILLE_BEHANDLING,
                tittel = "Ferdigstilt behandling",
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SYSTEM),
                tekst = ""
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


