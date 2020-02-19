package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DiskresjonskodeServiceTest {
    val personUtenDiskresjonskode = Personinfo(LocalDate.now(), null, null)
    val personKode7 = Personinfo(LocalDate.now(), null, Diskresjonskode.KODE7.kode)
    val personKode6 = Personinfo(LocalDate.now(), null, Diskresjonskode.KODE6.kode)
    val personMedAnnenDiskresjonskode = Personinfo(LocalDate.now(), null, "FOO")

    @Test
    fun `ingen er kode 6 eller kode 7 - skal gi null`() {
        assertEquals(null, DiskresjonskodeService.finnStrengesteDiskresjonskode(listOf(
                personUtenDiskresjonskode,
                personUtenDiskresjonskode)
        ))
    }

    @Test
    fun `en er kode 6, en er kode 7, en har ingen diskresjonskode - skal gi kode 6`() {
        assertEquals(Diskresjonskode.KODE6.kode, DiskresjonskodeService.finnStrengesteDiskresjonskode(listOf(
                personKode7,
                personKode6,
                personUtenDiskresjonskode)
        ))
    }

    @Test
    fun `en har en annen diskresjonskode som vi ikke håndterer - skal gi null`() {
        assertEquals(null, DiskresjonskodeService.finnStrengesteDiskresjonskode(listOf(personMedAnnenDiskresjonskode)))
    }

    @Test
    fun `en har kode 7, en har ingen diskresjonskode, en har en ukjent diskresjonskode - skal gi kode 7`() {
        assertEquals(Diskresjonskode.KODE7.kode, DiskresjonskodeService.finnStrengesteDiskresjonskode(listOf(
                personUtenDiskresjonskode,
                personKode7,
                personMedAnnenDiskresjonskode
        )))
    }

    @Test
    fun `tom liste - skal gi null`() {
        assertEquals(null, DiskresjonskodeService.finnStrengesteDiskresjonskode(listOf<Personinfo>()))
    }
}