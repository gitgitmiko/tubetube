package com.liskovsoft.smartyoutubetv2.phone.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneBaseActivity
import java.util.ArrayDeque

class AppDialogActivity : PhoneBaseActivity(), AppDialogView {
    private lateinit var presenter: AppDialogPresenter
    private lateinit var titleView: TextView
    private lateinit var list: RecyclerView
    private lateinit var adapter: CategoryAdapter

    private val backstack = ArrayDeque<DialogPage>()
    private var shown = false
    private var paused = false
    private var transparent = false
    private var overlay = false
    private var viewId = 0

    private data class DialogPage(
        val categories: List<OptionCategory>,
        val title: CharSequence?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)

        titleView = findViewById(R.id.dialog_title)
        list = findViewById(R.id.dialog_list)
        adapter = CategoryAdapter()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        findViewById<MaterialButton>(R.id.btn_close).setOnClickListener { finish() }

        presenter = AppDialogPresenter.instance(this)
        presenter.setView(this)
        // Preference-style dialogs call this when UI is ready.
        presenter.onViewInitialized()
        shown = true
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        paused = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (presenter.view === this) {
            presenter.onViewDestroyed()
        }
        shown = false
    }

    override fun onBackPressed() {
        if (canGoBack()) {
            goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun show(
        categories: MutableList<OptionCategory>?,
        title: CharSequence?,
        isExpandable: Boolean,
        isTransparent: Boolean,
        isOverlay: Boolean,
        id: Int
    ) {
        transparent = isTransparent
        overlay = isOverlay
        viewId = id
        val cats = categories?.toList() ?: emptyList()
        backstack.clear()
        backstack.addLast(DialogPage(cats, title))
        render(backstack.last())
        shown = true
    }

    private fun render(page: DialogPage) {
        titleView.text = page.title ?: getString(R.string.app_name)
        adapter.submit(page.categories)
    }

    override fun finish() {
        shown = false
        presenter.onFinish()
        try {
            super.finish()
        } catch (_: Exception) {
            // ignore OEM finish issues
        }
    }

    override fun goBack() {
        if (backstack.size > 1) {
            backstack.removeLast()
            render(backstack.last())
        }
    }

    override fun clearBackstack() {
        if (backstack.isNotEmpty()) {
            val current = backstack.last()
            backstack.clear()
            backstack.addLast(current)
        }
    }

    override fun canGoBack(): Boolean = backstack.size > 1

    override fun isShown(): Boolean = shown && !isFinishing

    override fun isTransparent(): Boolean = transparent

    override fun isOverlay(): Boolean = overlay

    override fun isPaused(): Boolean = paused

    override fun getViewId(): Int = viewId

    private inner class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.Holder>() {
        private val categories = mutableListOf<OptionCategory>()

        fun submit(items: List<OptionCategory>) {
            categories.clear()
            categories.addAll(items)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_option_category, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val category = categories[position]
            holder.title.text = category.title
            holder.title.visibility =
                if (category.title.isNullOrBlank()) View.GONE else View.VISIBLE
            holder.options.removeAllViews()

            when (category.type) {
                OptionCategory.TYPE_RADIO_LIST -> bindRadio(holder.options, category.options)
                OptionCategory.TYPE_CHECKBOX_LIST -> bindCheckboxes(holder.options, category.options)
                OptionCategory.TYPE_SINGLE_SWITCH -> bindSwitch(holder.options, category.options)
                else -> bindButtons(holder.options, category.options)
            }
        }

        private fun bindRadio(container: LinearLayout, options: List<OptionItem>?) {
            if (options == null) return
            val group = RadioGroup(container.context)
            group.orientation = LinearLayout.VERTICAL
            options.forEach { option ->
                val radio = RadioButton(container.context)
                radio.text = option.title
                radio.setTextColor(resources.getColor(R.color.phone_on_surface))
                radio.isChecked = option.isSelected
                radio.setOnClickListener {
                    option.onSelect(true)
                    notifyDataSetChanged()
                }
                group.addView(radio)
            }
            container.addView(group)
        }

        private fun bindCheckboxes(container: LinearLayout, options: List<OptionItem>?) {
            if (options == null) return
            options.forEach { option ->
                val check = CheckBox(container.context)
                check.text = option.title
                check.setTextColor(resources.getColor(R.color.phone_on_surface))
                check.isChecked = option.isSelected
                check.setOnCheckedChangeListener { _, isChecked ->
                    option.onSelect(isChecked)
                }
                container.addView(check)
            }
        }

        private fun bindSwitch(container: LinearLayout, options: List<OptionItem>?) {
            bindCheckboxes(container, options)
        }

        private fun bindButtons(container: LinearLayout, options: List<OptionItem>?) {
            if (options == null) return
            options.forEach { option ->
                val button = MaterialButton(container.context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
                button.text = option.title
                button.setOnClickListener {
                    option.onSelect(true)
                }
                container.addView(button)
            }
        }

        override fun getItemCount(): Int = categories.size

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.category_title)
            val options: LinearLayout = view.findViewById(R.id.options_container)
        }
    }
}
