package f95gm.client.ui.pages.auth

import dev.fritz2.core.*
import f95gm.client.actions.auth.signIn
import f95gm.client.actions.auth.updateCredentials
import f95gm.client.state.session.credentials
import f95gm.client.state.session.session
import f95gm.client.ui.shared.navigation.primaryNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.domain.api.F95Api
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.loginPage() {
    div("login-page position-relative min-vh-100 overflow-hidden bg-black") {
        div("login-shell container min-vh-100 d-flex align-items-center py-4 py-lg-5 position-relative z-1") {
            div("login-layout row justify-content-start w-100") {
                div("auth-card col-12 col-sm-10 col-md-7 col-lg-5 col-xl-4 d-flex") {
                    div("login-card card bg-black text-light border rounded-4 p-4 p-lg-5 w-100 align-self-center") {
                        div("login-brand d-flex align-items-center gap-3 mb-4") {
                            img("brand-image rounded-3") {
                                attr("width", "48")
                                attr("height", "48")
                                alt(UiMessages.app_brandName())
                                src("favicon.png")
                            }
                            div {
                                div("brand-kicker text-uppercase small fw-semibold") { +UiMessages.app_catalogClient() }
                                div("brand-name fw-bold fs-5") { +UiMessages.app_brandName() }
                            }
                        }
                        div("age-notice d-flex align-items-center gap-3 mb-3 p-3 border border-success border-opacity-50 rounded-3 bg-black bg-opacity-25") {
                            div("age-mark text-success fs-2 fw-bolder lh-1") { +"18+" }
                            div {
                                div("age-question text-white fw-bold") { +UiMessages.app_ageGateQuestion() }
                                div("age-copy text-white-50 small") { +UiMessages.app_ageGateNotice() }
                            }
                        }
                        h1("login-title h3 fw-bold mt-3 mb-2") { +UiMessages.app_signInToContinue() }
                        p("login-hint text-white-50 mb-0") { +UiMessages.app_credentialsHint() }
                        form("login-form vstack gap-2 mt-4") {
                            action(F95Api.LOGIN)
                            method("post")
                            label("form-label text-white-50 mt-2") { +UiMessages.app_usernameOrEmail() }
                            input(textFieldClass) {
                                type("text")
                                name("login")
                                autocomplete("username")
                                required(true)
                                placeholder(UiMessages.app_usernamePlaceholder())
                                value(credentials.data.map { it.login })
                                inputs.values() handledBy { updateCredentials(login = it) }
                            }
                            label("form-label text-white-50 mt-2") { +UiMessages.app_password() }
                            input(textFieldClass) {
                                type("password")
                                name("password")
                                autocomplete("current-password")
                                required(true)
                                placeholder(UiMessages.app_passwordPlaceholder())
                                value(credentials.data.map { it.password })
                                inputs.values() handledBy { updateCredentials(password = it) }
                            }
                            div("login-submit d-grid mt-3") {
                                button(primaryNavigationButtonClass) {
                                    type("button")
                                    +UiMessages.app_openCatalog()
                                    clicks handledBy { signIn() }
                                }
                            }
                            div("login-status status-slot") {
                                session.data.map { it.error to it.busy }.distinctUntilChanged().render {
                                    if (it.second) {
                                        div("alert alert-info py-2 mb-0") { +UiMessages.app_signingIn() }
                                    } else if (it.first.isNotBlank()) {
                                        div("alert alert-danger py-2 mb-0") { +it.first }
                                    }
                                }
                            }
                            submits { preventDefault() } handledBy { signIn() }
                        }
                        p("login-note text-white-50 small border-top pt-3 mt-4 mb-0") { +UiMessages.app_passwordNotSaved() }
                    }
                }
            }
        }
    }
}
