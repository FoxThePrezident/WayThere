package com.wayThereTeam.wayThere.utilities

/**
 * Clearing html page from things that we do not need
 * @property page is html string that we want to format
 */
@Suppress("SameParameterValue")
class Trimming(private var page: String) {
    //main function for controlling a clearing process
    fun run(): String {
        // narrowing search
        removeUntil("<div", "class", "connection-list")
        removeAfter("<div", "id", "row-btns")

        // deleting tags by selectors that I don't need
        tagsAndBetween("<p", "/p>", "specs")
        tagsAndBetween("<div", "/div>", "date-total")
        tagsAndBetween("<div", "/div>", "connection-expand")

        trimWords(">>")
        trimTags("<script", "/script>")

        // deleting tags and adding new line after them
        trimWords("</p>", "\n")
        trimWords("</h2>", "\n")
        trimWords("</h3>", "\n")

        // removing every leftover tag
        trimTags("<", ">")

        // removing leftover empty lines
        page = page.replace(Regex("^\\s*\n", RegexOption.MULTILINE), "")
        // removing empty space at the start of line
        page = page.replace(Regex("""^[ \t]+""", RegexOption.MULTILINE), "")
        return page
    }

    /**
     * Will remove everything from start until occurrence of an input
     * @param startTag html tag that will focus on, like <div
     * @param selector either class or id
     * @param filterBy parameter for selector, like header
     */
    private fun removeUntil(startTag: String, selector: String, filterBy: String) {
        // searching for matches
        val startingTagPattern = """$startTag $selector="$filterBy""".toRegex()
        val matchResult = startingTagPattern.find(page)

        // deleting found match
        if (matchResult != null) {
            val startIndex = matchResult.range.first
            page = page.substring(startIndex).trim()
        }
    }

    /**
     * Will remove everything after occurrence based on an input
     * @param startTag html tag that will focus on, like <div
     * @param selector either class or id
     * @param filterBy parameter for selector, like header
     */
    private fun removeAfter(startTag: String, selector: String, filterBy: String) {
        // searching for matches
        val startingTagPattern = """$startTag $selector="$filterBy""".toRegex()
        val matchResult = startingTagPattern.find(page)

        // deleting found match
        if (matchResult != null) {
            val startIndex = matchResult.range.first
            page = page.substring(0, startIndex).trim()
        }
    }

    /**
     * Removing individual words
     * @param word is a String, that will be replaced
     * @param replaceItWith defines, that with what string will be replacing occurrences
     * @param onlyOneTag tels if we want to remove everything, or only just one occurrence
     */
    private fun trimWords(word: String, replaceItWith: String = "", onlyOneTag: Boolean = false) {
        do {
            // finding index of word that was provided
            val index = page.indexOf(word)
            if (index == -1) {
                break
            }
            page = page.replace(word, replaceItWith).trim()
            // checking if cycle needs to repeat
        } while (!onlyOneTag)
    }

    /**
     * Will remove strings of text based on a starting point and ending point
     * @param startTag check for the starting point of removing process, like <
     * @param endTag check for ending point of removing, like >
     * @param replaceItWith defines, that with what string will be replacing occurrences
     * @param onlyOneTag tels if we want to remove everything, or only just one occurrence
     */
    private fun trimTags(startTag: String, endTag: String, replaceItWith: String = "", onlyOneTag: Boolean = false) {
        do {
            // getting indexes
            val startIndex = page.indexOf(startTag)
            val endIndex = page.indexOf(endTag, startIndex)

            // checking for valid indexes
            if (startIndex != -1 && endIndex != -1) {
                // removing selected text
                val textToRemove = page.substring(startIndex, endIndex + endTag.length)
                page = page.replace(textToRemove, replaceItWith).trim()
            } else {
                break
            }
            // checking if cycle needs to repeat
        } while (!onlyOneTag)
    }

    /**
     * Removing tags and everything in between based on a selector
     * @param startTag check for the starting point of removing process, like <div
     * @param endTag check for ending point of removing, like /div>
     * @param filterBy parameter for selector, like header
     * @param selector either class or id
     * @param onlyOneTag tels if we want to remove everything, or only just one occurrence
     */
    private fun tagsAndBetween(
        startTag: String, endTag: String, filterBy: String, selector: String = "class", onlyOneTag: Boolean = false
    ) {
        val startingTag = """$startTag $selector="$filterBy"""".toRegex().toString()
        do {
            // getting indexes
            val startIndex = page.indexOf(startingTag)
            var endIndex = page.indexOf(endTag, startIndex)

            // checking for valid start and end indexes
            if (startIndex != -1 && endIndex != -1) {
                // looping over selected text and checking if it is containing another occurrence of starting tag
                var subChildrenIndex = startIndex
                while (true) {
                    // finding the next occurrence of opening tag
                    subChildrenIndex = page.indexOf(startTag, subChildrenIndex + 1)
                    // checking if index of subChildrenIndex surpassed endIndex
                    if (subChildrenIndex != -1 && subChildrenIndex < endIndex) {
                        // if subChildrenIndex did not surpass the endIndex, then code is finding next occurrence of endIndex
                        endIndex = page.indexOf(endTag, endIndex + 1)
                    } else {
                        // if subChildrenIndex did surpass the endIndex, it will break from a loop
                        break
                    }
                }
                // replacing selected text with empty
                val textToRemove = page.substring(startIndex, endIndex + endTag.length)
                page = page.replace(textToRemove, "").trim()
            } else {
                break
            }
        } while (!onlyOneTag)
    }
}