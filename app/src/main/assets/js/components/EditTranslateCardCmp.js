"use strict";

const EditTranslateCardCmp = ({card,translationEnabled,onCancelled,onSaved,onDeleted}) => {
    const {renderMessagePopup, showError, confirmAction, showMessageWithProgress} = useMessagePopup()

    const [textToTranslate, setTextToTranslate] = useState(card.textToTranslate)
    const [translation, setTranslation] = useState(card.translation)

    function isModified({initialValue, currValue}) {
        return initialValue != currValue
    }

    const textToTranslateIsModified = isModified({initialValue: card.textToTranslate, currValue:textToTranslate})
    const translationIsModified = isModified({initialValue: card.translation, currValue:translation})
    const dataIsModified = textToTranslateIsModified || translationIsModified

    async function cancel() {
        if (!dataIsModified || dataIsModified && await confirmAction({text: 'Your changes will be lost.'})) {
            onCancelled()
        }
    }

    async function saveChanges() {
        const closeProgressIndicator = showMessageWithProgress({text: 'Saving changes...'})
        const res = await be.updateTranslateCard({
            cardId: card.id,
            textToTranslate: textToTranslate,
            translation: translationEnabled ? translation : null,
        })
        closeProgressIndicator()
        if (!res.err) {
            onSaved()
        } else {
            await showError(res.err)
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
        re(UpdateTranslateCardCmp,{
            textToTranslate,
            textToTranslateOnChange: newValue => setTextToTranslate(newValue),
            textToTranslateBgColor: getBgColor(textToTranslateIsModified),
            translation: translationEnabled ? translation : null,
            translationOnChange: newValue => translationEnabled ? setTranslation(newValue) : null,
            translationBgColor: getBgColor(translationIsModified),
            onSave: saveChanges,
            saveDisabled: !dataIsModified || (textToTranslate.length === 0 || translation.length === 0),
            onCancel: cancel,
            onDelete: doDelete

        }),
        renderMessagePopup()
    )
}
