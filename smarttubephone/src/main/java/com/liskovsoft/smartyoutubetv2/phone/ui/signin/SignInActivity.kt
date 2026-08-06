package com.liskovsoft.smartyoutubetv2.phone.ui.signin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SignInPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView
import com.liskovsoft.smartyoutubetv2.common.utils.Utils
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneBaseActivity

class SignInActivity : PhoneBaseActivity(), SignInView {
    private lateinit var presenter: SignInPresenter
    private lateinit var codeView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var qrView: ImageView
    private var fullSignInUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { close() }
        codeView = findViewById(R.id.signin_code)
        descriptionView = findViewById(R.id.signin_description)
        qrView = findViewById(R.id.signin_qr)

        findViewById<MaterialButton>(R.id.btn_open_browser).setOnClickListener {
            val url = fullSignInUrl
            if (!url.isNullOrBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
        findViewById<MaterialButton>(R.id.btn_done).setOnClickListener { close() }

        presenter = SignInPresenter.instance(this)
        presenter.setView(this)
        presenter.onViewInitialized()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onViewDestroyed()
    }

    override fun showCode(userCode: String?, signInUrl: String?) {
        showCode(userCode, signInUrl, null)
    }

    override fun showCode(userCode: String?, signInUrl: String?, fullSignInUrl: String?) {
        codeView.text = userCode ?: ""
        this.fullSignInUrl = fullSignInUrl ?: signInUrl
        descriptionView.text = getString(
            com.liskovsoft.smartyoutubetv2.common.R.string.signin_view_description,
            signInUrl ?: ""
        )
        val qrUrl = Utils.toQrCodeLink(this.fullSignInUrl)
        if (!qrUrl.isNullOrBlank()) {
            Glide.with(this).load(qrUrl).into(qrView)
        }
    }

    override fun close() {
        finish()
    }
}
