<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password'); section>
  <#if section = "header">
    ${msg("loginAccountTitle")}
  <#elseif section = "form">

  <div class="atg-login-wrapper">
    <div class="atg-login-card">

      <div class="atg-logo">
        <img src="${url.resourcesPath}/img/ATG.png" alt="ATG">
      </div>

      <h1 class="atg-title">${msg("loginAccountTitle")}</h1>
      <p class="atg-subtitle">Accede con tu cuenta ATG para continuar</p>

      <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
        <div class="atg-alert atg-alert-${message.type}">
          <span>${kcSanitize(message.summary)?no_esc}</span>
        </div>
      </#if>

      <form id="kc-form-login" class="atg-form" action="${url.loginAction}" method="post">

        <div class="atg-field">
          <label for="username" class="atg-label">
            <#if !realm.loginWithEmailAllowed>${msg("username")}
            <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
            <#else>${msg("email")}
            </#if>
          </label>
          <input
            tabindex="1"
            id="username"
            name="username"
            value="${(login.username!'')}"
            type="text"
            autocomplete="username"
            class="atg-input <#if messagesPerField.existsError('username','password')>atg-input-error</#if>"
            autofocus
          />
          <#if messagesPerField.existsError('username','password')>
            <span class="atg-field-error">${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}</span>
          </#if>
        </div>

        <div class="atg-field">
          <div class="atg-label-row">
            <label for="password" class="atg-label">${msg("password")}</label>
            <#if realm.resetPasswordAllowed>
              <a href="${url.loginResetCredentialsUrl}" class="atg-forgot-link">${msg("doForgotPassword")}</a>
            </#if>
          </div>
          <input
            tabindex="2"
            id="password"
            name="password"
            type="password"
            autocomplete="current-password"
            class="atg-input <#if messagesPerField.existsError('username','password')>atg-input-error</#if>"
          />
        </div>

        <#if realm.rememberMe && !usernameEditDisabled??>
          <div class="atg-remember">
            <label class="atg-checkbox-label">
              <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"
                <#if login.rememberMe??>checked</#if>>
              <span>${msg("rememberMe")}</span>
            </label>
          </div>
        </#if>

        <input type="hidden" id="id-hidden-input" name="credentialId"
          <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

        <button tabindex="4" type="submit" class="atg-submit-btn">
          ${msg("doLogIn")}
        </button>

      </form>

    </div>
  </div>

  </#if>
</@layout.registrationLayout>
