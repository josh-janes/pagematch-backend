package bookrec.service

import com.opencsv.CSVReader
import com.opencsv.exceptions.CsvValidationException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStreamReader
import java.util.Map
import java.util.stream.Collectors

@Service
class CsvProcessingService {
    @Throws(IOException::class, CsvValidationException::class)
    fun processCsv(file: MultipartFile): String {
        require(file.getSize() <= MAX_FILE_SIZE) { "File size exceeds the 10MB limit." }

        val authorFrequency: MutableMap<String?, Int?> = HashMap<String?, Int?>()
        val genreFrequency: MutableMap<String?, Int?> = HashMap<String?, Int?>()
        val highRatedBooks = StringBuilder()

        InputStreamReader(file.getInputStream()).use { reader ->
            CSVReader(reader).use { csvReader ->
                val headers = csvReader.readNext() // Read header
                val headerMap = getHeaderMap(headers)

                var line: Array<String?>?
                while ((csvReader.readNext().also { line = it }) != null) {
                    val author = line!![headerMap.get("Author")!!]
                    authorFrequency.put(author, authorFrequency.getOrDefault(author, 0)!! + 1)

                    val bookshelves = line[headerMap.get("Bookshelves")!!]
                    if (bookshelves != null && !bookshelves.isEmpty()) {
                        val genres: Array<String?> =
                            bookshelves.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        for (genre in genres) {
                            genreFrequency.put(genre, genreFrequency.getOrDefault(genre, 0)!! + 1)
                        }
                    }

                    try {
                        val myRating = line[headerMap.get("My Rating")!!]!!.toInt()
                        if (myRating >= 4) {
                            highRatedBooks.append(line[headerMap.get("Title")!!]).append(" by ").append(author)
                                .append("\n")
                        }
                    } catch (e: NumberFormatException) {
                        // Ignore books without a rating
                    }
                }
            }
        }
        return createSummary(authorFrequency, genreFrequency, highRatedBooks.toString())
    }

    private fun getHeaderMap(headers: Array<String?>): MutableMap<String?, Int?> {
        val headerMap: MutableMap<String?, Int?> = HashMap<String?, Int?>()
        for (i in headers.indices) {
            headerMap.put(headers[i], i)
        }
        return headerMap
    }

    private fun createSummary(
        authorFrequency: MutableMap<String?, Int?>,
        genreFrequency: MutableMap<String?, Int?>,
        highRatedBooks: String?
    ): String {
        val topAuthors = getTopItems(authorFrequency, 5)
        val topGenres = getTopItems(genreFrequency, 5)

        return String.format(
            "Here is a summary of a user's reading habits:\n\n" +
                    "Top Authors:\n%s\n\n" +
                    "Top Genres/Bookshelves:\n%s\n\n" +
                    "A selection of their highly-rated books (4 or 5 stars):\n%s",
            topAuthors, topGenres, highRatedBooks
        )
    }

    private fun getTopItems(frequencyMap: MutableMap<String?, Int?>, limit: Int): String {
        return frequencyMap.entries.stream()
            .sorted(Map.Entry.comparingByValue<String?, Int?>().reversed())
            .limit(limit.toLong())
            .map<String?> { it -> it.key }
            .collect(Collectors.joining("\n"))
    }

    companion object {
        private val MAX_FILE_SIZE = (10 * 1024 * 1024 // 10MB
                ).toLong()
    }
}