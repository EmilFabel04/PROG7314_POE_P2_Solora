package dev.solora.i18n

import android.content.Context
import android.content.ContextWrapper
import android.os.LocaleList
import java.util.Locale

object I18n {
    enum class Lang(val locale: Locale) { EN(Locale("en")), AF(Locale("af")), XH(Locale("xh")) }

    fun wrap(context: Context, lang: Lang): ContextWrapper {
        val config = context.resources.configuration
        config.setLocales(LocaleList(lang.locale))
        val ctx = context.createConfigurationContext(config)
        return ContextWrapper(ctx)
    }
}


