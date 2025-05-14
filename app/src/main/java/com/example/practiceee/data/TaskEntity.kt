package com.example.practiceee.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val task: String,
    val date: String?,
    val time: String?,
    val priority: String,
     val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),  // epoch millis
    val completedAt: Long? = null                     // null if not done
)


@Entity(tableName = "tasks")
data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<Article>
)

data class Article(
    val title: String,
    val description: String?,
    val urlToImage: String?,
    val publishedAt: String,
    val url: String
)

