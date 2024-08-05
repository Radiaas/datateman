package com.example.datateman

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friend")
class Friend (
    var name : String,
    var school : String,
    var hobby : String,
    var photo : String = ""
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}