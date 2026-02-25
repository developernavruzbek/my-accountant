package org.example.myaccountant

import org.example.myaccountant.security.JwtService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

        val updated = userRepository.save(user)
        return mapper.toDto(updated)
    }


    override fun delete(id: Long) {
           userRepository.trash(id)
            ?: throw UserNotFoundException()
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
