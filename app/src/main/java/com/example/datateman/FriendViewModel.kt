package com.example.datateman

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow

class FriendViewModel(private val friendDao: FriendDao) : ViewModel() {

    fun getFriend(): Flow<List<Friend>> = friendDao.getAll()

    suspend fun insertFriend(data: Friend) {
        friendDao.insert(data)
    }

    suspend fun updateFriend(data: Friend) {
        friendDao.update(data)
    }

    suspend fun deleteFriend(data: Friend) {
        friendDao.delete(data)
    }
}
