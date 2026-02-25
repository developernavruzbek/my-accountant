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

    /*
    fun toDto(user: User): UserResponse{
        user.run {
            return UserResponse(
                id = id,
                firstName = firstName,
                role = role,
                status = status
            )
        }
    }

    fun toDtoFull(user: User): UserFullResponse{
        user.run {
            return UserFullResponse(
                id = id,
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                role  = role,
                wareHouseId = wareHouse.id,
                wareHouseName = wareHouse.name,
                status = status
            )
        }
    }

     */


}

