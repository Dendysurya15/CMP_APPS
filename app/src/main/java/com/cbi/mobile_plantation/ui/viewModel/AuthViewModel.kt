package com.cbi.mobile_plantation.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.mobile_plantation.data.model.LoginResponse
import com.cbi.mobile_plantation.data.repository.AuthRepository
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.launch
import retrofit2.Response

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginResponse = MutableLiveData<Response<LoginResponse>?>()
    val loginResponse: LiveData<Response<LoginResponse>?> get() = _loginResponse

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val response = repository.login(email, password)
            AppLogger.d(response.toString())
            _loginResponse.postValue(response)
        }
    }

    // ViewModel Factory inside AuthViewModel
    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}