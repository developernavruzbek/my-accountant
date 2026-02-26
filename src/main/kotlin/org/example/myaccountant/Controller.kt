package org.example.myaccountant


import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService
) {
    @PostMapping("/register")
    fun create(@RequestBody request: UserCreateRequest) = userService.create(request)

    @PostMapping("/login")
    fun login(@RequestBody req: LoginRequest): JwtResponse {
        return userService.loginIn(req)
    }
}


@RestController
@RequestMapping("/users")
@PreAuthorize("hasAuthority('ADMIN')")
class UserController(
    private val userService: UserService

){

    @GetMapping
    fun getAll(): List<UserResponse> {
        return userService.getAll()
    }


    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): UserResponse {
        return userService.getById(id)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody body: UserUpdateRequest
    ): UserResponse {
        return userService.update(id, body)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): BaseMessage {
        userService.delete(id)
        return BaseMessage(200, "User deleted successfully")
    }

}

@RestController
@RequestMapping("/category")
class CategoryController(
    private val categoryService: CategoryService
){
    @GetMapping
    fun getAll() = categoryService.getAll()

    @PostMapping
    fun create(@RequestBody categoryCreateRequest: CategoryCreateRequest) = categoryService.create(categoryCreateRequest)

    @GetMapping("/{categoryId}")
    fun getOne(@PathVariable categoryId:Long) = categoryService.getOne(categoryId)

    @PutMapping("/{categoryId}")
    fun update(@PathVariable categoryId:Long, @RequestBody categoryUpdateRequest: CategoryUpdateRequest) = categoryService.update(categoryId, categoryUpdateRequest)

    @DeleteMapping("/{categoryId}")
    fun delete(@PathVariable categoryId:Long) = categoryService.delete(categoryId)
}

@RestController
@RequestMapping("/expenses")
class ExpensesController(
    private val expensesService: ExpensesService
){
    @GetMapping
    fun getAll() = expensesService.getAll()

    @PostMapping
    fun create(@RequestBody expensesCreateRequest: ExpensesCreateRequest)  = expensesService.create(expensesCreateRequest)

    @GetMapping("/{id}")
    fun getOne(@PathVariable id:Long) = expensesService.getOne(id)

    @PutMapping("/{id}")
    fun update(@PathVariable id:Long, @RequestBody expensesUpdateRequest: ExpensesUpdateRequest) = expensesService.update(id, expensesUpdateRequest)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id:Long) = expensesService.delete(id)

}