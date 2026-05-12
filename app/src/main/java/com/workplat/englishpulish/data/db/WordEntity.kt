package com.workplat.englishpulish.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [Index(value = ["lemma"], unique = true)],
)
data class WordEntity(
    @PrimaryKey val id: String,
    val lemma: String,
    val phonetic: String?,
    val partOfSpeech: String?,
    val definitionZh: String,
    val exampleEn: String?,
    val exampleZh: String?,
    val source: String,
    val createdAt: Long,
    val deletedAt: Long?,
)
