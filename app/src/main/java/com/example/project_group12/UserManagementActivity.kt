package com.example.project_group12

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_group12.data.AppDatabase
import com.example.project_group12.data.UserModel
import com.example.project_group12.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserManagementActivity : AppCompatActivity() {
    private lateinit var repository: AuthRepository
    private lateinit var adapter: UserAdapter
    private lateinit var rvUsers: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        val dao = AppDatabase.getDatabase(this).appDao()
        repository = AuthRepository(dao)
        rvUsers = findViewById(R.id.rvUsers)
        rvUsers.layoutManager = LinearLayoutManager(this)

        // BẮT SỰ KIỆN NÚT QUAY LẠI
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Lệnh này sẽ đóng trang hiện tại và quay về trang trước đó
        }

        adapter = UserAdapter(
            users = emptyList(),
            onRoleToggle = { user -> toggleUserRole(user) },
            onDeleteClick = { user -> showDeleteConfirmDialog(user) }
        )
        rvUsers.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            val result = repository.getAllUsers()
            result.onSuccess { users ->
                adapter.updateData(users)
            }.onFailure {
                Toast.makeText(this@UserManagementActivity, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleUserRole(user: UserModel) {
        val newRole = if (user.role == "admin") "user" else "admin"
        lifecycleScope.launch {
            val result = repository.updateUserRole(user.uid, newRole)
            result.onSuccess {
                Toast.makeText(this@UserManagementActivity, "Đã cập nhật quyền thành công", Toast.LENGTH_SHORT).show()
                loadUsers()
            }.onFailure {
                Toast.makeText(this@UserManagementActivity, "Lỗi khi cập nhật quyền", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmDialog(user: UserModel) {
        AlertDialog.Builder(this)
            .setTitle("Xóa tài khoản")
            .setMessage("Bạn có chắc chắn muốn xóa thành viên '${user.displayName}' khỏi hệ thống không?")
            .setPositiveButton("Xóa") { _, _ ->
                lifecycleScope.launch {
                    val result = repository.deleteUser(user.uid)
                    result.onSuccess {
                        Toast.makeText(this@UserManagementActivity, "Đã xóa người dùng", Toast.LENGTH_SHORT).show()
                        loadUsers() // Tải lại danh sách
                    }.onFailure {
                        Toast.makeText(this@UserManagementActivity, "Lỗi khi xóa", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}