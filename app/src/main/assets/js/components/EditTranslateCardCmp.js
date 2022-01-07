"use strict";

const EditTranslateCardCmp = ({allTags, allTagsMap, card, reducedMode = false, onCancelled, onSaved, onDeleted}) => {
    const {renderMessagePopup, showError, confirmAction, showMessageWithProgress} = useMessagePopup()

    const [textToTranslate, setTextToTranslate] = useState(card.textToTranslate)
    const [translation, setTranslation] = useState(card.translation)
    const [paused, setPaused] = useState(card.paused)
    const [tagIds, setTagIds] = useState(card.tagIds)
    const [delay, setDelay] = useState(card.schedule.delay)
    const createdAt = useMemo(() => new Date(card.createdAt), [card.createdAt])

    const {renderValidationHistory} = useTranslateCardHistory({cardId:card.id})

    useEffect(() => {
        window.scrollTo(0, 0)
    }, [])

    function isModified({initialValue, currValue}) {
        if (Array.isArray(initialValue)) {
            return !arraysAreEqualAsSets(initialValue, currValue)
        } else {
            return initialValue !== currValue
        }
    }

    const textToTranslateIsModified = isModified({initialValue: card.textToTranslate, currValue:textToTranslate})
    const translationIsModified = isModified({initialValue: card.translation, currValue:translation})
    const pausedIsModified = isModified({initialValue: card.paused, currValue:paused})
    const tagIdsIsModified = isModified({initialValue: card.tagIds, currValue:tagIds})
    const delayIsModified = isModified({initialValue: card.schedule.delay, currValue:delay})
    const dataIsModified = textToTranslateIsModified || translationIsModified || pausedIsModified || tagIdsIsModified || delayIsModified

    async function doCancel() {
        if (!dataIsModified || dataIsModified && await confirmAction({text: 'Your changes will be lost.'})) {
            onCancelled()
        }
    }

    async function doSave() {
        const closeProgressIndicator = showMessageWithProgress({text: 'Saving changes...'})
        const res = await be.updateTranslateCard({
            cardId: card.id,
            paused: pausedIsModified?paused:null,
            delay: delayIsModified?delay:null,
            recalculateDelay: delayIsModified,
            tagIds: tagIdsIsModified?tagIds:null,
            textToTranslate: textToTranslateIsModified?textToTranslate:null,
            translation: translationIsModified ? translation : null,
        })
        closeProgressIndicator()
        if (res.err) {
            await showError(res.err)
        } else {
            onSaved()
        }
    }

    async function doDelete() {
        if (await confirmAction({text: 'Delete this card?', okBtnColor: 'secondary'})) {
            const closeProgressIndicator = showMessageWithProgress({text: 'Deleting...'})
            const res = await be.deleteTranslateCard({cardId:card.id})
            closeProgressIndicator()
            if (res.err) {
                await showError(res.err)
            } else {
                onDeleted()
            }
        }
    }

    function getBgColor(isModified) {
        if (isModified) {
            return '#ffffcc'
        }
    }

    return RE.Fragment({},
        re(EditTranslateCardForm,{
            allTags, allTagsMap,

            paused,
            pausedOnChange: newValue=>setPaused(newValue),
            pausedBgColor: getBgColor(pausedIsModified),

            textToTranslate,
            textToTranslateOnChange: newValue => setTextToTranslate(newValue),
            textToTranslateBgColor: getBgColor(textToTranslateIsModified),

            translation: reducedMode ? null : translation,
            translationOnChange: newValue => setTranslation(newValue),
            translationBgColor: getBgColor(translationIsModified),

            delay: reducedMode ? null : delay,
            delayOnChange: newValue => setDelay(newValue),
            delayBgColor: getBgColor(delayIsModified),

            tagIds: reducedMode ? null : tagIds,
            tagIdsOnChange: newValue => setTagIds(newValue),
            tagIdsBgColor: getBgColor(tagIdsIsModified),

            activatesIn: reducedMode ? null : card.activatesIn,
            createdAt,

            onSave: doSave,
            saveDisabled: !dataIsModified || (textToTranslate.length === 0 || (!reducedMode && translation.length === 0)),

            onCancel: doCancel,
            onDelete: doDelete
        }),
        renderValidationHistory(),
        renderMessagePopup()
    )
}
