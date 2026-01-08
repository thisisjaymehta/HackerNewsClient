package com.neuralquark.hackernewsclient.data.model

/**
 * API response models for Hacker News Firebase API
 */
data class HNItemResponse(
    val id: Long,
    val deleted: Boolean? = false,
    val type: String? = null,
    val by: String? = null,
    val time: Long? = null,
    val text: String? = null,
    val dead: Boolean? = false,
    val parent: Long? = null,
    val poll: Long? = null,
    val kids: List<Long>? = null,
    val url: String? = null,
    val score: Int? = null,
    val title: String? = null,
    val parts: List<Long>? = null,
    val descendants: Int? = null
)

fun HNItemResponse.toStory(category: StoryCategory, orderIndex: Int = 0): Story? {
    return if (type == "story" || type == "job" || type == "poll") {
        Story(
            id = id,
            title = title ?: return null,
            url = url,
            by = by ?: "unknown",
            score = score ?: 0,
            time = time ?: 0,
            descendants = descendants ?: 0,
            type = type ?: "story",
            text = text,
            category = category,
            orderIndex = orderIndex
        )
    } else null
}

fun HNItemResponse.toComment(storyId: Long): Comment? {
    return if (type == "comment") {
        Comment(
            id = id,
            storyId = storyId,
            parent = parent ?: storyId,
            by = by,
            text = text,
            time = time ?: 0,
            deleted = deleted ?: false,
            dead = dead ?: false,
            kids = kids ?: emptyList()
        )
    } else null
}
