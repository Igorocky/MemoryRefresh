"use strict";

const EditTranslateCardCmp = ({card,translationEnabled,onDone}) => {
    const {renderMessagePopup, showMessage, showError} = useMessagePopup()

    const [textToTranslate, setTextToTranslate] = useState(card.textToTranslate)
    const [translation, setTranslation] = useState(card.translation)
    const [delay, setDelay] = useState(card.schedule.delay)

    async function saveChanges() {
        const res = await be.updateTranslateCard({
            cardId: card.id,
            textToTranslate: textToTranslate,
            translation: translationEnabled ? translation : null,
        })
        if (!res.err) {
            onDone()
        } else {
            await showError(res.err)
        }
    }

    function getBgColor({initialValue, currValue}) {
        if (initialValue != currValue) {
            return '#ffffcc'
        }
    }

    return RE.Fragment({},
        re(UpdateTranslateCardCmp,{
            textToTranslate,
            textToTranslateOnChange: newValue => setTextToTranslate(newValue),
            textToTranslateBgColor: getBgColor({initialValue: card.textToTranslate, currValue:textToTranslate}),
            translation: translationEnabled ? translation : null,
            translationOnChange: newValue => translationEnabled ? setTranslation(newValue) : null,
            translationBgColor: getBgColor({initialValue: card.translation, currValue:translation}),
            onSave: saveChanges,
            onCancel: onDone,
            canSave: textToTranslate.length && translation.length && delay.length
        }),
        renderMessagePopup()
    )
}
