package no.nav.familie.ba.sak.datagenerator.simulering

import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.MottakerType

fun mockØkonomiSimuleringMottaker(
    id: Long = 0,
    mottakerNummer: String? = randomFnr(),
    mottakerType: MottakerType = MottakerType.BRUKER,
    behandling: Behandling = mockk(relaxed = true),
    økonomiSimuleringPostering: List<ØkonomiSimuleringPostering> = listOf(mockØkonomiSimuleringPostering()),
) = ØkonomiSimuleringMottaker(id, mottakerNummer, mottakerType, behandling, økonomiSimuleringPostering)
