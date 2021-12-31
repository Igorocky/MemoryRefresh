"use strict";

const TranslateCardFilterCmp = ({onSubmit}) => {
    const {renderMessagePopup, showMessage, confirmAction, showError} = useMessagePopup()

    const [allTags, setAllTags] = useState(null)
    const [allTagsMap, setAllTagsMap] = useState(null)
    const [errorLoadingTags, setErrorLoadingTags] = useState(null)

    const [cardToTagsMap, setCardToTagsMap] = useState(null)
    const [errorLoadingCardToTagsMap, setErrorLoadingCardToTagsMap] = useState(null)

    const [tagsToInclude, setTagsToInclude] = useState([])
    const [tagsToExclude, setTagsToExclude] = useState([])
    const [remainingTags, setRemainingTags] = useState([])

    useEffect(async () => {
        const res = await be.readAllTags()
        if (res.err) {
            setErrorLoadingTags(res.err)
            showError(res.err)
        } else {
            const allTags = res.data
            setAllTags(allTags)
            const allTagsMap = {}
            for (const tag of allTags) {
                allTagsMap[tag.id] = tag
            }
            setAllTagsMap(allTagsMap)

            const res2 = await be.getCardToTagMapping()
            if (res2.err) {
                setErrorLoadingCardToTagsMap(res2.err)
                showError(res2.err)
            } else {
                setCardToTagsMap(res2.data)
            }
        }
    }, [])

    useEffect(async () => {
        if (cardToTagsMap) {
            recalculateRemainingTags()
        }
    }, [cardToTagsMap, tagsToInclude, tagsToExclude])

    function recalculateRemainingTags() {
        const remainingTagIds = []
        for (const cardId in cardToTagsMap) {
            const cardTagIds = cardToTagsMap[cardId]
            let cardPassed = true
            for (const tag of tagsToInclude) {
                if (!cardTagIds.includes(tag.id)) {
                    cardPassed = false
                    break
                }
            }
            if (cardPassed) {
                for (const tag of tagsToExclude) {
                    if (cardTagIds.includes(tag.id)) {
                        cardPassed = false
                        break
                    }
                }
                if (cardPassed) {
                    remainingTagIds.push(...cardTagIds)
                }
            }
        }
        const tagIdsToInclude = tagsToInclude.map(t=>t.id)
        const tagIdsToExclude = tagsToExclude.map(t=>t.id)
        const remainingTags = remainingTagIds
            .distinct()
            .filter(tagId => !tagIdsToInclude.includes(tagId) && !tagIdsToExclude.includes(tagId))
            .map(tagId => allTagsMap[tagId])
        setRemainingTags(remainingTags)
    }

    function renderComponentContent() {
        if (errorLoadingTags) {
            return RE.Fragment({},
                `An error occurred during loading of tags: [${errorLoadingTags.code}] - ${errorLoadingTags.msg}`,
            )
        } else if (errorLoadingCardToTagsMap) {
            return RE.Fragment({},
                `An error occurred during loading of card to tags mapping: [${errorLoadingCardToTagsMap.code}] - ${errorLoadingCardToTagsMap.msg}`,
            )
        } else if (hasNoValue(allTags) || hasNoValue(allTagsMap) || hasNoValue(cardToTagsMap)) {
            return 'Loading tags...'
        } else {
            return RE.Container.col.top.left({style:{marginTop:'5px'}},{style:{marginTop:'5px'}},
                RE.Paper({},
                    re(TagSelector,{
                        allTags: remainingTags,
                        selectedTags: tagsToInclude,
                        onTagRemoved:tag=>{
                            setTagsToInclude(prev=>prev.filter(t=>t.id!=tag.id))
                        },
                        onTagSelected:tag=>{
                            setTagsToInclude(prev=>[...prev,tag])
                        },
                        label: 'Include',
                        color:'primary',
                    })
                ),
                RE.Paper({},
                    re(TagSelector,{
                        allTags: remainingTags,
                        selectedTags: tagsToExclude,
                        onTagRemoved:tag=>{
                            setTagsToExclude(prev=>prev.filter(t=>t.id!=tag.id))
                        },
                        onTagSelected:tag=>{
                            setTagsToExclude(prev=>[...prev,tag])
                        },
                        label: 'Exclude',
                        color:'secondary',
                    })
                ),
            )
        }
    }

    return RE.Fragment({},
        renderComponentContent(),
        renderMessagePopup()
    )
}
