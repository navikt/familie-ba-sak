package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.statistikk.saksstatistikk.RelatertBehandling
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.KlageinstansResultatDto
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun lagKlagebehandlingDto(
    id: UUID = UUID.randomUUID(),
    fagsakId: UUID = UUID.randomUUID(),
    status: no.nav.familie.kontrakter.felles.klage.BehandlingStatus = no.nav.familie.kontrakter.felles.klage.BehandlingStatus.FERDIGSTILT,
    opprettet: LocalDateTime = LocalDateTime.now(),
    mottattDato: LocalDate = LocalDate.now(),
    resultat: BehandlingResultat? = BehandlingResultat.MEDHOLD,
    årsak: no.nav.familie.kontrakter.felles.klage.Årsak? = null,
    vedtaksdato: LocalDateTime? = LocalDateTime.now(),
    klageinstansResultat: List<KlageinstansResultatDto> = emptyList(),
    henlagtÅrsak: HenlagtÅrsak? = null,
) = KlagebehandlingDto(
    id = id,
    fagsakId = fagsakId,
    status = status,
    opprettet = opprettet,
    mottattDato = mottattDato,
    resultat = resultat,
    årsak = årsak,
    vedtaksdato = vedtaksdato,
    klageinstansResultat = klageinstansResultat,
    henlagtÅrsak = henlagtÅrsak,
)

fun lagKlageinstansResultatDto(
    type: BehandlingEventType = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
    utfall: KlageinstansUtfall? = KlageinstansUtfall.MEDHOLD,
    mottattEllerAvsluttetTidspunkt: LocalDateTime = LocalDateTime.now(),
    journalpostReferanser: List<String> = emptyList(),
    årsakFeilregistrert: String? = null,
): KlageinstansResultatDto =
    KlageinstansResultatDto(
        type = type,
        utfall = utfall,
        mottattEllerAvsluttetTidspunkt = mottattEllerAvsluttetTidspunkt,
        journalpostReferanser = journalpostReferanser,
        årsakFeilregistrert = årsakFeilregistrert,
    )

fun lagRelatertBehandling(
    id: String = "1",
    vedtattTidspunkt: LocalDateTime = LocalDateTime.now(),
    fagsystem: RelatertBehandling.Fagsystem = RelatertBehandling.Fagsystem.BA,
): RelatertBehandling =
    RelatertBehandling(
        id = id,
        vedtattTidspunkt = vedtattTidspunkt,
        fagsystem = fagsystem,
    )
