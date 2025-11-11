package bookrec.controller


import bookrec.model.Book
import bookrec.model.Recommendation
import bookrec.service.BookService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/books")
class BookController(
    private val bookService: BookService
) {
    private val logger = LoggerFactory.getLogger(BookController::class.java)

    // --- CREATE ---
    @PostMapping
    fun createBook(@RequestBody book: Book): ResponseEntity<Book> {
        logger.info("Create book")
        logger.info("Creating new book: {}", book)
        val createdBook = bookService.createBook(book)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBook)
    }

    // --- CREATE MULTIPLE (BATCH) ---
    @PostMapping("/batch")
    fun createBooksBatch(@RequestBody books: List<Book>): ResponseEntity<List<Book>> {
        logger.info("Create books")
        logger.info("Creating batch of {} books", books.size)
        val createdBooks = bookService.createBooksBatch(books)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBooks)
    }


    // --- READ ALL ---
    @GetMapping
    fun getAllBooks(): ResponseEntity<List<Book>> {
        logger.info("Get all books")
        logger.info("Fetching all books")
        val books = bookService.getAllBooks()
        return ResponseEntity.ok(books)
    }

    // --- READ ONE ---
    @GetMapping("/{id}")
    fun getBookById(@PathVariable id: Long): ResponseEntity<Book> {
        logger.info("Get book by id")
        logger.info("Fetching book with id: {}", id)
        val book = bookService.getBookById(id)
        return if (book != null) ResponseEntity.ok(book)
        else {
            logger.warn("Book not found with id: {}", id)
            ResponseEntity.notFound().build()
        }
    }

    // --- UPDATE ---
    @PutMapping("/{id}")
    fun updateBook(@PathVariable id: Long, @RequestBody updatedBook: Book): ResponseEntity<Book> {
        logger.info("Update book")
        logger.info("Updating book with id: {}", id)
        val book = bookService.updateBook(id, updatedBook)
        return if (book != null) ResponseEntity.ok(book)
        else {
            logger.warn("Cannot update, book not found with id: {}", id)
            ResponseEntity.notFound().build()
        }
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable id: Long): ResponseEntity<Void> {
        logger.info("Delete book")
        logger.info("Deleting book with id: {}", id)
        return if (bookService.deleteBook(id)) {
            logger.info("Book deleted successfully: {}", id)
            ResponseEntity.noContent().build()
        } else {
            logger.warn("Book not found for deletion: {}", id)
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/popular")
    fun getPopularBooks(): ResponseEntity<List<Book>> {
        logger.info("Get popular books")
        logger.info("Fetching popular book recommendations")
        val recommendations = bookService.getPopularBooks()
        return ResponseEntity.ok(recommendations)
    }

    @GetMapping("/title/{title}/{author}")
    fun getBooksByTitleAndAuthor(
        @PathVariable title: String,
        @PathVariable author: String
    ): ResponseEntity<List<Book>> {
        logger.info("Fetching books by title: {}", title)
        val books = bookService.getBooksByTitleAndAuthor(title, author)
        return if (books.isNotEmpty()) {
            ResponseEntity.ok(books)
        } else {
            logger.warn("No books found with title: {}", title)
            ResponseEntity.notFound().build()
        }
    }
}