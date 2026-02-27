package org.example.myaccountant

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date
import kotlin.apply
import kotlin.collections.map
import kotlin.run

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeletedForPageable(pageable: Pageable): Page<T>
    fun saveAndRefresh(t: T): T
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>,
    private val entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllNotDeletedForPageable(pageable: Pageable): Page<T> =
        findAll(isNotDeletedSpecification, pageable)

    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }

    @Transactional
    override fun saveAndRefresh(t: T): T {
        return save(t).apply { entityManager.refresh(this) }
    }
}

@Repository
interface UserRepository:BaseRepository<User> {
    fun findByPhone(phone: String): User?

}

@Repository
interface CategoryRepository: BaseRepository<Category>{
    fun findByName(name: String): Category?
}

@Repository
interface ExpensesRepository: BaseRepository<Expenses> {

    @Query("""
        SELECT e FROM Expenses e 
        WHERE e.deleted = false 
          AND e.user.id = :userId 
          AND e.date BETWEEN :start AND :end
    """)
    fun findByDateRange(
        @Param("userId") userId: Long,
        @Param("start") start: Date,
        @Param("end") end: Date
    ): List<Expenses>

    @Query("""
        SELECT e.category.name, SUM(e.amount) 
        FROM Expenses e
        WHERE e.deleted = false 
          AND e.user.id = :userId 
          AND e.date BETWEEN :start AND :end
        GROUP BY e.category.name
    """)
    fun sumByCategory(
        @Param("userId") userId: Long,
        @Param("start") start: Date,
        @Param("end") end: Date
    ): List<Array<Any>>

    fun findAllByUser(user: User): List<Expenses>
}