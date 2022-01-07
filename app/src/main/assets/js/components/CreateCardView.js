"use strict";

const CreateCardView = ({query,openView,setPageTitle}) => {
    const {renderMessagePopup, showError, showMessageWithProgress} = useMessagePopup()

    const [allTags, setAllTags] = useState(null)
    const [allTagsMap, setAllTagsMap] = useState(null)
    const [errorLoadingTags, setErrorLoadingTags] = useState(null)

    const [paused, setPaused] = useState(false)
    const [textToTranslate, setTextToTranslate] = useState('')
    const [translation, setTranslation] = useState('')
    const [tagIds, setTagIds] = useState([])
    const [cardCounter, setCardCounter] = useState(0)

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
        }
    }, [])

    async function createCard() {
        const closeProgressIndicator = showMessageWithProgress({text: 'Saving new card...'})
        const res = await be.createTranslateCard({textToTranslate, translation, paused, tagIds})
        closeProgressIndicator()
        if (res.err) {
            await showError(res.err)
        } else {
            setTextToTranslate('')
            setTranslation('')
            setCardCounter(c => c + 1)
        }
    }

    function renderPageContent() {
        if (errorLoadingTags) {
            return RE.Fragment({},
                `An error occurred during loading of tags: [${errorLoadingTags.code}] - ${errorLoadingTags.msg}`,
            )
        } else if (hasNoValue(allTags) || hasNoValue(allTagsMap)) {
            return 'Loading tags...'
        } else {
            return re(EditTranslateCardForm,{
                key: cardCounter,
                allTags, allTagsMap,
                paused,pausedOnChange:newValue => setPaused(newValue),
                textToTranslate,textToTranslateOnChange: newValue => setTextToTranslate(newValue),
                translation,translationOnChange: newValue => setTranslation(newValue),
                tagIds,tagIdsOnChange:newValue => setTagIds(newValue),
                onSave: createCard, saveDisabled: !textToTranslate.length || !translation.length
            })
        }
    }

    return RE.Fragment({},
        renderPageContent(),
        renderMessagePopup()
    )
}
