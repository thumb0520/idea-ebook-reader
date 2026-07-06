package com.ebook.reader

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "EbookSettings",
    storages = [Storage("ebook-reader.xml")]
)
class EbookSettings : PersistentStateComponent<EbookSettings.State> {
    data class State(
        var lastBookPath: String? = null,
        var lastChapter: Int = 0,
        var lastScrollPosition: Int = 0,
        var fontSize: Int = 12
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }
}
