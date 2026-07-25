package com.classiiiai.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);

    @androidx.room.Update
    void updateUser(User user);

    @Query("SELECT * FROM users ORDER BY loginTimestamp DESC LIMIT 1")
    User getLastLoggedInUser();
    
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("DELETE FROM users")
    void deleteAllUsers();

    @Query("SELECT COUNT(*) FROM users")
    int getTotalUsers();
    
    @Query("SELECT * FROM users")
    java.util.List<User> getAllUsers();
}
