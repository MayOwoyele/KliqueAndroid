import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justself.klique.JWTNetworkCaller
import com.justself.klique.KliqueHttpMethod
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.NetworkUtils
import com.justself.klique.SessionManager
import com.justself.klique.WebSocketManager
import com.justself.klique.networkTriple
import kotlinx.coroutines.launch
import org.json.JSONObject

class SettingsViewModel : ViewModel() {
    val showDeleteAccountDialog = mutableStateOf(false)

    val deletionReason = mutableStateOf("")

    val isDeletingAccount = mutableStateOf(false)

    fun showDeleteDialog() {
        showDeleteAccountDialog.value = true
    }

    fun dismissDeleteDialog() {
        showDeleteAccountDialog.value = false
        deletionReason.value = ""
    }
    fun deleteAccount(onFailure: (String) -> Unit) {
        if (isDeletingAccount.value) return

        isDeletingAccount.value = true
        viewModelScope.launch {
            try {
                val jsonObject = JSONObject().put("userId", SessionManager.customerId.value).toString()
                val response: suspend () -> networkTriple = {
                    NetworkUtils.makeJwtRequest("deleteMyAccount", KliqueHttpMethod.POST, emptyMap(), jsonObject)
                }
                val action: suspend (networkTriple) -> Unit = {
                    if (it.first) {
                        Toast.makeText(appContext, "Successfully deleted your account", Toast.LENGTH_LONG).show()
                        SessionManager.resetCustomerData()
                        WebSocketManager.close()
                    } else {
                        Toast.makeText(appContext, "Temporary issues with deleting your account", Toast.LENGTH_LONG).show()
                    }
                }
                val error: suspend (networkTriple) -> Unit = {
                    onFailure("Unknown error occurred")
                }
                JWTNetworkCaller.performReusableNetworkCalls(response, action, error)
            } catch (e: Exception) {
                onFailure(e.message ?: "Unknown error occurred")
            }
        }
    }
}