package no.nav.familie.ba.sak.task.dto

import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse

data class SimuleringTaskDTO(val nyBehandling: NyBehandlingHendelse,
                             val skalBehandlesHosInfotrygd: Boolean)