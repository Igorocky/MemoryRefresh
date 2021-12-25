"use strict";

const CreateCardView = ({query,openView,setPageTitle}) => {
    const {renderMessagePopup, showMessage, showError} = useMessagePopup()

    const [textToTranslate, setTextToTranslate] = useState('')
    const [translation, setTranslation] = useState('')
    const [cardCounter, setCardCounter] = useState(0)

    async function createCard() {
        const res = await be.saveNewTranslateCard({textToTranslate, translation})
        if (!res.err) {
            await showMessage({text: 'New card was saved.'})
            setTextToTranslate('')
            setTranslation('')
            setCardCounter(c => c + 1)
        } else {
            await showError(res.err)
        }
    }

    return RE.Fragment({},
        re(UpdateTranslateCardCmp,{
            key: cardCounter,
            textToTranslate,textToTranslateOnChange: newValue => setTextToTranslate(newValue),
            translation,translationOnChange: newValue => setTranslation(newValue),
            onSave: createCard, saveDisabled: !textToTranslate.length || !translation.length
        }),
        renderMessagePopup()
    )
}
