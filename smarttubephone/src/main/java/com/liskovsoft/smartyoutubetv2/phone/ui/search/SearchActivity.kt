package com.liskovsoft.smartyoutubetv2.phone.ui.search

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.search.MediaServiceSearchTagProvider
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.adapter.VideoCardAdapter
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneBaseActivity

class SearchActivity : PhoneBaseActivity(), SearchView {
    private lateinit var presenter: SearchPresenter
    private lateinit var searchInput: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var resultsList: RecyclerView
    private lateinit var suggestionsList: RecyclerView
    private lateinit var resultsAdapter: VideoCardAdapter
    private lateinit var suggestionsAdapter: SuggestionAdapter
    private var tagsProvider: MediaServiceSearchTagProvider? = null
    private var showingResults = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finishReally() }
        searchInput = findViewById(R.id.search_input)
        progressBar = findViewById(R.id.progress_bar)
        resultsList = findViewById(R.id.results_list)
        suggestionsList = findViewById(R.id.suggestions_list)

        resultsAdapter = VideoCardAdapter(
            onClick = { presenter.onVideoItemClicked(it) },
            onLongClick = { presenter.onVideoItemLongClicked(it) }
        )
        resultsList.layoutManager = LinearLayoutManager(this)
        resultsList.adapter = resultsAdapter
        resultsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lm = recyclerView.layoutManager as LinearLayoutManager
                val last = lm.findLastVisibleItemPosition()
                val items = resultsAdapter.items()
                if (items.isNotEmpty() && last >= items.size - 4) {
                    presenter.onScrollEnd(items.last())
                }
            }
        })

        suggestionsAdapter = SuggestionAdapter { tag ->
            searchInput.setText(tag)
            searchInput.setSelection(tag.length)
            hideKeyboard()
            presenter.onSearch(tag)
        }
        suggestionsList.layoutManager = LinearLayoutManager(this)
        suggestionsList.adapter = suggestionsAdapter

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                presenter.onSearch(searchInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (showingResults && s.isNullOrEmpty()) {
                    showingResults = false
                }
                requestSuggestions(s?.toString().orEmpty())
            }
        })

        presenter = SearchPresenter.instance(this)
        presenter.setView(this)
        presenter.onViewInitialized()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onViewDestroyed()
    }

    override fun updateSearch(group: VideoGroup?) {
        if (group == null) return
        showingResults = true
        suggestionsList.visibility = View.GONE
        resultsList.visibility = View.VISIBLE
        val replace = group.action == VideoGroup.ACTION_REPLACE
        resultsAdapter.submit(group.videos, replace)
    }

    override fun clearSearch() {
        resultsAdapter.clear()
    }

    override fun clearSearchTags() {
        suggestionsAdapter.submit(emptyList())
    }

    override fun removeSearchTag(tag: Tag?) {
        if (tag == null) return
        suggestionsAdapter.remove(tag)
    }

    override fun setTagsProvider(provider: MediaServiceSearchTagProvider?) {
        tagsProvider = provider
        requestSuggestions(searchInput.text?.toString().orEmpty())
    }

    override fun showProgressBar(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun startSearch(searchText: String?) {
        if (searchText != null) {
            searchInput.setText(searchText)
            searchInput.setSelection(searchText.length)
        }
        searchInput.requestFocus()
        searchInput.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun getSearchText(): String = searchInput.text?.toString().orEmpty()

    override fun startVoiceRecognition() {
        // Voice search is optional for phone Core+.
    }

    override fun finishReally() {
        finish()
    }

    private fun requestSuggestions(query: String) {
        if (showingResults && query.isNotEmpty()) {
            return
        }
        tagsProvider?.search(query) { tags ->
            runOnUiThread {
                if (showingResults) return@runOnUiThread
                val values = tags?.mapNotNull { it.tag }?.filter { it.isNotBlank() } ?: emptyList()
                suggestionsAdapter.submit(values)
                val show = values.isNotEmpty()
                suggestionsList.visibility = if (show) View.VISIBLE else View.GONE
                resultsList.visibility = if (show) View.GONE else View.VISIBLE
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }
}
