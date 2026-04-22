package hka.awp.temi_cgi_app.koin

import hka.awp.temi_cgi_app.ui.shell.SidebarViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel {
        SidebarViewModel()
    }
}