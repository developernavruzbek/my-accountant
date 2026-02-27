package org.example.myaccountant

import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.example.myaccountant.security.JwtService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.collections.map
import kotlin.let
import kotlin.run



interface UserService{
    fun create(body:UserCreateRequest)
    fun loginIn(request: LoginRequest) : JwtResponse

    fun getAll(): List<UserResponse>
    fun getById(id: Long): UserResponse
    fun update(id: Long, body: UserUpdateRequest): UserResponse
    fun delete(id: Long)
}

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val mapper: UserMapper,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jwtService: JwtService,
): UserService {
    @Transactional
    override fun create(body: UserCreateRequest) {

        body.run {
            userRepository.findByPhone(phone)?.let {
                throw PhoneNumberAlreadyExistsException()
            }?: run {

                 val savedUser = userRepository.save(mapper.toEntity(body))
                println("Sawed user => $savedUser")

            }
        }

    }

    override fun loginIn(request: LoginRequest): JwtResponse {
        val user =  userRepository.findByPhone(request.phone)
            ?: throw  UserNotFoundException()

        if(!passwordEncoder.matches(request.password, user.password)){
            throw PasswordIsIncorrect()
        }
        val token  = jwtService.generateToken(user.phone, user.role.name)
        return JwtResponse(token)
    }

    override fun getAll(): List<UserResponse> {
        return userRepository.findAllNotDeleted()
            .map { mapper.toDto(it) }
    }

    override fun getById(id: Long): UserResponse {
        val user = userRepository.findByIdAndDeletedFalse(id)
            ?: throw UserNotFoundException()

        return mapper.toDto(user)
    }


    @Transactional
    override fun update(id: Long, body: UserUpdateRequest): UserResponse {

        val user = userRepository.findByIdAndDeletedFalse(id)
            ?: throw UserNotFoundException()

        body.phone?.let { newPhone ->
            val exist = userRepository.findByPhone(newPhone)
            if (exist != null && exist.id != id) {
                throw PhoneNumberAlreadyExistsException()
            }
            if(newPhone.length>0)
                 user.phone = newPhone
        }

        body.fullName?.let {
            if (it.length>0)
            user.fullName = it }

        body.password?.let {
            if (it.length>0)
            user.password = passwordEncoder.encode(it)
        }

        body.role?.let {
            user.role = it }

        body.age?.let {
            user.age = it
        }

        val updated = userRepository.save(user)
        return mapper.toDto(updated)
    }


    override fun delete(id: Long) {
           userRepository.trash(id)
            ?: throw UserNotFoundException()
    }

}


interface CategoryService{
    fun create(categoryCreateRequest: CategoryCreateRequest)
    fun getOne(id: Long): CategoryResponse
    fun getAll(): List<CategoryResponse>
    fun update(id:Long, categoryUpdateRequest: CategoryUpdateRequest)
    fun delete(id: Long)
}


@Service
class CategoryServiceImpl(
    private val categoryRepository: CategoryRepository,
    private val categoryMapper: CategoryMapper
): CategoryService {

    @Transactional
    override fun create(categoryCreateRequest: CategoryCreateRequest) {
        categoryRepository.findByName(categoryCreateRequest.name)?.let {
            throw CategoryAlreadyExistsException()
        }
        categoryRepository.save(categoryMapper.toEntity(categoryCreateRequest))

    }

    override fun getOne(id: Long): CategoryResponse {
        val category = categoryRepository.findByIdAndDeletedFalse(id)
            ?:throw CategoryNotFoundException()
        return categoryMapper.toDto(category)
    }

    override fun getAll(): List<CategoryResponse> {
        return categoryRepository.findAll().map { category->
            categoryMapper.toDto(category)
        }
    }

    override fun update(id: Long, categoryUpdateRequest: CategoryUpdateRequest) {
        categoryRepository.findByIdAndDeletedFalse(id)?.let { category ->
            categoryUpdateRequest.name?.let {
                categoryRepository.findByName(it)?.let {
                    if (it.id!=category.id)
                        throw CategoryNameAlreadyExistsException()
                }
                category.name = it
            }
            categoryUpdateRequest.description?.let {
                category.description =it
            }

         categoryRepository.save(category)
        }?:throw CategoryNotFoundException()
    }

    override fun delete(id: Long) {
       categoryRepository.findByIdAndDeletedFalse(id)?.let {
           categoryRepository.trash(id)
       }?:throw CategoryNotFoundException()
    }
}

