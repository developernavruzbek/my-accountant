package org.example.myaccountant

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import kotlin.collections.map
import kotlin.run

@Component
class UserMapper(
    private val passwordEncoder:PasswordEncoder
){
    fun toEntity(userRequest: UserCreateRequest): User{
        userRequest.run {
            return User(
                fullName = fullName,
                phone = phone,
                password = passwordEncoder.encode(password),
                role = role,
                age = age,
            )
        }
    }

    fun toDto(user: User): UserResponse {
        return UserResponse(
            id = user.id!!,
            fullName = user.fullName,
            phone = user.phone,
            role = user.role,
            age = user.age
        )
    }

}

@Component
class CategoryMapper{
    fun toEntity(categoryCreateRequest: CategoryCreateRequest): Category{
        return Category(
            name = categoryCreateRequest.name,
            description = categoryCreateRequest.description
        )
    }

    fun toDto(category: Category): CategoryResponse{
        return CategoryResponse(
            id = category.id,
            name = category.name,
            description = category.description,
            createdDate = category.createdDate!!
        )
    }
}

