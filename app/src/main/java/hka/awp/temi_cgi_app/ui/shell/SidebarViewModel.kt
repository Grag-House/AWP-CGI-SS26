package hka.awp.temi_cgi_app.ui.shell

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SidebarViewModel : ViewModel() {
    var selectedRoute by mutableStateOf(Screen.Dashboard.route)
        private set

    var isSidebarExpanded by mutableStateOf(true)
        private set

    fun onRouteSelect(screen: Screen) {
        selectedRoute = screen.route
        Log.d(this.javaClass.simpleName, "TODO routing")
    }

    fun onSideBarToggle() {
        isSidebarExpanded = !isSidebarExpanded
        Log.d(this.javaClass.simpleName, "Sidepanel collapse triggered, currently: $isSidebarExpanded")
    }
}