interface ExpensesService{
    fun create(expensesCreateRequest: ExpensesCreateRequest)
    fun getOne(id:Long): ExpensesResponse
    fun getAll(): List<ExpensesResponse>
    fun update(id:Long, expensesUpdateRequest: ExpensesUpdateRequest)
    fun delete(id:Long)
    fun findAllUser(): List<ExpensesResponse>
}

@Service
class ExpensesServiceImpl(
    private val expensesRepository: ExpensesRepository,
    private val expensesMapper: ExpensesMapper,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository
) : ExpensesService {

    @Transactional
    override fun create(expensesCreateRequest: ExpensesCreateRequest) {
        val category = categoryRepository.findByIdAndDeletedFalse(expensesCreateRequest.categoryId)
            ?: throw CategoryNotFoundException()
        val now = Date()
        if (expensesCreateRequest.date > now.time) throw FutureDateNotAllowedException()
        if (expensesCreateRequest.amount <= BigDecimal.ZERO) throw InvalidAmountException()
        val date = Date(expensesCreateRequest.date)
        expensesRepository.save(expensesMapper.toEntity(expensesCreateRequest, date, category, getCurrentUser(userRepository)))
    }

    override fun getOne(id: Long): ExpensesResponse =
        expensesRepository.findByIdAndDeletedFalse(id)?.let { expensesMapper.toDto(it, it.date.time) }
            ?: throw ExpensesNotFoundException()

    override fun getAll(): List<ExpensesResponse> =
        expensesRepository.findAll().map { expensesMapper.toDto(it, it.date.time) }

    @Transactional
    override fun update(id: Long, expensesUpdateRequest: ExpensesUpdateRequest) {
        val expenses = expensesRepository.findByIdAndDeletedFalse(id) ?: throw ExpensesNotFoundException()
        val now = Date()
        expensesUpdateRequest.run {
            title?.let { expenses.title = it }
            amount?.let { if (it <= BigDecimal.ZERO) throw InvalidAmountException() else expenses.amount = it }
            categoryId?.let {
                val category = categoryRepository.findByIdAndDeletedFalse(it) ?: throw CategoryNotFoundException()
                expenses.category = category
            }
            description?.let { expenses.description = it }
            date?.let { if (it > now.time) throw FutureDateNotAllowedException() else expenses.date = Date(it) }
        }
        expensesRepository.save(expenses)
    }

    override fun delete(id: Long) {
        expensesRepository.trash(id) ?: throw ExpensesNotFoundException()
    }

    override fun findAllUser(): List<ExpensesResponse> {
        val currentUser = getCurrentUser(userRepository)
        val expenses = expensesRepository.findAllByUser(currentUser)
       return expenses.map {
            expenses ->
            expensesMapper.toDto(expenses, expenses.date.time)
        }
    }
}



@Service
class CustomUserDetailsService(
    private val repository: UserRepository
) : UserDetailsService {
    override fun loadUserByUsername(phone: String): UserDetails {
        return repository.findByPhone(phone)?.let {
            UserDetailsResponse(
                id = it.id!!,
                myUsername = it.phone,
                fullName = it.fullName,
                role = it.role,
                myPassword = it.password,
                age = it.age
            )
        } ?: throw UserNotFoundException()
    }
}


fun getCurrentUser(userRepository: UserRepository): User {
    val auth = SecurityContextHolder.getContext().authentication
    if (auth == null || !auth.isAuthenticated) {
        throw NotLoggedInException()
    }

    val userDetails = auth.principal as UserDetails
    return userRepository.findByPhone(userDetails.username)
        ?: throw UserNotFoundException()
}

interface StatisticsService {
    fun statistics(request: StatisticsRequest): IntervalStatsResponse
}

