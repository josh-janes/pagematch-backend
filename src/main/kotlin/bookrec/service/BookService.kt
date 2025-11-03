package bookrec.service

import bookrec.model.Book
import bookrec.repository.BookRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BookService(
    private val bookRepository: BookRepository
) {
    private val logger = LoggerFactory.getLogger(BookService::class.java)

    // --- CREATE ---
    fun createBook(book: Book): Book {
        logger.info("Saving new book: {}", book.title)
        return bookRepository.save(book)
    }

    // --- READ ALL ---
    fun getAllBooks(): List<Book> {
        logger.info("Fetching all books")
        return bookRepository.findAll()
    }

    // --- READ ONE ---
    fun getBookById(id: Long): Book? {
        logger.info("Fetching book with id: {}", id)
        return bookRepository.findById(id).orElse(null)
    }

    // --- UPDATE ---
    fun updateBook(id: Long, updatedBook: Book): Book? {
        logger.info("Updating book with id: {}", id)
        val existingBook = bookRepository.findById(id)
        if (existingBook.isPresent) {
            val book = existingBook.get() //TODO
            //            .copy(
//                id = updatedBook.id,
//                title = updatedBook.title,
//                author = updatedBook.author,
//                genre = updatedBook.genre,
//                averageRating = updatedBook.score,
//                synopsis = updatedBook.description
//            )
            logger.debug("Book before update: {}", existingBook.get())
            logger.debug("Book after update: {}", book)
            return bookRepository.save(book)
        }
        logger.warn("Book not found with id: {}", id)
        return null
    }

    // --- DELETE ---
    fun deleteBook(id: Long): Boolean {
        logger.info("Deleting book with id: {}", id)
        return if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id)
            true
        } else {
            logger.warn("Book not found for deletion: {}", id)
            false
        }
    }

    fun getPopularBooks(): List<Book> {
        logger.info("Fetching popular books (rating > 4.5)")
        return bookRepository.findTopRated(4.5)
    }

    fun createBooksBatch(books: List<Book>): List<Book> {
        logger.info("Saving batch of {} books", books.size)
        return bookRepository.saveAll(books)
    }

    fun getBooksByTitleAndAuthor(title: String, author: String): List<Book> {
        logger.info("Fetching books with title containing: {}", title)
        return bookRepository.findByTitleAndAuthorContainingIgnoreCase(title, author)
    }
}
