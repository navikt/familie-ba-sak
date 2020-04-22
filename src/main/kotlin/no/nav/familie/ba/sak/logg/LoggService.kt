package no.nav.familie.ba.sak.logg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service

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

    fun opprettRegistrertSøknadLogg(behandling: Behandling, søknadFinnesFraFør: Boolean) {
        val tittel = if (!søknadFinnesFraFør) "Søknaden ble registrert" else "Søknaden ble endret"
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.FØDSELSHENDELSE,
                tittel = tittel,
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                tekst = ""
        ))
    }

    fun opprettVilkårsvurderingLogg(behandling: Behandling,
                                    forrigeBehandlingResultat: BehandlingResultat?,
                                    nyttBehandlingResultat: BehandlingResultat): Logg {
        return if (forrigeBehandlingResultat != null) {
            lagre(Logg(
                    behandlingId = behandling.id,
                    type = LoggType.VILKÅRSVURDERING,
                    tittel = "Endring på vilkårsvurdering",
                    rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                    tekst = "Resultat gikk fra ${forrigeBehandlingResultat.hentSamletResultat().displayName.toLowerCase()} " +
                            "til ${nyttBehandlingResultat.hentSamletResultat().displayName.toLowerCase()}"
            ))
        } else {
            lagre(Logg(
                    behandlingId = behandling.id,
                    type = LoggType.VILKÅRSVURDERING,
                    tittel = "Opprettet vilkårsvurdering",
                    rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
                    tekst = "Resultat ble ${nyttBehandlingResultat.hentSamletResultat().displayName.toLowerCase()}"
            ))
        }
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

    fun opprettBeslutningOmVedtakLogg(behandling: Behandling, beslutning: Beslutning, beslutter: String, begrunnelse: String?) {
        lagre(Logg(
                behandlingId = behandling.id,
                type = LoggType.GODKJENNE_VEDTAK,
                tittel = if (beslutning.erGodkjent()) "Godkjent vedtak" else "Underkjent vedtak",
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.BESLUTTER),
                tekst = "Saksbehandler $beslutter satte vedtak til $beslutning" +
                        if (begrunnelse != null) " med begrunnelse: $begrunnelse" else ""
        ))
    }

    fun opprettDistribuertBrevLogg(behandlingId: Long, tekst: String) {
        lagre(Logg(
                behandlingId = behandlingId,
                type = LoggType.DISTRIBUERE_BREV,
                tittel = "Dokument er sendt",
                rolle = SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.SYSTEM),
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