@Service
class StatisticsServiceImpl(
    private val expensesRepository: ExpensesRepository,
    private val userRepository: UserRepository
): StatisticsService {

    override fun statistics(statisticsRequest: StatisticsRequest): IntervalStatsResponse {
        val user = getCurrentUser(userRepository)
        if (statisticsRequest.start > statisticsRequest.end) throw IllegalArgumentException("Start date must be <= end date")

        val startDate = millisToLocalDate(statisticsRequest.start)
        val endDate = millisToLocalDate(statisticsRequest.end)
        val days = ChronoUnit.DAYS.between(startDate, endDate) + 1
        val periodType = detectPeriod(days)
        val (prevStart, prevEnd) = calculatePreviousPeriod(startDate, endDate, periodType)

        val start = Date(statisticsRequest.start)
        val end = Date(statisticsRequest.end)
        val prevStartMillis = Date(localDateToMillis(prevStart))
        val prevEndMillis = Date(localDateToMillis(prevEnd))

        val currentExpenses = expensesRepository.findByDateRange(user.id!!, start, end)
        val totalCurrent = currentExpenses.sumOf { it.amount }

        val previousExpenses = expensesRepository.findByDateRange(user.id!!, prevStartMillis, prevEndMillis)
        val totalPrevious = previousExpenses.sumOf { it.amount }

        val difference = totalCurrent - totalPrevious

        val categoryList = expensesRepository.sumByCategory(user.id!!, start, end).map { row ->
            CategoryStatResponse(name = row[0] as String, total = row[1] as BigDecimal)
        }

        return IntervalStatsResponse(
            currentStart = statisticsRequest.start,
            currentEnd = statisticsRequest.end,
            previousStart = localDateToMillis(prevStart),
            previousEnd = localDateToMillis(prevEnd),
            totalAmount = totalCurrent,
            previousTotalAmount = totalPrevious,
            difference = difference,
            categories = categoryList
        )
    }

    private fun detectPeriod(days: Long) = when {
        kotlin.math.abs(days - 7) <= 2 -> PeriodType.WEEK
        kotlin.math.abs(days - 30) <= 5 -> PeriodType.MONTH
        kotlin.math.abs(days - 365) <= 10 -> PeriodType.YEAR
        else -> PeriodType.CUSTOM
    }

    private fun calculatePreviousPeriod(start: LocalDate, end: LocalDate, type: PeriodType): Pair<LocalDate, LocalDate> =
        when(type) {
            PeriodType.WEEK -> start.minusWeeks(1) to end.minusWeeks(1)
            PeriodType.MONTH -> start.minusMonths(1) to end.minusMonths(1)
            PeriodType.YEAR -> start.minusYears(1) to end.minusYears(1)
            PeriodType.CUSTOM -> {
                val days = ChronoUnit.DAYS.between(start, end) + 1
                start.minusDays(days) to end.minusDays(days)
            }
        }

    private fun millisToLocalDate(millis: Long) =
        Date(millis).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    private fun localDateToMillis(date: LocalDate) =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}


@Service
class StatisticsExportService(
    private val statisticsService: StatisticsService
) {

    fun exportToExcel(statisticsRequest: StatisticsRequest): ByteArray {
        val stats = statisticsService.statistics(statisticsRequest)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Statistics")

        val headerStyle: XSSFCellStyle = workbook.createCellStyle() as XSSFCellStyle
        headerStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerStyle.setFont(headerFont)

        val numberStyle: XSSFCellStyle = workbook.createCellStyle() as XSSFCellStyle
        numberStyle.dataFormat = workbook.creationHelper.createDataFormat().getFormat("#,##0.00")


        val headerRow = sheet.createRow(0)
        listOf("Category", "Amount").forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }


        stats.categories.forEachIndexed { i, category ->
            val row = sheet.createRow(i + 1)
            row.createCell(0).setCellValue(category.name)
            val amountCell = row.createCell(1)
            amountCell.setCellValue(category.total.toDouble())
            amountCell.cellStyle = numberStyle
        }

        val summaryStartRow = stats.categories.size + 2
        val labels = listOf("Total Current", "Total Previous", "Difference")
        val values = listOf(stats.totalAmount, stats.previousTotalAmount, stats.difference)

        labels.forEachIndexed { idx, label ->
            val row = sheet.createRow(summaryStartRow + idx)
            val labelCell = row.createCell(0)
            labelCell.setCellValue(label)
            labelCell.cellStyle = headerStyle

            val valueCell = row.createCell(1)
            valueCell.setCellValue(values[idx].toDouble())
            valueCell.cellStyle = numberStyle
        }

        sheet.autoSizeColumn(0)
        sheet.autoSizeColumn(1)

        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()
        return outputStream.toByteArray()
    }
}