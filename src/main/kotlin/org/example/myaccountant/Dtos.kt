package org.example.myaccountant

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.io.FileInputStream
import java.util.Date
import kotlin.collections.toList
import kotlin.sequences.forEach
import kotlin.text.toRegex

data class BaseMessage(
    val code: Long? = null,
    val message: String? = null
)

data class UserCreateRequest(
    val fullName: String,
    val phone:String,
    val password:String,
    val age: Long,
    val role: UserRole,
)

data class UserUpdateRequest(
    val fullName: String?,
    val phone: String?,
    val password: String?,
    val role: UserRole?,
    val age: Long?
)

data class UserResponse(
    val id: Long,
    val fullName: String,
    val phone: String,
    val role: UserRole,
    val age: Long,
)

data class UserDetailsResponse(
    val id: Long,
    val myUsername: String,
    val fullName: String?,
    val role: UserRole,
    val myPassword: String,
    val age: Long
) : UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return mutableListOf(SimpleGrantedAuthority(role.name))
    }

    override fun getPassword(): String {
        return myPassword
    }

    override fun getUsername(): String {
        return myUsername
    }
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true


}

data class LoginRequest(val phone: String, val password: String)
data class JwtResponse(val token: String)


data class CategoryCreateRequest(
    val name:String,
    val description:String
)

data class CategoryUpdateRequest(
    val name:String?,
    val description:String?
)

data class CategoryResponse(
    val id:Long?,
    val name:String,
    val description: String,
    val createdDate : Date?
)