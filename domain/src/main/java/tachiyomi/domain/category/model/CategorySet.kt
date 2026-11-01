package tachiyomi.domain.category.model

data class CategorySet(
    val id: Long,
    val syncKey: String,
    val name: String,
    val order: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
