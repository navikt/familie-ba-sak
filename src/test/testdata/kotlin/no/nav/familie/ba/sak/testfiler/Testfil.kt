package no.nav.familie.ba.sak.testfiler

object Testfil {
    val TEST_PDF = Testfil::class.java.getResource("/dokument/mockvedtak.pdf")!!.readBytes()
}
