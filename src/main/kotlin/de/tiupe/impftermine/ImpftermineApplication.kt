package de.tiupe.impftermine

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import java.io.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalQueries.localDate
import javax.sound.sampled.*


@Configuration
class ImpftermineConfiguration {
    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder.build()
    }
}

@SpringBootApplication
class ImpftermineApplication {

    @Bean
    fun init() = CommandLineRunner {
        var hasTermin = false
        playSound(once=true)

        // var birthdateString: String
        // var plz: Int
        println("Gib dein Geburtsdatum in der Form dd.mm.yyyy z.B. 02.05.1968 ein")
        val birthdateString = readLine()!!
        val birthdate = getBirthdateFromInput(birthdateString)
        println("Gib die Postleitzahl deines Wohnortes als fünfstellige Zahl, z.B. 38126 ein")
        val plz = readLine()!!.toInt()
        println("Wie groß soll die Pause bis zur nächsten Abfrage sein? Gib das Zeitintervall in Sekunden ein!")
        val intervall = readLine()!!.toInt()
        val intervallToSet: Long = intervall * 1_000L


        while(!hasTermin) {
            hasTermin = executeRestRequest(birthDate = birthdate, plz=plz)
            Thread.sleep(intervallToSet)
        }
        playSound()
    }

    @Autowired
    lateinit var restTemplate: RestTemplate

    fun getBirthdateFromInput(inputDate: String): LocalDate {
        val dateList = inputDate.split(".")
        return LocalDate.of(dateList[2].toInt(), dateList[1].toInt(), dateList[0].toInt())
    }
    fun executeRestRequest(count: Int = 1, birthDate: LocalDate, plz: Int): Boolean {
        // count=0 Biontec
        // count=1 Moderna
        // count=2 Biontec
        // count=3 Biontec
        // count=4 Biontec

        val instant: Instant = birthDate.atStartOfDay(ZoneId.of("UTC")/*ZoneId.of("Europe/Paris")*/).toInstant()
        val timeInMillis = instant.toEpochMilli()

        val restUrl =
            "https://www.impfportal-niedersachsen.de/portal/rest/appointments/findVaccinationCenterListFree/${plz}?stiko=&count=$count&birthdate=$timeInMillis"
        try {


            val answer = restTemplate.getForObject<ImpfAnswer>(restUrl)
            println(answer)
            val anzahlEintraege = answer.resultList.size
            println("Anzahl der Eiträge: ${answer.resultList.size}")
            if (anzahlEintraege > 0) {
                println("Nächster freier Impftag: ${answer.resultList[0].firstAppoinmentDateSorterAsDate}")
                return !(answer.resultList[0].outOfStock)
            } else {
                return false
            }
        } catch(ex: Exception) {
            println("Es ist ein Fehler aufgetreten, gucken was los ist....")
            return true
        }
    }

    private fun playSound(once: Boolean = false) {
        try {
            // get the sound file as a resource out of my jar file;
            // the sound file must be in the same directory as this class file.
            // the input stream portion of this recipe comes from a javaworld.com article.
            val res = ClassPathResource("grillePeter.wav")
            val inputStream: InputStream = res.inputStream
            // Der Stram benötigt die Möglichkeit, mark/reset  zu setzen, sonst geht der AudioStream auf
            // die Brette, daher wird noch einmal in einen BufferedInput-Stream gewandelt.
            val bufferedIn = BufferedInputStream(inputStream)
            val audioStream = AudioSystem.getAudioInputStream(bufferedIn)
            // the reference to the clip
            val clip = AudioSystem.getClip()
            clip.open(audioStream)
            if(once){
                clip.loop(1)
            } else {
                clip.loop(Clip.LOOP_CONTINUOUSLY)
            }
        } catch (e: Exception) {
            // a special way i'm handling logging in this application
            e.printStackTrace()
            println("Das Sound-File konnte nicht gefunden werden, es werden keine Signale gespielt")
        }
    }

}
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ImpfAnswer(val resultList: List<Impfzentrum>, val succeeded: Boolean)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Impfzentrum(
        val vaccinationCenterPk: Long,
        val name: String,
        val streetName: String,
        val streetNumber: String,
        val zipcode: Int,
        val city: String,
        val scheduleSaturday: Boolean,
        val scheduleSunday: Boolean,
        val vaccinationCenterType: Int,
        val vaccineName: String,
        val vaccineType: String,
        val interval1to2: Int,
        val offsetStart2Appointment: Int,
        val offsetEnd2Appointment: Int,
        val distance: Int,
        val outOfStock: Boolean,
        val firstAppoinmentDateSorterOnline: Long,
        val freeSlotSizeOnline: Int,
        val maxFreeSlotPerDay: Int,
        val publicAppointment: Boolean
        ) {
        val firstAppoinmentDateSorterAsDate: LocalDate
            get() {
                val inst = Instant.ofEpochMilli(firstAppoinmentDateSorterOnline)
                val ofInstant: LocalDate = LocalDate.ofInstant(inst, ZoneId.of("UTC"))
                return ofInstant
            }
    }

fun main(args: Array<String>) {
    runApplication<ImpftermineApplication>(*args)
}


