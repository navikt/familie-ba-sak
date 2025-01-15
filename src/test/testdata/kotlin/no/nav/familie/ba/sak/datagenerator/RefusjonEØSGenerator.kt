package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøs
import java.time.LocalDate

fun lagRefusjonEøs(
    behandlingId: Long = 0L,
    fom: LocalDate = LocalDate.now().minusMonths(1),
    tom: LocalDate = LocalDate.now().plusMonths(1),
    refusjonsbeløp: Int = 0,
    land: String = "NO",
    refusjonAvklart: Boolean = true,
    id: Long = 0L,
): RefusjonEøs =
    RefusjonEøs(
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        refusjonsbeløp = refusjonsbeløp,
        land = land,
        refusjonAvklart = refusjonAvklart,
        id = id,
    )
