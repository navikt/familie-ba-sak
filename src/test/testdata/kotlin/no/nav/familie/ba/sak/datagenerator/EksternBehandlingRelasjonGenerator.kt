package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.behandling.domene.NyEksternBehandlingRelasjon
import java.time.LocalDateTime
import java.util.UUID

fun lagEksternBehandlingRelasjon(
    id: Long = 0L,
    internBehandlingId: Long = 1000L,
    eksternBehandlingId: String = UUID.randomUUID().toString(),
    eksternBehandlingFagsystem: EksternBehandlingRelasjon.Fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
    opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
): EksternBehandlingRelasjon =
    EksternBehandlingRelasjon(
        id = id,
        internBehandlingId = internBehandlingId,
        eksternBehandlingId = eksternBehandlingId,
        eksternBehandlingFagsystem = eksternBehandlingFagsystem,
        opprettetTidspunkt = opprettetTidspunkt,
    )

fun lagNyEksternBehandlingRelasjon(
    eksternBehandlingId: String = UUID.randomUUID().toString(),
    eksternBehandlingFagsystem: EksternBehandlingRelasjon.Fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
): NyEksternBehandlingRelasjon =
    NyEksternBehandlingRelasjon(
        eksternBehandlingId = eksternBehandlingId,
        eksternBehandlingFagsystem = eksternBehandlingFagsystem,
    )
