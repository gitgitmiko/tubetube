package com.liskovsoft.smartyoutubetv2.phone.ui.search

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import androidx.recyclerview.widget.GridLayoutManager
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
    private lateinit var resultsAdapter: VideoCardAdapter
    private var tagsProvider: MediaServiceSearchTagProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finishReally() }
        searchInput = findViewById(R.id.search_input)
        progressBar = findViewById(R.id.progress_bar)
        resultsList = findViewById(R.id.results_list)

        resultsAdapter = VideoCardAdapter(
            onClick = { presenter.onVideoItemClicked(it) },
            onLongClick = { presenter.onVideoItemLongClicked(it) }
        )
        resultsList.layoutManager =
            GridLayoutManager(this, com.liskovsoft.smartyoutubetv2.phone.ui.PhoneUiMetrics.videoGridSpan(this))
        resultsList.adapter = resultsAdapter
        com.liskovsoft.smartyoutubetv2.phone.ui.PhoneUiMetrics.applyCenteredMaxWidth(resultsList)
        resultsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lm = recyclerView.layoutManager as GridLayoutManager
                val last = lm.findLastVisibleItemPosition()
                val items = resultsAdapter.items()
                if (items.isNotEmpty() && last >= items.size - 4) {
                    presenter.onScrollEnd(items.last())
                }
            }
        })

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                presenter.onSearch(searchInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }

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
        val replace = group.action == VideoGroup.ACTION_REPLACE
        resultsAdapter.submit(group.videos, replace)
    }

    override fun clearSearch() {
        resultsAdapter.clear()
    }

    override fun clearSearchTags() {}
    override fun removeSearchTag(tag: Tag?) {}

    override fun setTagsProvider(provider: MediaServiceSearchTagProvider?) {
        tagsProvider = provider
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
    }

    override fun getSearchText(): String = searchInput.text?.toString().orEmpty()

    override fun startVoiceRecognition() {
        // Voice search is optional for phone Core+.
    }

    override fun finishReally() {
        finish()
    }
}
