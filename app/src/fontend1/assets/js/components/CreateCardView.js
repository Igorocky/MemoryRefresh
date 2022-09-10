"use strict";

const TEXT_TO_TRANSLATE_ID = 'textToTranslateId'
const TRANSLATION_ID = 'translationId'

const CreateCardView = ({query,openView,setPageTitle}) => {
    const {renderMessagePopup, showError, showMessageWithProgress} = useMessagePopup()

    const [allTags, setAllTags] = useState(null)
    const [allTagsMap, setAllTagsMap] = useState(null)
    const [errorLoadingTags, setErrorLoadingTags] = useState(null)

    const [paused, setPaused] = useState(false)
    const [direction, setDirection] = useState('BOTH')
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

    function getDirectionToSave() {
        return direction === 'BOTH' ? 'NATIVE_FOREIGN' : direction;
    }

    async function createCard() {
        const closeProgressIndicator = showMessageWithProgress({text: 'Saving new card...'})
        const res = await be.createTranslateCard({
            textToTranslate, translation, paused, tagIds,
            direction: direction === 'BOTH' ? 'NATIVE_FOREIGN' : direction,
            createReversedCard: direction === 'BOTH',
        })
        closeProgressIndicator()
        if (res.err) {
            await showError(res.err)
        } else {
            setTextToTranslate('')
            setTranslation('')
            setCardCounter(c => c + 1)
            document.getElementById(TEXT_TO_TRANSLATE_ID)?.focus()
        }
    }
    
    function extractWords() {
        const textToTranslateInput = document.getElementById(TEXT_TO_TRANSLATE_ID)
        if (textToTranslateInput) {
            const selectionStart = textToTranslateInput.selectionStart
            const selectionEnd = textToTranslateInput.selectionEnd
            if ((selectionStart??0) < (selectionEnd??0)) {
                const leftPart = textToTranslate.substring(0, selectionStart)
                const selectedText = textToTranslate.substring(selectionStart, selectionEnd)
                const rightPart = textToTranslate.substring(selectionEnd)
                setTextToTranslate(`${leftPart}[???]${rightPart}`)
                setTranslation(translation + selectedText)
                document.getElementById(TRANSLATION_ID)?.focus()
            }
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
                direction,directionOnChange:newValue => setDirection(newValue),directionExtended:true,
                textToTranslate,textToTranslateOnChange: newValue => setTextToTranslate(newValue),textToTranslateId: TEXT_TO_TRANSLATE_ID,
                textToTranslateOnExtractWords:extractWords,
                translation,translationOnChange: newValue => setTranslation(newValue),translationId: TRANSLATION_ID,
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
